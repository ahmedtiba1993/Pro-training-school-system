package com.tiba.pts.modules.room.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.room.domain.enums.RoomStatus;
import com.tiba.pts.modules.room.domain.enums.RoomType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "room_seq")
  @SequenceGenerator(name = "room_seq", sequenceName = "room_seq", allocationSize = 1)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String emplacement;

  @Column(nullable = false)
  private Integer capacity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RoomType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private RoomStatus status = RoomStatus.DRAFT;
}
