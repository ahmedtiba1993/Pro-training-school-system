package com.tiba.pts.modules.room.service;

import com.tiba.pts.core.exception.EntityAlreadyExistsException;
import com.tiba.pts.core.exception.ResourceNotFoundException;
import com.tiba.pts.modules.room.domain.entity.Room;
import com.tiba.pts.modules.room.domain.enums.RoomStatus;
import com.tiba.pts.modules.room.dto.request.RoomRequest;
import com.tiba.pts.modules.room.dto.request.RoomUpdateRequest;
import com.tiba.pts.modules.room.dto.response.RoomResponse;
import com.tiba.pts.modules.room.dto.response.RoomStatusStatsResponse;
import com.tiba.pts.modules.room.mapper.RoomMapper;
import com.tiba.pts.modules.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

  private final RoomRepository roomRepository;
  private final RoomMapper roomMapper;

  @Transactional
  public long createRoom(RoomRequest request) {
    if (roomRepository.existsByNameIgnoreCase(request.name())) {
      throw new EntityAlreadyExistsException("ROOM_NAME_ALREADY_EXISTS");
    }

    Room room = roomMapper.toEntity(request);
    String generatedCode = this.generateSlugCode(room.getName());
    room.setCode(generatedCode);

    room.setStatus(RoomStatus.DRAFT);

    return roomRepository.save(room).getId();
  }

  @Transactional(readOnly = true)
  public List<RoomResponse> getAllRooms() {
    return roomRepository.findAll().stream().map(roomMapper::toResponse).toList();
  }

  @Transactional
  public Long updateRoom(Long id, RoomUpdateRequest request) {
    Room room =
        roomRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ROOM_NOT_FOUND"));

    // Name uniqueness validation (outside this entity)
    if (roomRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
      throw new EntityAlreadyExistsException("ROOM_NAME_ALREADY_EXISTS");
    }

    // Mapping of modifications (Name, Capacity, Type)
    roomMapper.updateEntityFromRequest(request, room);

    return roomRepository.save(room).getId();
  }

  @Transactional
  public void changeStatus(Long id, RoomStatus newStatus) {
    Room room =
        roomRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ROOM_NOT_FOUND"));

    room.setStatus(newStatus);
    roomRepository.save(room);
  }

  private String generateSlugCode(String name) {
    if (name == null) {
      return null;
    }
    String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
    String withoutAccents = normalized.replaceAll("\\p{M}", "");
    String withUnderscores = withoutAccents.replaceAll("[\\s-]+", "_");
    String cleanString = withUnderscores.replaceAll("[^a-zA-Z0-9_]", "");
    return cleanString.toUpperCase();
  }

  @Transactional(readOnly = true)
  public RoomStatusStatsResponse getRoomStatusStats() {
    // Retrieval of specific counts
    long active = roomRepository.countByStatus(RoomStatus.ACTIVE);
    long maintenance = roomRepository.countByStatus(RoomStatus.MAINTENANCE);

    // Construction of the consolidated response
    return RoomStatusStatsResponse.builder()
        .activeCount(active)
        .maintenanceCount(maintenance)
        .totalOperationalCount(active + maintenance)
        .build();
  }

  @Transactional(readOnly = true)
  public List<RoomResponse> getActiveRooms() {
    return roomRepository.findAllByStatus(RoomStatus.ACTIVE).stream()
        .map(roomMapper::toResponse)
        .toList();
  }
}
