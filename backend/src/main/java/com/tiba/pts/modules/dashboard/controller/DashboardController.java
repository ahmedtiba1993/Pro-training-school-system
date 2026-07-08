package com.tiba.pts.modules.dashboard.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.dashboard.dto.response.EnrollmentStatsResponse;
import com.tiba.pts.modules.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard/admin")
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardService dashboardService;

  @GetMapping("/enrollment-stats")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<EnrollmentStatsResponse>> getAdminEnrollmentStats() {
    EnrollmentStatsResponse response = dashboardService.getAdminEnrollmentStats();
    return ResponseEntity.ok(ApiResponse.success("ADMIN_DASHBOARD_STATS_RETRIEVED", response));
  }
}
