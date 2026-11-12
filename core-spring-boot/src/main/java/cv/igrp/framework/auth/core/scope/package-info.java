/**
 * Data scopes (v3-14). The starter's one scope filter is declared here and added to every
 * persistence unit by {@link cv.igrp.framework.auth.core.scope.ScopeAutoConfiguration}, so
 * scoped entities only name it ({@code @Filter(name = IgrpScope.FILTER)}). Reads by id are
 * checked against it by {@link cv.igrp.framework.auth.core.scope.ScopedLoadByKey}.
 */
@FilterDef(name = IgrpScope.FILTER, defaultCondition = IgrpScope.DEFAULT_CONDITION,
        parameters = {
                @ParamDef(name = "igrpOwn", type = Boolean.class),
                @ParamDef(name = "igrpSub", type = String.class),
                @ParamDef(name = "igrpByRole", type = Boolean.class),
                @ParamDef(name = "igrpRoles", type = String.class),
                @ParamDef(name = "igrpDepartment", type = Boolean.class),
                @ParamDef(name = "igrpDepartments", type = String.class)
        })
package cv.igrp.framework.auth.core.scope;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
