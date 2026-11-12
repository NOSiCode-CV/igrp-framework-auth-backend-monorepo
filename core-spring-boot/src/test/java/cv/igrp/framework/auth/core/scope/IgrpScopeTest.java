package cv.igrp.framework.auth.core.scope;

import org.hibernate.Filter;
import org.hibernate.Session;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** v3-14: reading a key's scope value, and the edges of the load-by-id check, without a database. */
class IgrpScopeTest {

    @Test
    @DisplayName("A comma list is a union; spaces are trimmed; all wins over the rest")
    void parse() {
        IgrpScope.Context scope = IgrpScope.from("by_role, own", "u", List.of("R"), List.of());
        assertTrue(scope.own());
        assertTrue(scope.byRole());
        assertFalse(scope.department());
        assertFalse(scope.all());

        assertSame(IgrpScope.Context.EVERYTHING, IgrpScope.from("own,all", "u", List.of(), List.of()));
    }

    @Test
    @DisplayName("Missing, blank or unknown codes give no scope: the request is refused")
    void invalid() {
        assertNull(IgrpScope.from(null, "u", List.of(), List.of()));
        assertNull(IgrpScope.from(" ", "u", List.of(), List.of()));
        assertNull(IgrpScope.from("own,everything", "u", List.of(), List.of()));
    }

    @Test
    @DisplayName("by_role with no roles is off, and empty lists become a placeholder that matches no code")
    void emptyLists() {
        IgrpScope.Context scope = IgrpScope.from("by_role,department", "u", List.of(), List.of());
        assertFalse(scope.byRole());
        assertEquals(List.of("~"), scope.roleParameter());
        assertEquals(List.of("~"), scope.departmentParameter());
    }

    @Test
    @DisplayName("Outside a request there is no scope to bind or read")
    void outsideRequest() {
        IgrpScope.bind(IgrpScope.Context.EVERYTHING);
        assertNull(IgrpScope.current());
    }

    @Test
    @DisplayName("With no scope or with all, a transaction turns the filter off")
    void dialectOff() {
        Session session = mock(Session.class);
        ScopingJpaDialect.apply(session, null);
        ScopingJpaDialect.apply(session, IgrpScope.Context.EVERYTHING);
        verify(session, times(2)).disableFilter(IgrpScope.FILTER);
        verify(session, never()).enableFilter(IgrpScope.FILTER);
    }

    @Test
    @DisplayName("The load-by-id check leaves alone: nothing loaded, other load types, no active filter, unscoped entities")
    void loadByKeyEdges() {
        ScopedLoadByKey check = new ScopedLoadByKey();
        EventSource session = mock(EventSource.class);
        LoadEvent event = mock(LoadEvent.class);
        when(event.getSession()).thenReturn(session);

        when(event.getResult()).thenReturn(null);
        check.onLoad(event, LoadEventListener.GET);

        when(event.getResult()).thenReturn(new Object());
        check.onLoad(event, LoadEventListener.INTERNAL_LOAD_EAGER);

        when(session.getEnabledFilter(IgrpScope.FILTER)).thenReturn(null);
        check.onLoad(event, LoadEventListener.GET);

        when(session.getEnabledFilter(IgrpScope.FILTER)).thenReturn(mock(Filter.class));
        check.onLoad(event, LoadEventListener.LOAD);

        verify(session, never()).createSelectionQuery(anyString(), eq(Long.class));
        verify(event, never()).setResult(any());
    }

    @Test
    @DisplayName("A Context copies its sets, so later changes to the caller's lists do not leak in")
    void contextCopies() {
        Set<String> roles = new HashSet<>(Set.of("R"));
        IgrpScope.Context scope = new IgrpScope.Context(false, false, true, false, "u", roles, Set.of());
        roles.add("X");
        assertEquals(Set.of("R"), scope.roles());
    }
}
