package cm.kamerbiz.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.kamerbiz.identity.AccountController;
import cm.kamerbiz.identity.IdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = AccountController.class,
    properties = {
      "app.security.jwk-set-uri=http://localhost:8081/realms/kamerbiz/protocol/openid-connect/certs",
      "app.security.issuer-uri=http://localhost:8081/realms/kamerbiz",
      "app.security.audience=kamerbiz-api",
      "app.security.allowed-origin=http://localhost:5173"
    })
@Import(SecurityConfiguration.class)
class SecurityConfigurationTest {
  @Autowired private MockMvc mockMvc;
  @MockitoBean private IdentityService identityService;

  @Test
  void accountEndpointRejectsAnonymousRequests() throws Exception {
    mockMvc.perform(get("/api/v1/account")).andExpect(status().isUnauthorized());
  }

  @Test
  void apiUsesBearerAuthenticationWithoutCookieBasedCsrf() throws Exception {
    mockMvc.perform(post("/api/v1/account")).andExpect(status().isUnauthorized());
  }

  @Test
  void csrfProtectionRemainsEnabledOutsideApiPaths() throws Exception {
    mockMvc.perform(post("/non-api-resource")).andExpect(status().isForbidden());
  }
}
