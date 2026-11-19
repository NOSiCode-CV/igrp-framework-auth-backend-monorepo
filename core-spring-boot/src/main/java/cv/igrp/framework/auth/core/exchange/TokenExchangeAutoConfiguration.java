package cv.igrp.framework.auth.core.exchange;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import java.time.Clock;

@AutoConfiguration
@ConditionalOnProperty(prefix = "igrp.security.token-exchange", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(TokenExchangeProperties.class)
public class TokenExchangeAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public ExchangedTokenCache igrpExchangedTokenCache(TokenExchangeProperties settings) {
        Clock clock = Clock.systemUTC();
        return new ExchangedTokenCache(new TokenExchangeHttpClient(settings.getTokenUri(), settings.getClientId(),
                settings.getClientSecret(), settings.getTimeout(), clock), clock,
                settings.getExpirySkew(), settings.getMaxEntries());
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenExchangeClients igrpTokenExchangeClients(ExchangedTokenCache cache, TokenExchangeProperties settings) {
        return new TokenExchangeClients(cache, settings.getTargets(), settings.getTimeout());
    }
}
