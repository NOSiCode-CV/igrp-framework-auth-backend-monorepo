package cv.igrp.framework.auth.core.security;

import cv.igrp.framework.auth.core.config.MethodSecurityConfig;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.core.Authentication;

import java.util.function.Supplier;

/**
 * iGRP's MethodSecurityExpressionHandler that injects a IgrpTypeLocator
 * so you can use T(Permission) instead of the full path.
 */
public class IgrpMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

    @Override
    public EvaluationContext createEvaluationContext(Supplier<Authentication> authentication, MethodInvocation mi) {
        EvaluationContext context = super.createEvaluationContext(authentication, mi);

        if (context instanceof StandardEvaluationContext standardContext) {
            standardContext.setTypeLocator(new MethodSecurityConfig.IgrpTypeLocator());
        }

        return context;
    }

}
