package com.ipos.ipos_sa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** General application configuration. */
@Configuration
public class AppConfig {

  /** RestTemplate bean for outbound HTTP calls to other IPOS subsystems. */
  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
