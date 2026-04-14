
package com.ipos.ipos_sa.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Integration service for sending emails via the IPOS-PU subsystem.
 *
 * <p>PU exposes POST /api/email/send which accepts { "to", "subject", "body" } and returns {
 * "success": true/false }.
 *
 * <p>SA calls this service whenever it needs to notify merchants — for example:
 *
 * <ul>
 *   <li>Account status changes (suspended, in-default)
 *   <li>Order dispatched notifications
 *   <li>Payment reminders
 *   <li>Commercial application approval/rejection
 * </ul>
 *
 * <p>If PU is unreachable, the failure is logged but does not block the calling operation — email is
 * best-effort, not transactional.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PUEmailService {

  private final RestTemplate restTemplate;

  /**
   * Base URL of the PU subsystem. Defaults to http://localhost:8082. Override in
   * application.properties with: pu.base-url=http://localhost:XXXX
   */
  @Value("${pu.base-url:http://localhost:8082}")
  private String puBaseUrl;

  /**
   * Sends an email via PU's email service.
   *
   * @param to recipient email address
   * @param subject email subject line
   * @param body email body text
   * @return true if PU confirmed the email was sent, false otherwise
   */
  public boolean sendEmail(String to, String subject, String body) {
    String url = puBaseUrl + "/api/email/send";

    Map<String, String> payload = Map.of("to", to, "subject", subject, "body", body);

    try {
      @SuppressWarnings("unchecked")
      ResponseEntity<Map> response = restTemplate.postForEntity(url, payload, Map.class);

      if (response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"))) {
        log.info("Email sent via PU to={}, subject=\"{}\"", to, subject);
        return true;
      } else {
        log.warn("PU email service returned failure for to={}, subject=\"{}\"", to, subject);
        return false;
      }
    } catch (RestClientException e) {
      log.error(
          "Failed to reach PU email service at {} — email not sent to={}, subject=\"{}\": {}",
          url,
          to,
          subject,
          e.getMessage());
      return false;
    }
  }
}
