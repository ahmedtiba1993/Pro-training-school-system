package com.tiba.pts.core.exception;

import com.tiba.pts.core.dto.ErrorDetail;
import lombok.Getter;

import java.util.List;

@Getter
public class BusinessValidationException extends RuntimeException {

  private final List<ErrorDetail> errors;

  public BusinessValidationException(String message, List<ErrorDetail> errors) {
    super(message);
    this.errors = errors;
  }
}
