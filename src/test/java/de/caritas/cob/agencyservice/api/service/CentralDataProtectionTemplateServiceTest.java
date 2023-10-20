package de.caritas.cob.agencyservice.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import de.caritas.cob.agencyservice.api.model.DataProtectionContactDTO;
import de.caritas.cob.agencyservice.api.repository.agency.Agency;
import de.caritas.cob.agencyservice.api.repository.agency.DataProtectionResponsibleEntity;
import de.caritas.cob.agencyservice.api.util.JsonConverter;
import de.caritas.cob.agencyservice.tenantservice.generated.web.model.AgencyContextDTO;
import de.caritas.cob.agencyservice.tenantservice.generated.web.model.Content;
import de.caritas.cob.agencyservice.tenantservice.generated.web.model.DataProtectionContactTemplateDTO;
import de.caritas.cob.agencyservice.tenantservice.generated.web.model.DataProtectionOfficerDTO;
import de.caritas.cob.agencyservice.tenantservice.generated.web.model.RestrictedTenantDTO;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("testing")
@AutoConfigureTestDatabase(replace = Replace.ANY)
class CentralDataProtectionTemplateServiceTest {

  public static final String DATA_PROTECTION_OFFICER_CONTACT_TEMPLATE = "Data protection officer contact name: <#if name?exists>${name},</#if><#if city?exists> city: ${city}, </#if><#if postCode?exists>postcode: ${postCode}, </#if><#if phoneNumber?exists>phoneNumber: ${phoneNumber}</#if>";
  public static final String RESPONSIBLE_CONTACT_TEMPLATE = "Data protection responsible contact name: <#if name?exists>${name},</#if><#if city?exists> city: ${city}, </#if><#if postCode?exists>postcode: ${postCode}, </#if><#if phoneNumber?exists>phoneNumber: ${phoneNumber}</#if>";
  @Autowired
  CentralDataProtectionTemplateService centralDataProtectionTemplateService;

  @MockBean
  TenantService tenantService;

  @Test
  void renderDataProtectionPrivacy_shouldProperlyRenderPrivacy_When_PlaceholdersAreRendered() {

    // given
    when(tenantService.getRestrictedTenantDataByTenantId(anyLong())).thenReturn(
        new RestrictedTenantDTO()
            .content(
                new Content().dataProtectionContactTemplate(getDataProtectionContactTemplate())
                    .privacy(
                        "Privacy template with placeholders: ${dataProtectionOfficer} ${responsible}")));
    DataProtectionContactDTO dataProtectionContactDTO = new DataProtectionContactDTO()
        .nameAndLegalForm("Max Mustermann");

    Agency agency = Agency.builder()
        .id(1000L)
        .tenantId(1L)
        .consultingTypeId(1)
        .name("agencyName")
        .dataProtectionResponsibleEntity(DataProtectionResponsibleEntity.DATA_PROTECTION_OFFICER)
        .dataProtectionOfficerContactData(JsonConverter.convertToJson(dataProtectionContactDTO))
        .dataProtectionAgencyContactData(JsonConverter.convertToJson(dataProtectionContactDTO))
        .build();

    // when
    var renderedPrivacy = centralDataProtectionTemplateService.renderPrivacyTemplateWithRenderedPlaceholderValues(
        agency);

    // then
    assertThat(
        renderedPrivacy).isEqualTo(
        "Privacy template with placeholders: Data protection officer contact name: Max Mustermann, Data protection responsible contact name: Max Mustermann,");
  }

  @Test
  void renderDataProtectionTemplatePlaceholders_shouldProperlyRenderPlaceholders_If_SomeVariableDataIsMissing() {

    // given
    when(tenantService.getRestrictedTenantDataByTenantId(anyLong())).thenReturn(
        new RestrictedTenantDTO()
            .content(
                new Content().dataProtectionContactTemplate(getDataProtectionContactTemplate())));
    DataProtectionContactDTO dataProtectionContactDTO = new DataProtectionContactDTO()
        .nameAndLegalForm("Max Mustermann");

    Agency agency = Agency.builder()
        .id(1000L)
        .tenantId(1L)
        .consultingTypeId(1)
        .name("agencyName")
        .dataProtectionResponsibleEntity(DataProtectionResponsibleEntity.DATA_PROTECTION_OFFICER)
        .dataProtectionOfficerContactData(JsonConverter.convertToJson(dataProtectionContactDTO))
        .dataProtectionAgencyContactData(JsonConverter.convertToJson(dataProtectionContactDTO))
        .build();

    // when
    var renderedPlaceholders = centralDataProtectionTemplateService.renderDataProtectionPlaceholdersFromTemplates(
        agency);

    // then
    assertThat(
        renderedPlaceholders).containsEntry(DataProtectionPlaceHolderType.DATA_PROTECTION_OFFICER,
        "Data protection officer contact name: Max Mustermann,").containsEntry(
        DataProtectionPlaceHolderType.DATA_PROTECTION_RESPONSIBLE,
        "Data protection responsible contact name: Max Mustermann,");
  }

