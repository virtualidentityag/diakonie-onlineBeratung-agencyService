package de.caritas.cob.agencyservice.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.caritas.cob.agencyservice.api.authorization.Authority.AuthorityValue;
import de.caritas.cob.agencyservice.api.service.AgencyService;
import jakarta.servlet.http.Cookie;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

/** Authorization tests for the registration url endpoint of {@link AgencyController}. */
@RunWith(SpringRunner.class)
@TestPropertySource(properties = "spring.profiles.active=testing")
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class AgencyControllerRegistrationUrlAuthorizationIT {

  private static final String REGISTRATION_URL_PATH = "/agencies/1/registration-url";
  private static final String BODY =
      "{\"registrationUrl\":\"https://www.diakonie.de/registration/agency\","
          + "\"addedBy\":\"aadc0ecf-c048-4bfc-857d-8c9b2e425500\"}";
  // /agencies/** is not CSRF-whitelisted, so the names must match application-testing.properties
  private static final String CSRF_HEADER = "X-CSRF-TOKEN";
  private static final String CSRF_VALUE = "test";
  private static final Cookie CSRF_COOKIE = new Cookie("CSRF-TOKEN", CSRF_VALUE);

  @Autowired private MockMvc mvc;

  @MockBean private AgencyService agencyService;

  @Test
  public void setAgencyRegistrationUrl_Should_ReturnUnauthorized_When_noKeycloakAuthorizationIsPresent()
      throws Exception {
    mvc.perform(
            put(REGISTRATION_URL_PATH)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(agencyService);
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.AGENCY_ADMIN})
  public void setAgencyRegistrationUrl_Should_ReturnForbidden_When_callerIsNotTechnicalUser()
      throws Exception {
    mvc.perform(
            put(REGISTRATION_URL_PATH)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isForbidden());

    verifyNoInteractions(agencyService);
  }

  @Test
  @WithMockUser(authorities = {AuthorityValue.TECHNICAL_DEFAULT})
  public void setAgencyRegistrationUrl_Should_ReturnNoContent_When_callerIsTechnicalUser()
      throws Exception {
    mvc.perform(
            put(REGISTRATION_URL_PATH)
                .cookie(CSRF_COOKIE)
                .header(CSRF_HEADER, CSRF_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isNoContent());

    verify(agencyService).setRegistrationUrl(anyLong(), any(), any());
  }
}
