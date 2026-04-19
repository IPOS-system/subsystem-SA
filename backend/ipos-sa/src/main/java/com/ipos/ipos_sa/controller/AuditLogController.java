package com.ipos.ipos_sa.controller;

import com.ipos.ipos_sa.dto.audit.AuditLogDTO;
import com.ipos.ipos_sa.entity.AuditLog;
import com.ipos.ipos_sa.entity.User;
import com.ipos.ipos_sa.exception.AccessDeniedException;
import com.ipos.ipos_sa.repository.AuditLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the audit log.
 * GET /api/audit-log — ADMIN only.
 */
@RestController
@RequestMapping("/api/audit-log")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuditLogController {

  private final AuditLogRepository auditLogRepository;

  /**
   * GET /api/audit-log
   * Returns audit log entries, newest first.
   */
  @GetMapping
  public ResponseEntity<List<AuditLogDTO>> getAuditLog(
      @RequestParam(required = false) Integer userId,
      @RequestParam(required = false) String targetType,
      @RequestParam(required = false) String targetId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
      Authentication auth) {

    requireRole(auth, User.Role.ADMIN);

    List<AuditLog> entries;

    if (userId != null) {
      entries = auditLogRepository.findByUser_UserIdOrderByLoggedAtDesc(userId);
    } else if (targetType != null && targetId != null) {
      entries = auditLogRepository.findByTargetTypeAndTargetIdOrderByLoggedAtDesc(targetType, targetId);
    } else if (from != null && to != null) {
      entries = auditLogRepository.findByLoggedAtBetweenOrderByLoggedAtDesc(from, to);
    } else {
      entries = auditLogRepository.findAll(
          org.springframework.data.domain.Sort.by(
              org.springframework.data.domain.Sort.Direction.DESC, "loggedAt"));
    }

    List<AuditLogDTO> dtos = entries.stream()
        .map(log -> AuditLogDTO.builder()
            .logId(log.getLogId())
            .userId(log.getUser() != null ? log.getUser().getUserId() : null)
            .username(log.getUser() != null ? log.getUser().getUsername() : null)
            .action(log.getAction())
            .targetType(log.getTargetType())
            .targetId(log.getTargetId())
            .details(log.getDetails())
            .loggedAt(log.getLoggedAt())
            .build())
        .collect(Collectors.toList());

    return ResponseEntity.ok(dtos);
  }

  private void requireRole(Authentication auth, User.Role... permitted) {
    String roleStr = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    User.Role callerRole = User.Role.valueOf(roleStr);
    for (User.Role r : permitted) {
      if (r == callerRole) return;
    }
    throw new AccessDeniedException(
        "Role " + callerRole + " is not permitted to perform this action.");
  }
}