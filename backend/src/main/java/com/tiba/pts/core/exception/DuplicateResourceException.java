package com.tiba.pts.core.exception;

import com.tiba.pts.core.dto.ErrorDetail;
import lombok.Getter;

import java.util.List;

@Getter
public class DuplicateResourceException extends RuntimeException {

  private final List<ErrorDetail> conflicts;

  public DuplicateResourceException(String message, List<ErrorDetail> conflicts) {
    super(message);
    this.conflicts = conflicts;
  }
}
