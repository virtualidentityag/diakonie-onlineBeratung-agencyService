package de.caritas.cob.agencyservice.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import de.caritas.cob.agencyservice.api.model.RegistrationUrlDTO;
import de.caritas.cob.agencyservice.api.service.AgencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/** tests for the registration url endpoint of {@link AgencyController}. */
@ExtendWith(MockitoExtension.class)
class AgencyControllerRegistrationUrlTest {

  private static final Long AGENCY_ID = 98L;
  private static final String CONSULTANT_ID = "aadc0ecf-c048-4bfc-857d-8c9b2e425500";
  private static final String URL = "https://www.diakonie.de/registration/agency";

  @InjectMocks private AgencyController agencyController;

  @Mock private AgencyService agencyService;

  @Test
  void setAgencyRegistrationUrl_Should_delegateAndReturnNoContent() {
    var dto = new RegistrationUrlDTO().registrationUrl(URL).addedBy(CONSULTANT_ID);

    var response = agencyController.setAgencyRegistrationUrl(AGENCY_ID, dto);

    verify(agencyService).setRegistrationUrl(AGENCY_ID, URL, CONSULTANT_ID);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void setAgencyRegistrationUrl_Should_delegateRemoval_When_urlIsBlank() {
    var dto = new RegistrationUrlDTO().registrationUrl(null).addedBy(CONSULTANT_ID);

    var response = agencyController.setAgencyRegistrationUrl(AGENCY_ID, dto);

    verify(agencyService).setRegistrationUrl(AGENCY_ID, null, CONSULTANT_ID);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }
}
