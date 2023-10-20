package de.caritas.cob.agencyservice.api.service;


import com.google.common.collect.Maps;
import de.caritas.cob.agencyservice.api.model.DataProtectionContactDTO;
import de.caritas.cob.agencyservice.api.repository.agency.Agency;
import de.caritas.cob.agencyservice.api.util.JsonConverter;
import de.caritas.cob.agencyservice.tenantservice.generated.web.model.DataProtectionContactTemplateDTO;
import de.caritas.cob.agencyservice.tenantservice.generated.web.model.DataProtectionOfficerDTO;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import de.caritas.cob.agencyservice.tenantservice.generated.web.model.RestrictedTenantDTO;

@Service
@RequiredArgsConstructor
@Slf4j
public class CentralDataProtectionTemplateService {

  private final @NonNull TenantService tenantService;

  private final @NonNull TemplateRenderer templateRenderer;

  public String renderPrivacyTemplateWithRenderedPlaceholderValues(Agency agency) {
    RestrictedTenantDTO restrictedTenantDataByTenantId = tenantService.getRestrictedTenantDataByTenantId(
        agency.getTenantId());
    if (restrictedTenantDataByTenantId != null && restrictedTenantDataByTenantId.getContent() != null) {
      return renderPrivacyTemplateWithRenderedPlaceholderValues(agency, restrictedTenantDataByTenantId);
    }
    log.debug("No privacy content set for tenant with id: {}", agency.getTenantId());
    return null;
  }

  @Nullable
  private String renderPrivacyTemplateWithRenderedPlaceholderValues(Agency agency, RestrictedTenantDTO restrictedTenantDataByTenantId) {
    var renderedPlaceholdersMap = renderDataProtectionPlaceholdersFromTemplates(agency);
    Map<String, Object> dataModel = renderedPlaceholdersMap.entrySet().stream()
        .collect(Collectors.toMap(entry -> entry.getKey().getPlaceholderVariable(),
            Entry::getValue));
    String privacyTemplate = restrictedTenantDataByTenantId.getContent().getPrivacy();
    try {
      return templateRenderer.renderTemplate(privacyTemplate,
          dataModel);
    } catch (Exception e) {
      log.error("Error while rendering data protection template: {}", e.getMessage());
      return null;
    }
  }

  protected Map<DataProtectionPlaceHolderType, String> renderDataProtectionPlaceholdersFromTemplates(
      Agency agency) {
    RestrictedTenantDTO restrictedTenantDataByTenantId = tenantService.getRestrictedTenantDataByTenantId(
        agency.getTenantId());

    Map<DataProtectionPlaceHolderType, String> result = Maps.newHashMap();
    if (restrictedTenantDataByTenantId.getContent() != null
        && restrictedTenantDataByTenantId.getContent().getDataProtectionContactTemplate() != null) {
      var renderedDataProtectionOfficerContact = renderDataProtectionOfficerContactFromTemplate(
          agency, restrictedTenantDataByTenantId.getContent().getDataProtectionContactTemplate());
      result.put(DataProtectionPlaceHolderType.DATA_PROTECTION_OFFICER,
          renderedDataProtectionOfficerContact);
      var renderedDataProtectionResponsible = renderDataProtectionResponsibleFromTemplate(
          agency, restrictedTenantDataByTenantId.getContent().getDataProtectionContactTemplate());
      result.put(DataProtectionPlaceHolderType.DATA_PROTECTION_RESPONSIBLE,
          renderedDataProtectionResponsible);
    }
    return result;
  }

  @Nullable
  private String renderDataProtectionResponsibleFromTemplate(Agency agency,
      DataProtectionContactTemplateDTO dataProtectionContactTemplateDTO) {
    if (isDataProtectionResponsibleTemplateAvailable(dataProtectionContactTemplateDTO)) {
      return renderDataProtectionResponsibleFromTemplateIfAvailable(agency, dataProtectionContactTemplateDTO);
    } else {
      log.warn("No data protection responsible template set for tenant with id: {}",
          agency.getTenantId());
    }
    return null;
  }

  private String renderDataProtectionResponsibleFromTemplateIfAvailable(Agency agency,
      DataProtectionContactTemplateDTO dataProtectionContactTemplateDTO) {
    var agencyContact = JsonConverter.convertFromJsonNullSafe(
        agency.getDataProtectionAgencyContactData());
    return renderDataProtectionContactTemplate(
        dataProtectionContactTemplateDTO.getAgencyContext().getResponsibleContact(), agencyContact);
  }

