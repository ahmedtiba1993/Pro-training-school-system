package com.tiba.pts.modules.room.controller;

import com.tiba.pts.core.dto.ApiResponse;
import com.tiba.pts.modules.room.domain.enums.RoomStatus;
import com.tiba.pts.modules.room.dto.request.RoomRequest;
import com.tiba.pts.modules.room.dto.request.RoomUpdateRequest;
import com.tiba.pts.modules.room.dto.response.RoomResponse;
import com.tiba.pts.modules.room.dto.response.RoomStatusStatsResponse;
import com.tiba.pts.modules.room.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Validated
public class RoomController {

  private final RoomService roomService;

  @PostMapping
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> createRoom(@Valid @RequestBody RoomRequest request) {
    ApiResponse<Long> response =
        ApiResponse.success("ROOM_CREATED_SUCCESSFULLY", roomService.createRoom(request));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<RoomResponse>>> getAllRooms() {
    List<RoomResponse> rooms = roomService.getAllRooms();
    ApiResponse<List<RoomResponse>> response = ApiResponse.success("ROOM_LIST_RETRIEVED", rooms);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Long>> updateRoom(
      @PathVariable Long id, @Valid @RequestBody RoomUpdateRequest request) {
    Long updatedId = roomService.updateRoom(id, request);
    ApiResponse<Long> response = ApiResponse.success("ROOM_UPDATED_SUCCESSFULLY", updatedId);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> changeRoomStatus(
      @PathVariable Long id, @RequestParam RoomStatus newStatus) {
    roomService.changeStatus(id, newStatus);
    return ResponseEntity.ok(ApiResponse.success("ROOM_STATUS_CHANGED"));
  }

  @GetMapping("/stats")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<RoomStatusStatsResponse>> getRoomStatusStats() {
    RoomStatusStatsResponse stats = roomService.getRoomStatusStats();
    ApiResponse<RoomStatusStatsResponse> response =
        ApiResponse.success("ROOM_STATS_RETRIEVED", stats);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/active")
  @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<RoomResponse>>> getActiveRooms() {
    List<RoomResponse> rooms = roomService.getActiveRooms();
    ApiResponse<List<RoomResponse>> response =
        ApiResponse.success("ACTIVE_ROOM_LIST_RETRIEVED", rooms);
    return ResponseEntity.ok(response);
  }
}
