package cv.igrp.sdktest.harness;

import cv.igrp.framework.auth.core.audit.CorrelationIds;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * v3-24 (#31): every Envers revision in a service records who made the change
 * and the request's trace id, so the change history joins the audit event and
 * the trace.
 */
class RevisionHarnessTest extends AbstractStarterTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    NoteRepository notes;
    @Autowired
    TransactionTemplate tx;

    @Test
    @DisplayName("A change made by a user records their username and the request's correlation id")
    void revisionRecordsUsernameAndCorrelationId() throws Exception {
        long id = createNote(post("/notes").header(CorrelationIds.HEADER, "order-77"));

        assertThat(revisionOf(id))
                .containsEntry("USERNAME", "alice")
                .containsEntry("TRACE_ID", "order-77");
    }

    @Test
    @DisplayName("With tracing on, the revision records the trace id")
    void revisionRecordsTheTraceId() throws Exception {
        long id;
        try (Scope ignored = Span.wrap(SpanContext.create(TRACE_ID, "00f067aa0ba902b7",
                TraceFlags.getSampled(), TraceState.getDefault())).makeCurrent()) {
            id = createNote(post("/notes"));
        }

        assertThat(revisionOf(id)).containsEntry("TRACE_ID", TRACE_ID);
    }

    @Test
    @DisplayName("A change outside any request is made by 'system' and still gets an id")
    void backgroundRevisionIsSystem() {
        Long id = Objects.requireNonNull(tx.execute(status -> notes.save(new Note("from a scheduler")).getId()));

        Map<String, Object> revision = revisionOf(id);
        assertThat(revision).containsEntry("USERNAME", "system");
        assertThat(revision.get("TRACE_ID")).asString().matches("[0-9a-f]{32}");
    }

    private long createNote(MockHttpServletRequestBuilder request)
            throws Exception {
        String body = mvc.perform(request.content("hello")
                        .with(jwt().jwt(token -> token.claim("preferred_username", "alice")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(body);
    }

    private Map<String, Object> revisionOf(long noteId) {
        return jdbc.queryForMap("""
                SELECT r.username, r.trace_id FROM revinfo r
                  JOIN note_aud a ON a.rev = r.rev
                 WHERE a.id = ?""", noteId);
    }
}
