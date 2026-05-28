package com.tiba.pts.modules.room.mapper;

import com.tiba.pts.modules.room.domain.entity.Room;
import com.tiba.pts.modules.room.dto.request.RoomRequest;
import com.tiba.pts.modules.room.dto.request.RoomUpdateRequest;
import com.tiba.pts.modules.room.dto.response.RoomResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface RoomMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "code", ignore = true)
  @Mapping(target = "status", ignore = true)
  Room toEntity(RoomRequest request);

  RoomResponse toResponse(Room room);

  void updateEntityFromRequest(RoomUpdateRequest request, @MappingTarget Room room);
}