  @Nullable
  private String renderDataProtectionOfficerContactFromTemplate(Agency agency,
      DataProtectionContactTemplateDTO dataProtectionContactTemplateDTO) {
    if (isDataProtectionOfficerTemplateAvailable(dataProtectionContactTemplateDTO)) {
      return renderDataProtectionOfficerContactFromTemplateIfAvailable(agency, dataProtectionContactTemplateDTO);
    } else {
      log.warn("No data protection officer template set for tenant with id: {}",
          agency.getTenantId());
    }
    return null;
  }

  @Nullable
  private String renderDataProtectionOfficerContactFromTemplateIfAvailable(Agency agency,
      DataProtectionContactTemplateDTO dataProtectionContactTemplateDTO) {
    final DataProtectionOfficerDTO dataProtectionOfficerDTO = dataProtectionContactTemplateDTO.getAgencyContext()
        .getDataProtectionOfficer();
    switch (agency.getDataProtectionResponsibleEntity()) {
      case DATA_PROTECTION_OFFICER -> {
        var contactDataDTO = JsonConverter.convertFromJsonNullSafe(
            agency.getDataProtectionOfficerContactData());
        return renderDataProtectionContactTemplate(
            dataProtectionOfficerDTO.getDataProtectionOfficerContact(), contactDataDTO);
      }
      case ALTERNATIVE_REPRESENTATIVE -> {
        var alternativeContact = JsonConverter.convertFromJsonNullSafe(
            agency.getDataProtectionAlternativeContactData());
        return renderDataProtectionContactTemplate(
            dataProtectionOfficerDTO.getAlternativeRepresentativeContact(),
            alternativeContact);
      }
      case AGENCY_RESPONSIBLE -> {
        var agencyContact = JsonConverter.convertFromJsonNullSafe(
            agency.getDataProtectionAgencyContactData());
        return renderDataProtectionContactTemplate(
            dataProtectionOfficerDTO.getAgencyResponsibleContact(), agencyContact);
      }
      default -> {
        log.error("No data protection responsible entity set for agency with id: {}",
            agency.getId());
        return null;
      }
    }
  }

  private boolean isDataProtectionResponsibleTemplateAvailable(
      DataProtectionContactTemplateDTO dataProtectionContactTemplateDTO) {
    return isDataProtectionAgencyContextAvailable(dataProtectionContactTemplateDTO)
        && dataProtectionContactTemplateDTO.getAgencyContext().getResponsibleContact() != null;
  }

  private boolean isDataProtectionOfficerTemplateAvailable(
      DataProtectionContactTemplateDTO dataProtectionContactTemplateDTO) {
    return isDataProtectionAgencyContextAvailable(dataProtectionContactTemplateDTO)
        && dataProtectionContactTemplateDTO.getAgencyContext().getDataProtectionOfficer() != null;
  }

  private boolean isDataProtectionAgencyContextAvailable(DataProtectionContactTemplateDTO dataProtectionContactTemplateDTO) {
    return dataProtectionContactTemplateDTO != null
        && dataProtectionContactTemplateDTO.getAgencyContext() != null;
  }

  private String renderDataProtectionContactTemplate(
      String templateToRender, DataProtectionContactDTO dataProtectionContactDTO) {
    try {
      return templateRenderer.renderTemplate(templateToRender,
          dataProtectionDTOToMap(dataProtectionContactDTO));
    } catch (Exception e) {
      log.error("Error while rendering data protection template: {}", e.getMessage());
      return null;
    }
  }

  private Map<String, Object> dataProtectionDTOToMap(DataProtectionContactDTO dto) {
    Map<String, Object> result = Maps.newHashMap();
    result.computeIfAbsent("name", key -> dto.getNameAndLegalForm());
    result.computeIfAbsent("street", key -> dto.getStreet());
    result.computeIfAbsent("postCode", key -> dto.getPostcode());
    result.computeIfAbsent("city", key -> dto.getCity());
    result.computeIfAbsent("phoneNumber", key -> dto.getPhoneNumber());
    result.computeIfAbsent("email", key -> dto.getEmail());
    return result;
  }
}
