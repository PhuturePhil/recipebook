package com.recipebook.analytics;

import com.recipebook.service.ShareLinkService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Teilen-Events: der Client schickt den Token der Teilen-Seite mit, gespeichert wird er nie. Der Server ersetzt ihn
 * durch recipeId, shareLinkId, linkRef (die ersten 12 Hex-Zeichen von SHA-256(Token)) und den Zustand des Links –
 * aus der Tabelle lässt sich so kein gültiger Link zurückgewinnen, und der Client kann kein fremdes Rezept angeben.
 */
@Component
public class ShareEventResolver {

    static final String LINK_VIEWED = "teilen_link_aufgerufen";
    static final String LOGIN = "teilen_login";
    static final int LINK_REF_LENGTH = 12;

    private static final Set<String> EVENTS = Set.of(LINK_VIEWED, LOGIN);
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_-]{16,64}");

    private final ShareLinkService shareLinkService;

    public ShareEventResolver(ShareLinkService shareLinkService) {
        this.shareLinkService = shareLinkService;
    }

    public boolean handles(String event) {
        return EVENTS.contains(event);
    }

    /** Fertige props des Events; null = verwerfen (Token fehlt oder hat nicht das Format eines Teilen-Tokens). */
    public Map<String, Object> resolve(String event, Map<String, Object> props, boolean loggedIn) {
        if (props == null || !(props.get("token") instanceof String token) || !TOKEN.matcher(token).matches()) return null;
        ShareLinkService.LinkState state = shareLinkService.linkState(token);
        Map<String, Object> out = new LinkedHashMap<>();
        if (state.recipeId() != null) out.put("recipeId", state.recipeId());
        if (state.shareLinkId() != null) out.put("shareLinkId", state.shareLinkId());
        out.put("linkRef", linkRef(token));
        out.put("link", state.status());
        if (LINK_VIEWED.equals(event)) out.put("status", loggedIn ? "eingeloggt" : "anonym");
        if (LOGIN.equals(event) && props.get("seit_ms") instanceof Number since && since.longValue() >= 0) {
            out.put("seit_ms", since.longValue());
        }
        return out;
    }

    static String linkRef(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, LINK_REF_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nicht verfügbar", e);
        }
    }
}
