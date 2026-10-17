package com.recipebook.analytics;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class UsageEventRepository {

    private static final String INSERT = "INSERT INTO usage_events "
            + "(app, event, ts, user_pseudonym, session_id, device, display, props) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb))";

    private final JdbcTemplate jdbcTemplate;

    public UsageEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertAll(List<UsageEvent> events) {
        if (events.isEmpty()) return;
        jdbcTemplate.batchUpdate(INSERT, events, events.size(), (ps, e) -> {
            ps.setString(1, e.app());
            ps.setString(2, e.event());
            ps.setObject(3, OffsetDateTime.ofInstant(e.ts(), ZoneOffset.UTC));
            ps.setString(4, e.userPseudonym());
            ps.setString(5, e.sessionId());
            ps.setString(6, e.device());
            ps.setString(7, e.display());
            ps.setString(8, e.propsJson());
        });
    }
}
