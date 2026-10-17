package com.recipebook.family;

import com.recipebook.model.User;
import com.recipebook.model.UserNames;
import com.recipebook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Familien: alle Mitglieder sind gleichberechtigt (umbenennen, einladen, entfernen, austreten). Jede Aktion prüft
 * serverseitig die Mitgliedschaft; wer nicht Mitglied ist, bekommt „nicht gefunden“. Höchstens {@link #MAX_MEMBERS}
 * Mitglieder; verlässt das letzte Mitglied die Familie, wird sie gelöscht. Beim Austreten/Entfernen verschwinden nur
 * die Freigaben der Rezepte dieses Mitglieds in dieser Familie – Rezepte gehen nie verloren.
 */
@Service
public class FamilyService {

    public static final int MAX_MEMBERS = 10;
    public static final int MAX_FAMILIES_PER_USER = 20;
    public static final int MAX_NAME = 60;
    public static final int INVITE_VALIDITY_DAYS = 14;
    static final int INVITE_BYTES = 18;

    static final String FAMILY_NOT_FOUND = "Die Familie wurde nicht gefunden.";
    static final String MEMBER_NOT_FOUND = "Das Mitglied wurde nicht gefunden.";
    // Ungültig und abgelaufen bewusst gleich – kein Orakel für geratene Codes
    public static final String INVITE_INVALID = "Dieser Einladungslink ist ungültig oder abgelaufen.";
    public static final String FAMILY_FULL = "Diese Familie ist voll.";
    static final String TOO_MANY_FAMILIES = "Du bist schon in " + MAX_FAMILIES_PER_USER + " Familien – mehr geht nicht.";
    static final String NAME_REQUIRED = "Bitte gib einen Namen ein.";
    static final String NAME_TOO_LONG = "Der Name darf höchstens " + MAX_NAME + " Zeichen lang sein.";
    static final String UNNAMED_MEMBER = "Mitglied ohne Namen";

    private static final Pattern CODE_FORMAT = Pattern.compile("^[A-Za-z0-9_-]{16,64}$");

    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository memberRepository;
    private final RecipeFamilyRepository recipeFamilyRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private Clock clock = Clock.systemDefaultZone();

    @Value("${app.url:http://localhost:8080}")
    private String appUrl;

    public FamilyService(FamilyRepository familyRepository, FamilyMemberRepository memberRepository,
            RecipeFamilyRepository recipeFamilyRepository, UserRepository userRepository) {
        this.familyRepository = familyRepository;
        this.memberRepository = memberRepository;
        this.recipeFamilyRepository = recipeFamilyRepository;
        this.userRepository = userRepository;
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    public record Member(Long id, String name, String initials, boolean self) {}

    public record Invite(String code, String url, LocalDateTime expiresAt) {}

    public record FamilyView(Long id, String name, int memberCount, int maxMembers, long recipeCount,
            List<Member> members, Invite invite) {}

    public record InviteInfo(String familyName, String invitedBy, int memberCount, int maxMembers, boolean full,
            boolean member, Long familyId) {}

    @Transactional(readOnly = true)
    public List<FamilyView> myFamilies(Long userId) {
        List<Family> families = familyRepository.findByMember(userId);
        Map<Long, List<Member>> members = membersOf(families.stream().map(Family::getId).toList(), userId);
        return families.stream()
            .map(f -> view(f, members.getOrDefault(f.getId(), List.of()), null))
            .toList();
    }

    @Transactional(readOnly = true)
    public FamilyView get(Long familyId, Long userId) {
        Family family = requireMembership(familyId, userId);
        return detail(family, userId);
    }

    @Transactional
    public FamilyView create(Long userId, String rawName) {
        return create(userId, rawName, false);
    }

    /**
     * Familie gründen; mit shareOwnRecipes bekommen alle Rezepte des Gründers diese Familie zusätzlich in ihre
     * Freigaben (privat → für die Familie sichtbar, öffentlich bleibt öffentlich, andere Familien bleiben).
     */
    @Transactional
    public FamilyView create(Long userId, String rawName, boolean shareOwnRecipes) {
        String name = cleanName(rawName);
        requireUser(userId);
        if (memberRepository.countByUserId(userId) >= MAX_FAMILIES_PER_USER) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, TOO_MANY_FAMILIES);
        }
        Family family = new Family();
        family.setName(name);
        // Gleich mit Einladungslink – das Erste nach dem Gründen ist das Einladen
        family.setInviteCode(newCode());
        family.setInviteExpiresAt(now().plusDays(INVITE_VALIDITY_DAYS));
        family.setInviteCreatedBy(userId);
        family = familyRepository.save(family);
        memberRepository.save(new FamilyMember(family.getId(), userId));
        if (shareOwnRecipes) {
            memberRepository.flush();
            recipeFamilyRepository.shareAllOwnedWith(family.getId(), userId);
        }
        return detail(family, userId);
    }

    @Transactional
    public FamilyView rename(Long familyId, Long userId, String rawName) {
        String name = cleanName(rawName);
        Family family = requireMembership(familyId, userId);
        family.setName(name);
        familyRepository.save(family);
        return detail(family, userId);
    }

    /** Neuer Einladungslink; der bisherige wird damit ungültig. */
    @Transactional
    public FamilyView renewInvite(Long familyId, Long userId) {
        Family family = requireMembership(familyId, userId);
        family.setInviteCode(newCode());
        family.setInviteExpiresAt(now().plusDays(INVITE_VALIDITY_DAYS));
        family.setInviteCreatedBy(userId);
        familyRepository.save(family);
        return detail(family, userId);
    }

    /**
     * Mitglied entfernen oder selbst austreten (targetId == userId). Liefert true, wenn die Familie danach
     * nicht mehr existiert (letztes Mitglied ausgetreten).
     */
    @Transactional
    public boolean removeMember(Long familyId, Long userId, Long targetId) {
        requireMembership(familyId, userId);
        Family family = familyRepository.findByIdForUpdate(familyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, FAMILY_NOT_FOUND));
        if (!memberRepository.existsByFamilyIdAndUserId(familyId, targetId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, MEMBER_NOT_FOUND);
        }
        return leave(family, targetId);
    }

    /** Beim Löschen eines Kontos: aus allen Familien austreten (leere Familien verschwinden). */
    @Transactional
    public void removeUserEverywhere(Long userId) {
        for (Long familyId : memberRepository.findFamilyIdsByUserId(userId)) {
            familyRepository.findByIdForUpdate(familyId).ifPresent(f -> leave(f, userId));
        }
    }

    private boolean leave(Family family, Long userId) {
        recipeFamilyRepository.deleteOwnedBy(family.getId(), userId);
        memberRepository.deleteMember(family.getId(), userId);
        memberRepository.flush();
        if (memberRepository.countByFamilyId(family.getId()) == 0) {
            familyRepository.delete(family);
            return true;
        }
        return false;
    }

    /** Öffentliche Einladungsseite. userId darf null sein (Gast). */
    @Transactional(readOnly = true)
    public InviteInfo inviteInfo(String code, Long userId) {
        Family family = validInvite(code);
        int count = (int) memberRepository.countByFamilyId(family.getId());
        boolean member = userId != null && memberRepository.existsByFamilyIdAndUserId(family.getId(), userId);
        String invitedBy = Optional.ofNullable(family.getInviteCreatedBy())
            .flatMap(userRepository::findById)
            .map(UserNames::firstName)
            .orElse(null);
        return new InviteInfo(family.getName(), invitedBy, count, MAX_MEMBERS, count >= MAX_MEMBERS, member,
            member ? family.getId() : null);
    }

    /** Beitritt per Einladungslink; schon Mitglied → einfach die Familie. */
    @Transactional
    public FamilyView join(String code, Long userId) {
        Family invited = validInvite(code);
        requireUser(userId);
        Family family = familyRepository.findByIdForUpdate(invited.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, INVITE_INVALID));
        if (!memberRepository.existsByFamilyIdAndUserId(family.getId(), userId)) {
            if (memberRepository.countByFamilyId(family.getId()) >= MAX_MEMBERS) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, FAMILY_FULL);
            }
            if (memberRepository.countByUserId(userId) >= MAX_FAMILIES_PER_USER) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, TOO_MANY_FAMILIES);
            }
            memberRepository.save(new FamilyMember(family.getId(), userId));
        }
        return detail(family, userId);
    }

    /** Für die Registrierung mit Familienlink: vorab prüfen, ob der Beitritt klappen wird. */
    @Transactional(readOnly = true)
    public void requireJoinable(String code) {
        Family family = validInvite(code);
        if (memberRepository.countByFamilyId(family.getId()) >= MAX_MEMBERS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, FAMILY_FULL);
        }
    }

    private Family validInvite(String code) {
        if (code == null || !CODE_FORMAT.matcher(code).matches()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, INVITE_INVALID);
        }
        return familyRepository.findByInviteCode(code)
            .filter(f -> f.inviteValid(now()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, INVITE_INVALID));
    }

    private Family requireMembership(Long familyId, Long userId) {
        if (familyId == null || userId == null || !memberRepository.existsByFamilyIdAndUserId(familyId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, FAMILY_NOT_FOUND);
        }
        return familyRepository.findById(familyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, FAMILY_NOT_FOUND));
    }

    private void requireUser(Long userId) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    private FamilyView detail(Family family, Long userId) {
        List<Member> members = membersOf(List.of(family.getId()), userId).getOrDefault(family.getId(), List.of());
        Invite invite = family.inviteValid(now())
            ? new Invite(family.getInviteCode(), appUrl + "/einladung/" + family.getInviteCode(), family.getInviteExpiresAt())
            : null;
        return view(family, members, invite);
    }

    private FamilyView view(Family family, List<Member> members, Invite invite) {
        return new FamilyView(family.getId(), family.getName(), members.size(), MAX_MEMBERS,
            recipeFamilyRepository.countByFamilyId(family.getId()), members, invite);
    }

    private Map<Long, List<Member>> membersOf(List<Long> familyIds, Long userId) {
        Map<Long, List<Member>> result = new HashMap<>();
        if (familyIds.isEmpty()) return result;
        List<FamilyMember> rows = memberRepository.findByFamilyIdInOrderByJoinedAtAscUserIdAsc(familyIds);
        Map<Long, User> users = new HashMap<>();
        userRepository.findAllById(rows.stream().map(FamilyMember::getUserId).distinct().toList())
            .forEach(u -> users.put(u.getId(), u));
        for (FamilyMember row : rows) {
            String name = UserNames.fullName(users.get(row.getUserId()));
            result.computeIfAbsent(row.getFamilyId(), id -> new ArrayList<>()).add(new Member(row.getUserId(),
                name != null ? name : UNNAMED_MEMBER, name != null ? UserNames.initials(name) : "?",
                row.getUserId().equals(userId)));
        }
        return result;
    }

    static String cleanName(String raw) {
        String name = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, NAME_REQUIRED);
        if (name.length() > MAX_NAME) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, NAME_TOO_LONG);
        return name;
    }

    private String newCode() {
        byte[] bytes = new byte[INVITE_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
