package com.tiba.pts.modules.examscheduling.domain.entity;

import com.tiba.pts.core.domain.BaseEntity;
import com.tiba.pts.modules.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exam_invigilations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamInvigilation extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exam_invigilation_seq")
  @SequenceGenerator(
      name = "exam_invigilation_seq",
      sequenceName = "exam_invigilation_seq",
      allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "exam_room_allocation_id", nullable = false)
  private ExamRoomAllocation examRoomAllocation;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prof_id", nullable = false)
  private User prof;
}
