package com.czetsuyatech.commons.web.exceptions;

import org.springframework.http.HttpStatusCode;

public class WebException extends WebBaseException {

  public WebException(HttpStatusCode status, String code) {
    this(status, code, null);
  }

  public WebException(HttpStatusCode status, String code, String message) {
    super(status, code, message);
  }
}
