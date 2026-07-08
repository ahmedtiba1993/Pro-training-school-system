package com.tiba.pts.modules.examscheduling.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.room.domain.entity.Room;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_room_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamRoomAllocation extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exam_room_allocation_seq")
  @SequenceGenerator(
      name = "exam_room_allocation_seq",
      sequenceName = "exam_room_allocation_seq",
      allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "exam_schedule_id", nullable = false)
  private ExamSchedule examSchedule;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "room_id", nullable = false)
  private Room room;

  @Min(value = 1, message = "CAPACITY_USED_MUST_BE_GREATER_THAN_ZERO")
  @Column(name = "capacity_used", nullable = false)
  private Integer capacityUsed;

  @OneToMany(mappedBy = "examRoomAllocation", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<ExamInvigilation> invigilations = new ArrayList<>();
}
