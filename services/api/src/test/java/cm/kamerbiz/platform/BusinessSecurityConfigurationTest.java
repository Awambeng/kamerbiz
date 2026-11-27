package cm.kamerbiz.platform;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cm.kamerbiz.business.BusinessController;
import cm.kamerbiz.business.BusinessRole;
import cm.kamerbiz.business.BusinessService;
import cm.kamerbiz.business.BusinessService.BusinessResponse;
import cm.kamerbiz.business.BusinessService.UpdateBusinessRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = BusinessController.class,
    properties = {
      "app.security.jwk-set-uri=http://localhost:8081/realms/kamerbiz/protocol/openid-connect/certs",
      "app.security.issuer-uri=http://localhost:8081/realms/kamerbiz",
      "app.security.audience=kamerbiz-api",
      "app.security.allowed-origin=http://localhost:5173"
    })
@Import(SecurityConfiguration.class)
class BusinessSecurityConfigurationTest {
  @Autowired private MockMvc mockMvc;

  @MockitoBean private BusinessService businessService;

  @Test
  void businessEndpointsRejectAnonymousRequests() throws Exception {
    mockMvc.perform(get("/api/v1/businesses")).andExpect(status().isUnauthorized());
    mockMvc.perform(post("/api/v1/businesses")).andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedBusinessProfilePatchAcceptsAValidName() throws Exception {
    UUID businessId = UUID.randomUUID();
    BusinessResponse response =
        new BusinessResponse(
            businessId,
            "Grace Fashion",
            "grace-fashion",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "Cameroon",
            BusinessRole.OWNER);
    when(businessService.update(any(), any(), any(UpdateBusinessRequest.class)))
        .thenReturn(response);

    mockMvc
        .perform(
            patch("/api/v1/businesses/{businessId}", businessId)
                .with(jwt().jwt(token -> token.subject("user-123")))
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"name":"Grace Fashion"}
                    """))
        .andExpect(status().isOk());

    verify(businessService).update(any(), any(), any(UpdateBusinessRequest.class));
  }
}
