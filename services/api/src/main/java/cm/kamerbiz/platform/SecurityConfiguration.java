package cm.kamerbiz.platform;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfiguration {
  @Bean
  JwtDecoder jwtDecoder(
      @Value("${app.security.jwk-set-uri}") String jwkSetUri,
      @Value("${app.security.issuer-uri}") String issuerUri,
      @Value("${app.security.audience}") String audience) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
    OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
    return decoder;
  }

  @Bean
  SecurityFilterChain applicationSecurity(HttpSecurity http) throws Exception {
    return http.cors(Customizer.withDefaults())
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/actuator/**"))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth ->
            auth.requestMatchers("/error")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/health", "/actuator/health")
                .permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/public/**")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/public/**")
                .permitAll()
                .anyRequest()
                .authenticated())
        .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
        .build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${app.security.allowed-origin}") String allowedOrigin) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(allowedOrigin));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    configuration.setAllowCredentials(false);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }
}
