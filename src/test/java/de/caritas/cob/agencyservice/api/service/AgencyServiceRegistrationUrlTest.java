package de.caritas.cob.agencyservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.caritas.cob.agencyservice.api.admin.service.agency.DemographicsConverter;
import de.caritas.cob.agencyservice.api.exception.httpresponses.BadRequestException;
import de.caritas.cob.agencyservice.api.exception.httpresponses.NotFoundException;
import de.caritas.cob.agencyservice.api.manager.consultingtype.ConsultingTypeManager;
import de.caritas.cob.agencyservice.api.repository.agency.Agency;
import de.caritas.cob.agencyservice.api.repository.agency.AgencyRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** tests for the registration url handling of {@link AgencyService}. */
@ExtendWith(MockitoExtension.class)
class AgencyServiceRegistrationUrlTest {

  private static final Long AGENCY_ID = 98L;
  private static final String CONSULTANT_ID = "aadc0ecf-c048-4bfc-857d-8c9b2e425500";
  private static final String VALID_REGISTRATION_URL =
      "https://www.diakonie.de/registration/agency";

  @InjectMocks private AgencyService agencyService;

  @Mock private ConsultingTypeManager consultingTypeManager;
  @Mock private AgencyRepository agencyRepository;
  @Mock private TenantService tenantService;
  @Mock private DemographicsConverter demographicsConverter;
  @Mock private CentralDataProtectionTemplateService centralDataProtectionTemplateService;
  @Mock private ApplicationSettingsService applicationSettingsService;

  @Test
  void setRegistrationUrl_Should_persistUrlAndAttribution_When_urlIsValid() {
    when(agencyRepository.findById(AGENCY_ID)).thenReturn(Optional.of(new Agency()));

    agencyService.setRegistrationUrl(AGENCY_ID, VALID_REGISTRATION_URL, CONSULTANT_ID);

    var saved = captureSavedAgency();
    assertThat(saved.getRegistrationUrl()).isEqualTo(VALID_REGISTRATION_URL);
    assertThat(saved.getRegistrationUrlAddedBy()).isEqualTo(CONSULTANT_ID);
    assertThat(saved.getRegistrationUrlAddedDate()).isNotNull();
  }

  @Test
  void setRegistrationUrl_Should_persist_When_urlIsSubdomain() {
    when(agencyRepository.findById(AGENCY_ID)).thenReturn(Optional.of(new Agency()));

    agencyService.setRegistrationUrl(
        AGENCY_ID, "https://beratung.diakonie.de/registration", CONSULTANT_ID);

    assertThat(captureSavedAgency().getRegistrationUrl())
        .isEqualTo("https://beratung.diakonie.de/registration");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://evil.example.com/registration",
        "diakonie.de",
        "http://diakonie.de",
        "https://diakonie.de.something-bad.com",
        "https://something-bad-diakonie.de",
        "https://something-bad.com/diakonie.de",
        "https://diakonie.de@something-bad.com",
        "https://diakonie.de:@something-bad.com"
      })
  void setRegistrationUrl_Should_throwBadRequest_When_urlIsNotAllowed(String url) {
    assertThrows(
        BadRequestException.class,
        () -> agencyService.setRegistrationUrl(AGENCY_ID, url, CONSULTANT_ID));

    verify(agencyRepository, never()).save(any());
  }

  @Test
  void setRegistrationUrl_Should_clearUrlButKeepAttribution_When_urlIsBlank() {
    var agency = new Agency();
    agency.setRegistrationUrl(VALID_REGISTRATION_URL);
    when(agencyRepository.findById(AGENCY_ID)).thenReturn(Optional.of(agency));

    agencyService.setRegistrationUrl(AGENCY_ID, "  ", CONSULTANT_ID);

    // url is cleared but who removed it and when is recorded
    var saved = captureSavedAgency();
    assertThat(saved.getRegistrationUrl()).isNull();
    assertThat(saved.getRegistrationUrlAddedBy()).isEqualTo(CONSULTANT_ID);
    assertThat(saved.getRegistrationUrlAddedDate()).isNotNull();
  }

  @Test
  void setRegistrationUrl_Should_throwNotFound_When_agencyDoesNotExist() {
    when(agencyRepository.findById(AGENCY_ID)).thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> agencyService.setRegistrationUrl(AGENCY_ID, VALID_REGISTRATION_URL, CONSULTANT_ID));
  }

  private Agency captureSavedAgency() {
    var captor = ArgumentCaptor.forClass(Agency.class);
    verify(agencyRepository).save(captor.capture());
    return captor.getValue();
  }
}
