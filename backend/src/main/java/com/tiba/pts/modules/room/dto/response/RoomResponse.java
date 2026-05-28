package com.tiba.pts.modules.room.dto.response;

import com.tiba.pts.modules.room.domain.enums.RoomStatus;
import com.tiba.pts.modules.room.domain.enums.RoomType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {
  private Long id;
  private String code;
  private String name;
  private String emplacement;
  private Integer capacity;
  private RoomType type;
  private RoomStatus status;
}
