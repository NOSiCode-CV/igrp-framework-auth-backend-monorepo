package cv.igrp.framework.auth.core.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * v3-24 (#31): registers {@link CorrelationIdFilter} for servlet services.
 *
 * <p>It runs just before Spring Security's chain, so security logs already carry
 * the id, and after the OpenTelemetry server filter, so the trace id exists.
 */
@org.springframework.boot.autoconfigure.AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CorrelationAutoConfiguration {

    /** Spring Security's chain sits at -100 ({@code SecurityProperties.DEFAULT_FILTER_ORDER}). */
    static final int ORDER = -101;

    @Bean
    @ConditionalOnMissingBean(name = "igrpCorrelationIdFilter")
    public FilterRegistrationBean<CorrelationIdFilter> igrpCorrelationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>(new CorrelationIdFilter());
        registration.setOrder(ORDER);
        return registration;
    }
}
