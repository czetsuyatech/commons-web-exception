package com.czetsuyatech.commons.web.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ResourceNotFoundException extends WebBaseException {

  private static final String MESSAGE_TEMPLATE = "Resource with %s=%s not found";
  private String key;
  private String value;

  public ResourceNotFoundException(String code, String key, String value) {

    super(HttpStatus.BAD_REQUEST, code, String.format(MESSAGE_TEMPLATE, key, value));

    this.key = key;
    this.value = value;
  }
}
