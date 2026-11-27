package cm.kamerbiz.platform;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Development-only HTTP Basic configuration. Replace this before any non-local deployment. The
 * configured local credentials are not suitable for production.
 */
@Configuration
public class SecurityConfiguration {
  @Bean
  SecurityFilterChain applicationSecurity(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(auth ->
            auth.requestMatchers(HttpMethod.GET, "/api/v1/health", "/actuator/health")
                .permitAll()
                .anyRequest()
                .authenticated())
        .httpBasic(basic -> {})
        .build();
  }
}
