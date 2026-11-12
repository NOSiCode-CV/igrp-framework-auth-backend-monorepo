package cv.igrp.framework.auth.core.scope;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Data scopes (v3-14): which rows of a scoped entity the caller may see, from the scope of
 * the route's permission key in the caller's active role.
 *
 * <p>A scoped entity declares the starter's one filter, whose condition holds every branch
 * combined with {@code OR} (one filter, so the branches never combine with {@code AND}):
 * <pre>{@code
 * @Entity
 * @Filter(name = IgrpScope.FILTER)   // columns owner_id, role_code, department_code
 * public class Fatura { ... }
 * }</pre>
 * An entity whose columns have other names writes the same condition with its own names,
 * keeping the parameters, e.g. {@code @Filter(name = IgrpScope.FILTER, condition = "(:igrpOwn
 * = true and criado_por = :igrpSub) or (:igrpByRole = true and papel in (:igrpRoles)) or
 * (:igrpDepartment = true and departamento in (:igrpDepartments))")}.
 *
 * <ul>
 *   <li>{@code all}: no filter at all;</li>
 *   <li>{@code own}: the owner column holds the caller's {@code sub};</li>
 *   <li>{@code by_role}: the role column holds one of the caller's role codes;</li>
 *   <li>{@code department}: the department column holds a code of the caller's visible department subtree.</li>
 * </ul>
 * A read by id outside the scope finds nothing. A scoped entity loaded with no scope (a
 * background job, no request) is loaded unfiltered, with a warning.
 */
public final class IgrpScope {

    /** The name of the starter's scope filter. */
    public static final String FILTER = "igrpScope";

    /** The condition for the conventional column names {@code owner_id}, {@code role_code}, {@code department_code}. */
    public static final String DEFAULT_CONDITION =
            "(:igrpOwn = true and owner_id = :igrpSub)"
                    + " or (:igrpByRole = true and role_code in (:igrpRoles))"
                    + " or (:igrpDepartment = true and department_code in (:igrpDepartments))";

    static final String ALL = "all";
    static final String OWN = "own";
    static final String BY_ROLE = "by_role";
    static final String DEPARTMENT = "department";

    /** Stands in for an empty list, which would be invalid SQL; never a role or department code. */
    static final String NONE = "~";

    private static final String ATTRIBUTE = IgrpScope.class.getName() + ".current";

    private IgrpScope() {
    }

    /**
     * The scope of one request: the strategies that apply and the caller's data they compare with.
     * {@code all} disables the filter.
     */
    public record Context(boolean all, boolean own, boolean byRole, boolean department,
                          String sub, Set<String> roles, Set<String> departments) {

        public static final Context EVERYTHING = new Context(true, false, false, false, null, Set.of(), Set.of());

        public Context {
            roles = Set.copyOf(roles);
            departments = Set.copyOf(departments);
        }

        /** The role codes as a filter parameter: never empty, so the SQL stays valid. */
        public List<String> roleParameter() {
            return roles.isEmpty() ? List.of(NONE) : List.copyOf(roles);
        }

        public List<String> departmentParameter() {
            return departments.isEmpty() ? List.of(NONE) : List.copyOf(departments);
        }
    }

    /**
     * The context for a key's scope value, a code or a comma list of codes (a service account
     * reaching the key through several roles).
     *
     * @return the context, or {@code null} when the value is missing or has an unknown code: the
     *         request is then refused rather than shown rows by a guess
     */
    public static Context from(String scopes, String sub, Collection<String> roles, Collection<String> departments) {
        if (scopes == null || scopes.isBlank()) {
            return null;
        }
        Set<String> codes = new HashSet<>();
        for (String code : scopes.split(",")) {
            String trimmed = code.trim();
            if (!Set.of(ALL, OWN, BY_ROLE, DEPARTMENT).contains(trimmed)) {
                return null;
            }
            codes.add(trimmed);
        }
        if (codes.contains(ALL)) {
            return Context.EVERYTHING;
        }
        Set<String> roleCodes = Set.copyOf(roles);
        return new Context(false, codes.contains(OWN), codes.contains(BY_ROLE) && !roleCodes.isEmpty(),
                codes.contains(DEPARTMENT), sub, roleCodes, Set.copyOf(departments));
    }

    /** Binds the scope to the current request, for the transactions it runs. */
    public static void bind(Context context) {
        RequestAttributes request = RequestContextHolder.getRequestAttributes();
        if (request != null) {
            request.setAttribute(ATTRIBUTE, context, RequestAttributes.SCOPE_REQUEST);
        }
    }

    /** The current request's scope; {@code null} outside a request or on a route with no scope. */
    public static Context current() {
        RequestAttributes request = RequestContextHolder.getRequestAttributes();
        return request == null ? null : (Context) request.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
    }
}
