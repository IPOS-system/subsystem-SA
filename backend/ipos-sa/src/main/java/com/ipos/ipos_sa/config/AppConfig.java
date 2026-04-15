package com.ipos.ipos_sa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** General application configuration — beans shared across services. */
@Configuration
public class AppConfig {

  /** RestTemplate bean for outbound HTTP calls to other IPOS subsystems (e.g. PU email). */
  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