  @Test
  void renderDataProtectionTemplatePlaceholders_shouldProperlyRenderPlaceholders() {

    // given
    when(tenantService.getRestrictedTenantDataByTenantId(anyLong())).thenReturn(
        new RestrictedTenantDTO()
            .content(
                new Content().dataProtectionContactTemplate(getDataProtectionContactTemplate())));
    DataProtectionContactDTO dataProtectionContactDTO = new DataProtectionContactDTO()
        .nameAndLegalForm("Max Mustermann")
        .street("Musterstraße 1")
        .postcode("12345")
        .city("Freiburg")
        .phoneNumber("0123456789");

    Agency agency = Agency.builder()
        .id(1000L)
        .tenantId(1L)
        .consultingTypeId(1)
        .name("agencyName")
        .dataProtectionResponsibleEntity(DataProtectionResponsibleEntity.DATA_PROTECTION_OFFICER)
        .dataProtectionOfficerContactData(JsonConverter.convertToJson(dataProtectionContactDTO))
        .dataProtectionAgencyContactData(JsonConverter.convertToJson(dataProtectionContactDTO))
        .build();

    // when
    var renderedPlaceholders = centralDataProtectionTemplateService.renderDataProtectionPlaceholdersFromTemplates(
        agency);

    // then
    assertThat(
        renderedPlaceholders).containsEntry(DataProtectionPlaceHolderType.DATA_PROTECTION_OFFICER,
            "Data protection officer contact name: Max Mustermann, city: Freiburg, postcode: 12345, phoneNumber: 0123456789")
        .containsEntry(
            DataProtectionPlaceHolderType.DATA_PROTECTION_RESPONSIBLE,
            "Data protection responsible contact name: Max Mustermann, city: Freiburg, postcode: 12345, phoneNumber: 0123456789");
  }

  @Test
  void renderDataProtectionTemplatePlaceholders_shouldReturnPlaceholderTemplate_IfDataProtectionAgencyContactDataIsNotSet() {

    // given
    when(tenantService.getRestrictedTenantDataByTenantId(anyLong())).thenReturn(
        new RestrictedTenantDTO()
            .content(
                new Content().dataProtectionContactTemplate(getDataProtectionContactTemplate())));

    Agency agency = Agency.builder()
        .id(1000L)
        .tenantId(1L)
        .consultingTypeId(1)
        .name("agencyName")
        .dataProtectionResponsibleEntity(DataProtectionResponsibleEntity.DATA_PROTECTION_OFFICER)
        .build();

    // when
    var renderedPlaceholders = centralDataProtectionTemplateService.renderDataProtectionPlaceholdersFromTemplates(
        agency);

    // then
    assertThat(
        renderedPlaceholders).containsEntry(DataProtectionPlaceHolderType.DATA_PROTECTION_OFFICER,
        "Data protection officer contact name: ").containsEntry(
        DataProtectionPlaceHolderType.DATA_PROTECTION_RESPONSIBLE,
        "Data protection responsible contact name: ").hasSize(2);
  }


  @Test
  void renderDataProtectionTemplatePlaceholders_shouldReturnPlaceholderTemplate_IfNoDataOnAgency() {

    // given
    when(tenantService.getRestrictedTenantDataByTenantId(anyLong())).thenReturn(
        new RestrictedTenantDTO()
            .content(
                new Content().dataProtectionContactTemplate(getDataProtectionContactTemplate())));

    Agency agency = Agency.builder()
        .id(1000L)
        .tenantId(1L)
        .consultingTypeId(1)
        .name("agencyName")
        .dataProtectionResponsibleEntity(DataProtectionResponsibleEntity.DATA_PROTECTION_OFFICER)
        .build();

    // when
    var renderedPlaceholders = centralDataProtectionTemplateService.renderDataProtectionPlaceholdersFromTemplates(
        agency);

    // then
    assertThat(
        renderedPlaceholders).containsEntry(DataProtectionPlaceHolderType.DATA_PROTECTION_OFFICER,
        "Data protection officer contact name: ").containsEntry(
        DataProtectionPlaceHolderType.DATA_PROTECTION_RESPONSIBLE,
        "Data protection responsible contact name: ").hasSize(2);
  }


  private DataProtectionContactTemplateDTO getDataProtectionContactTemplate() {
    return new DataProtectionContactTemplateDTO().agencyContext(
        getAgencyContext());
  }

  private AgencyContextDTO getAgencyContext() {
    return new AgencyContextDTO().dataProtectionOfficer(
            new DataProtectionOfficerDTO().dataProtectionOfficerContact(
                DATA_PROTECTION_OFFICER_CONTACT_TEMPLATE))
        .responsibleContact(
            RESPONSIBLE_CONTACT_TEMPLATE);
  }


}