package cv.igrp.framework.auth.core.config;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.method.AuthorizationManagerAfterMethodInterceptor;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.authorization.method.PostAuthorizeAuthorizationManager;
import org.springframework.security.authorization.method.PostFilterAuthorizationMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.authorization.method.PreFilterAuthorizationMethodInterceptor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * Method security in the 0.3 starter (v3-08). The route filter decides on a controller's
 * request handlers, so a {@code @PreAuthorize} left on them is not evaluated: it can
 * neither block nor open a route. Everywhere else (service layer, scheduled code) it is
 * evaluated against the caller's permission keys as authorities, e.g.
 * {@code @PreAuthorize("hasAuthority('faturacao-api.faturas.aprovar.update')")}.
 * {@code @PostAuthorize}, {@code @PreFilter} and {@code @PostFilter} work as usual.
 */
@AutoConfiguration
@EnableConfigurationProperties(MethodSecurityProperties.class)
@EnableMethodSecurity(prePostEnabled = false)
@ConditionalOnProperty(
        name = "igrp.framework.auth.method-security.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MethodSecurityEnableConfig {

    private MethodSecurityEnableConfig() {
        // Only static infrastructure beans.
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    static Advisor igrpPreAuthorize(ApplicationContext context) {
        PreAuthorizeAuthorizationManager preAuthorize = new PreAuthorizeAuthorizationManager();
        preAuthorize.setExpressionHandler(expressionHandler(context));
        return AuthorizationManagerBeforeMethodInterceptor.preAuthorize(new SkipRouteHandlers(preAuthorize));
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    static Advisor igrpPostAuthorize(ApplicationContext context) {
        PostAuthorizeAuthorizationManager postAuthorize = new PostAuthorizeAuthorizationManager();
        postAuthorize.setExpressionHandler(expressionHandler(context));
        return AuthorizationManagerAfterMethodInterceptor.postAuthorize(postAuthorize);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    static Advisor igrpPreFilter(ApplicationContext context) {
        PreFilterAuthorizationMethodInterceptor preFilter = new PreFilterAuthorizationMethodInterceptor();
        preFilter.setExpressionHandler(expressionHandler(context));
        return preFilter;
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    static Advisor igrpPostFilter(ApplicationContext context) {
        PostFilterAuthorizationMethodInterceptor postFilter = new PostFilterAuthorizationMethodInterceptor();
        postFilter.setExpressionHandler(expressionHandler(context));
        return postFilter;
    }

    /** SpEL with access to the application's beans ({@code @bean.method(...)}), as Spring's own configuration sets up. */
    private static DefaultMethodSecurityExpressionHandler expressionHandler(ApplicationContext context) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setApplicationContext(context);
        return handler;
    }

    /** Grants a controller's request handlers, where the route decision already applied; asks the delegate elsewhere. */
    static final class SkipRouteHandlers implements AuthorizationManager<MethodInvocation> {

        private final AuthorizationManager<MethodInvocation> delegate;

        SkipRouteHandlers(AuthorizationManager<MethodInvocation> delegate) {
            this.delegate = delegate;
        }

        @Override
        public AuthorizationResult authorize(Supplier<? extends Authentication> authentication, MethodInvocation invocation) {
            return isRouteHandler(invocation) ? new AuthorizationDecision(true)
                    : delegate.authorize(authentication, invocation);
        }

        static boolean isRouteHandler(MethodInvocation invocation) {
            Object bean = invocation.getThis();
            Class<?> target = bean == null ? invocation.getMethod().getDeclaringClass() : AopUtils.getTargetClass(bean);
            Method method = AopUtils.getMostSpecificMethod(invocation.getMethod(), target);
            return AnnotatedElementUtils.hasAnnotation(target, Controller.class)
                    && AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class);
        }
    }
}
