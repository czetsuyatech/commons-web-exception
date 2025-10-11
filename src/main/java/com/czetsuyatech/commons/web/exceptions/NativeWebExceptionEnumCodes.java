package com.czetsuyatech.commons.web.exceptions;

import java.util.stream.Stream;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum NativeWebExceptionEnumCodes {

  BAD_REQUEST("S400", HttpStatus.BAD_REQUEST, ""),
  INVALID_FORMAT("S401", HttpStatus.BAD_REQUEST, "Invalid format for (field: {0}, value: {1}, type: {2})"),
  HTTP_REQUEST_METHOD_NOT_SUPPORTED_EXCEPTION("S402", HttpStatus.METHOD_NOT_ALLOWED, ""),
  FORBIDDEN("S403", HttpStatus.FORBIDDEN, ""),
  METHOD_ARGUMENT_NOT_VALID("S404", HttpStatus.NOT_FOUND, "Validation failed for fields {0}"),
  NO_RESOURCE_FOUND_EXCEPTION("S405", HttpStatus.NOT_FOUND, ""),
  METHOD_NOT_ALLOWED("S406", HttpStatus.METHOD_NOT_ALLOWED, ""),
  CONSTRAINT_VIOLATION("S407", HttpStatus.INTERNAL_SERVER_ERROR, ""),
  NO_HANDLER("S499", HttpStatus.INTERNAL_SERVER_ERROR, ""),

  RUNTIME("S500", HttpStatus.INTERNAL_SERVER_ERROR, ""),
  INTERNAL_SERVER_ERROR("S501", HttpStatus.INTERNAL_SERVER_ERROR, ""),
  BAD_GATEWAY("S502", HttpStatus.BAD_GATEWAY, "")
  //
  ;

  private final HttpStatusCode httpStatus;
  private final String errorCode;
  private final String message;

  NativeWebExceptionEnumCodes(String errorCode, HttpStatusCode httpStatus, String message) {

    this.errorCode = errorCode;
    this.httpStatus = httpStatus;
    this.message = message;
  }

  public static NativeWebExceptionEnumCodes getByClassName(String className) {

    return Stream.of(NativeWebExceptionEnumCodes.values())
        .filter(e -> e.name().equals(className.toUpperCase()))
        .findFirst()
        .orElse(NativeWebExceptionEnumCodes.INTERNAL_SERVER_ERROR);
  }
}
