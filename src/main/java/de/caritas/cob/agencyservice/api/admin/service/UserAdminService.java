package de.caritas.cob.agencyservice.api.admin.service;

import de.caritas.cob.agencyservice.api.service.TenantHeaderSupplier;
import de.caritas.cob.agencyservice.api.service.securityheader.SecurityHeaderSupplier;
import de.caritas.cob.agencyservice.config.apiclient.UserAdminServiceApiControllerFactory;
import de.caritas.cob.agencyservice.useradminservice.generated.ApiClient;
import de.caritas.cob.agencyservice.useradminservice.generated.web.AdminUserControllerApi;
import de.caritas.cob.agencyservice.useradminservice.generated.web.model.AgencyTypeDTO;
import de.caritas.cob.agencyservice.useradminservice.generated.web.model.AgencyTypeDTO.AgencyTypeEnum;
import de.caritas.cob.agencyservice.useradminservice.generated.web.model.ConsultantAdminResponseDTO;

import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import de.caritas.cob.agencyservice.useradminservice.generated.web.model.AdminResponseDTO;

@Service
@RequiredArgsConstructor
public class UserAdminService {

  private final @NonNull UserAdminServiceApiControllerFactory userAdminServiceApiControllerFactory;
  private final @NonNull SecurityHeaderSupplier securityHeaderSupplier;
  private final @NonNull TenantHeaderSupplier tenantHeaderSupplier;

  /**
   * Change the assigned consultants of an agency when type of agency is changed from team-agency to
   * default and vice-versa.
   *
   * @param agencyId   the id of the agency
   * @param agencyType Type to decide if assigned consultants will be removed from team sessions or
   *                   tagged as team consultants
   */
  public void adaptRelatedConsultantsForChange(Long agencyId, String agencyType) {
    AdminUserControllerApi controllerApi = userAdminServiceApiControllerFactory.createControllerApi();
    addDefaultHeaders(controllerApi.getApiClient());
    controllerApi
        .changeAgencyType(agencyId,
            new AgencyTypeDTO()
                .agencyType(AgencyTypeEnum.fromValue(agencyType)));
  }

  protected void addDefaultHeaders(ApiClient apiClient) {
    HttpHeaders headers = this.securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders();
    tenantHeaderSupplier.addTenantHeader(headers);
    headers.forEach((key, value) -> apiClient.addDefaultHeader(key, value.iterator().next()));
  }

  public AdminResponseDTO getAdminUser(String userId) {
    var controllerApi = userAdminServiceApiControllerFactory.createControllerApi();
    addDefaultHeaders(controllerApi.getApiClient());
    return controllerApi.getAgencyAdmin(userId);
  }

  public List<Long> getAdminUserAgencyIds(String userId) {
    var controllerApi = userAdminServiceApiControllerFactory.createControllerApi();
    addDefaultHeaders(controllerApi.getApiClient());
    return controllerApi.getAdminAgencies(userId);
  }



  /**
   * Returns a list of {@link ConsultantAdminResponseDTO} for the provided agency ID.
   *
   * @param agencyId    agency ID
   * @return list of {@link ConsultantAdminResponseDTO}
   */
  public List<ConsultantAdminResponseDTO> getConsultantsOfAgency(Long agencyId) {
    var controllerApi = userAdminServiceApiControllerFactory.createControllerApi();
    addDefaultHeaders(controllerApi.getApiClient());

    return controllerApi.getAgencyConsultants(String.valueOf(agencyId)).getEmbedded();
  }
}
