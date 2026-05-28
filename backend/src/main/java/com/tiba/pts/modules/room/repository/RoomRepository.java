package com.tiba.pts.modules.room.repository;

import com.tiba.pts.modules.room.domain.entity.Room;
import com.tiba.pts.modules.room.domain.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
  boolean existsByNameIgnoreCase(String name);

  boolean existsByCode(String code);

  // Uniqueness check for modification
  boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

  long countByStatus(RoomStatus status);
}
