package com.tiba.pts.modules.profiles.mapper;

import com.tiba.pts.modules.profiles.domain.entity.RefTeacherSpecialty;
import com.tiba.pts.modules.profiles.domain.enums.TeacherStatus; // <-- Ne pas oublier cet import
import com.tiba.pts.modules.profiles.dto.request.RefTeacherSpecialtyRequest;
import com.tiba.pts.modules.profiles.dto.response.RefTeacherSpecialtyResponse;
import com.tiba.pts.modules.profiles.dto.response.RefTeacherSpecialtySimpleResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface RefTeacherSpecialtyMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "teachers", ignore = true)
  RefTeacherSpecialty toEntity(RefTeacherSpecialtyRequest request);

  // 1. On modifie l'expression pour appeler notre nouvelle méthode personnalisée 👇
  @Mapping(target = "teacherCount", expression = "java(countActiveTeachers(entity))")
  RefTeacherSpecialtyResponse toResponse(RefTeacherSpecialty entity);

  List<RefTeacherSpecialtyResponse> toResponseList(List<RefTeacherSpecialty> entities);

  // MAPPERS POUR LA VERSION SANS COMPTEUR
  RefTeacherSpecialtySimpleResponse toSimpleResponse(RefTeacherSpecialty entity);

  List<RefTeacherSpecialtySimpleResponse> toSimpleResponseList(List<RefTeacherSpecialty> entities);

  // 2. NOUVELLE MÉTHODE (Mot-clé 'default' pour écrire du code Java dans l'interface MapStruct)
  default int countActiveTeachers(RefTeacherSpecialty entity) {
    if (entity.getTeachers() == null || entity.getTeachers().isEmpty()) {
      return 0;
    }

    // On filtre le Set des profs pour ne garder que les actifs, et on les compte
    return (int)
        entity.getTeachers().stream()
            .filter(teacher -> teacher.getStatus() == TeacherStatus.ACTIVE)
            .count();
  }
}
