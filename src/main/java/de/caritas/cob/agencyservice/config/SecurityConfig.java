package de.caritas.cob.agencyservice.config;

import static de.caritas.cob.agencyservice.api.authorization.Authority.AGENCY_ADMIN;

import de.caritas.cob.agencyservice.api.authorization.RoleAuthorizationAuthorityMapper;
import de.caritas.cob.agencyservice.filter.HttpTenantFilter;
import de.caritas.cob.agencyservice.filter.StatelessCsrfFilter;
import de.caritas.cob.agencyservice.filter.SubdomainExtractor;
import de.caritas.cob.agencyservice.api.tenant.TenantResolver;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.client.KeycloakClientRequestFactory;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.keycloak.adapters.springsecurity.filter.KeycloakAuthenticatedActionsFilter;
import org.keycloak.adapters.springsecurity.filter.KeycloakAuthenticationProcessingFilter;
import org.keycloak.adapters.springsecurity.filter.KeycloakPreAuthActionsFilter;
import org.keycloak.adapters.springsecurity.filter.KeycloakSecurityContextRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfFilter;

/**
 * Provides the Keycloak/Spring Security configuration.
 */
@KeycloakConfiguration
public class SecurityConfig extends KeycloakWebSecurityConfigurerAdapter {

  public static final String[] WHITE_LIST =
      new String[]{"/agencies/docs", "/agencies/docs/**", "/v2/api-docs", "/configuration/ui",
          "/swagger-resources/**", "/configuration/security", "/swagger-ui.html", "/webjars/**"};

  @SuppressWarnings("unused")
  private final KeycloakClientRequestFactory keycloakClientRequestFactory;

  @Value("${csrf.cookie.property}")
  private String csrfCookieProperty;

  @Value("${csrf.header.property}")
  private String csrfHeaderProperty;

  @Autowired
  private Environment environment;

  @Value("${multitenancy.enabled}")
  private boolean multitenancy;

  /**
   * Processes HTTP requests and checks for a valid spring security authentication for the
   * (Keycloak) principal (authorization header).
   */
  public SecurityConfig(KeycloakClientRequestFactory keycloakClientRequestFactory) {
    this.keycloakClientRequestFactory = keycloakClientRequestFactory;
  }

  /**
   * Configure spring security filter chain: disable default Spring Boot CSRF token behavior and add
   * custom {@link StatelessCsrfFilter}, set all sessions to be fully stateless, define necessary
   * Keycloak roles for specific REST API paths
   */
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    super.configure(http);
    HttpSecurity httpSecurity = http.csrf().disable()
        .addFilterBefore(new StatelessCsrfFilter(csrfCookieProperty, csrfHeaderProperty),
            CsrfFilter.class);

    if (multitenancy) {
      httpSecurity = httpSecurity
          .addFilterAfter(new HttpTenantFilter(tenantResolver()), KeycloakAuthenticatedActionsFilter.class);
    }

    httpSecurity.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .sessionAuthenticationStrategy(sessionAuthenticationStrategy()).and().authorizeRequests()
        .antMatchers("/agencies/**").permitAll()
        .antMatchers(WHITE_LIST).permitAll()
        .antMatchers("/agencies").permitAll()
        .antMatchers("/agencyadmin", "/agencyadmin/**").hasAuthority(AGENCY_ADMIN.getAuthority())
        .anyRequest().denyAll();
  }

  @Bean
  TenantResolver tenantResolver() {
    return new TenantResolver(subdomainExtractor(), tenantControllerApi());
  }

  @Bean
  SubdomainExtractor subdomainExtractor() {
    return new SubdomainExtractor();
  }

  @Bean
  de.caritas.cob.agencyservice.tenantservice.generated.web.TenantControllerApi tenantControllerApi() {
    return new de.caritas.cob.agencyservice.tenantservice.generated.web.TenantControllerApi();
  }

  /**
   * Use the KeycloakSpringBootConfigResolver to be able to save the Keycloak settings in the spring
   * application properties.
   */
  @Bean
  public KeycloakConfigResolver keyCloakConfigResolver() {
    return new KeycloakSpringBootConfigResolver();
  }

  /**
   * Change springs authentication strategy to be stateless (no session is being created).
   */
  @Bean
  @Override
  protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
    return new NullAuthenticatedSessionStrategy();
  }

  /**
   * Change the default AuthenticationProvider to KeycloakAuthenticationProvider and register it in
   * the spring security context. This maps the Kecloak roles to match the spring security roles
   * (prefix ROLE_).
   */
  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth,
      RoleAuthorizationAuthorityMapper authorityMapper) {
    var keyCloakAuthProvider = keycloakAuthenticationProvider();
    keyCloakAuthProvider.setGrantedAuthoritiesMapper(authorityMapper);

    auth.authenticationProvider(keyCloakAuthProvider);
  }

  /**
   * From the Keycloag documentation: "Spring Boot attempts to eagerly register filter beans with
   * the web application context. Therefore, when running the Keycloak Spring Security adapter in a
   * Spring Boot environment, it may be necessary to add FilterRegistrationBeans to your security
   * configuration to prevent the Keycloak filters from being registered twice."
   * <p>
   * https://github.com/keycloak/keycloak-documentation/blob/master/securing_apps/topics/oidc/java/spring-security-adapter.adoc
   * <p>
   * {@link package.class#member label}
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  @Bean
  public FilterRegistrationBean keycloakAuthenticationProcessingFilterRegistrationBean(
      KeycloakAuthenticationProcessingFilter filter) {
    var registrationBean = new FilterRegistrationBean(filter);
    registrationBean.setEnabled(false);
    return registrationBean;
  }

  /**
   * see above: {@link SecurityConfig#keycloakAuthenticationProcessingFilterRegistrationBean(KeycloakAuthenticationProcessingFilter)
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  @Bean
  public FilterRegistrationBean keycloakPreAuthActionsFilterRegistrationBean(
      KeycloakPreAuthActionsFilter filter) {
    var registrationBean = new FilterRegistrationBean(filter);
    registrationBean.setEnabled(false);
    return registrationBean;
  }

  /**
   * see above: {@link SecurityConfig#keycloakAuthenticationProcessingFilterRegistrationBean(KeycloakAuthenticationProcessingFilter)
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  @Bean
  public FilterRegistrationBean keycloakAuthenticatedActionsFilterBean(
      KeycloakAuthenticatedActionsFilter filter) {
    var registrationBean = new FilterRegistrationBean(filter);
    registrationBean.setEnabled(false);
    return registrationBean;
  }

  /**
   * see above: {@link SecurityConfig#keycloakAuthenticationProcessingFilterRegistrationBean(KeycloakAuthenticationProcessingFilter)
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  @Bean
  public FilterRegistrationBean keycloakSecurityContextRequestFilterBean(
      KeycloakSecurityContextRequestFilter filter) {
    var registrationBean = new FilterRegistrationBean(filter);
    registrationBean.setEnabled(false);
    return registrationBean;
  }
}
