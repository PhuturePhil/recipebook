package com.recipebook.security;

import com.recipebook.model.Emails;
import com.recipebook.service.MailService;
import com.recipebook.service.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deckel für Bestätigungsmails (Registrierung, neue E-Mail-Adresse): je Adresse 3 in 15 Minuten, je IP 3 pro Stunde,
 * insgesamt 30 pro Stunde als Notbremse. Reißt der IP- oder der Gesamtdeckel, gibt es eine Warnung im Log und
 * höchstens eine Alarm-Mail pro Stunde und Deckel.
 */
@Component
public class ConfirmationMailLimiter {

    private static final Logger log = LoggerFactory.getLogger(ConfirmationMailLimiter.class);

    static final int PER_ADDRESS = 3;
    static final Duration ADDRESS_WINDOW = Duration.ofMinutes(15);
    static final int PER_IP = 3;
    static final int TOTAL = 30;
    static final Duration HOUR = Duration.ofHours(1);
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Berlin"));

    private final RateLimiter rateLimiter;
    private final MailService mailService;
    private final Clock clock;
    private final Map<String, Instant> lastAlert = new ConcurrentHashMap<>();

    @Value("${app.security.alert-mail:pastoors.cloud@mail.de}")
    private String alertMail;

    @Autowired
    public ConfirmationMailLimiter(MailService mailService) {
        this(new RateLimiter(), mailService, Clock.systemUTC());
    }

    public ConfirmationMailLimiter(RateLimiter rateLimiter, MailService mailService, Clock clock) {
        this.rateLimiter = rateLimiter;
        this.mailService = mailService;
        this.clock = clock;
    }

    /** Darf an diese Adresse jetzt eine Bestätigungsmail gehen? Verbraucht bei Ja einen Platz in allen Töpfen. */
    public boolean tryAcquire(String clientIp, String address) {
        String ip = clientIp == null || clientIp.isBlank() ? "unbekannt" : clientIp.trim();
        if (!rateLimiter.tryAcquire("confirm-mail-address:" + Emails.normalize(address), PER_ADDRESS, ADDRESS_WINDOW)) {
            return false;
        }
        if (!rateLimiter.tryAcquire("confirm-mail-ip:" + ip, PER_IP, HOUR)) {
            alarm("ip", "Eine IP-Adresse (" + mask(ip) + ") hat mehr als " + PER_IP
                    + " Bestätigungsmails in einer Stunde angefordert. Weitere Mails an diese IP werden zurückgehalten.");
            return false;
        }
        if (!rateLimiter.tryAcquire("confirm-mail-all", TOTAL, HOUR)) {
            alarm("total", "Es wurden mehr als " + TOTAL + " Bestätigungsmails in einer Stunde angefordert. "
                    + "Bis die Stunde um ist, verschickt die App keine weiteren.");
            return false;
        }
        return true;
    }

    private void alarm(String kind, String text) {
        log.warn("Bestätigungsmail-Deckel erreicht ({}): {}", kind, text);
        Instant now = clock.instant();
        Instant last = lastAlert.get(kind);
        if (last != null && last.plus(HOUR).isAfter(now)) return;
        lastAlert.put(kind, now);
        mailService.sendText(alertMail, "Rezepte-App: Deckel für Bestätigungsmails erreicht",
                "Hallo,\n\n" + text + "\n\nZeitpunkt: " + TIME.format(now) + "\n\n"
                        + "Das kann ein Versuch sein, über die App Mails an fremde Adressen zu schicken. "
                        + "Weitere Alarme zu diesem Deckel kommen frühestens in einer Stunde.\n\n"
                        + "Pastoors Familienrezepte\n");
    }

    /** IPv4 auf /24, IPv6 auf /48 gekürzt – wie im Server-Protokoll. */
    static String mask(String ip) {
        if (ip.contains(".") && !ip.contains(":")) {
            int last = ip.lastIndexOf('.');
            return last > 0 ? ip.substring(0, last) + ".0/24" : ip;
        }
        if (ip.contains(":")) {
            String[] parts = ip.split(":", -1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(3, parts.length); i++) {
                if (i > 0) sb.append(':');
                sb.append(parts[i]);
            }
            return sb + "::/48";
        }
        return ip;
    }
}
