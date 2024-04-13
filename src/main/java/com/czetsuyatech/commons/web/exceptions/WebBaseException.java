package com.czetsuyatech.commons.web.exceptions;

import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;


@Getter
@ToString
public class WebBaseException extends ErrorResponseException {

  public WebBaseException(HttpStatusCode status) {
    super(status);
  }

  public WebBaseException(HttpStatusCode status, String code, String message) {
    this(status, code, message, null);
  }

  public WebBaseException(HttpStatusCode status, String code, String message, Throwable t) {
    super(status, asProblemDetail(status, code, message), t);
  }

  private static ProblemDetail asProblemDetail(HttpStatusCode status, String code, String message) {

    String title = AbstractWebExceptions.getExceptionByCode(code);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
    problemDetail.setTitle(title);
    problemDetail.setProperty("code", code);
    return problemDetail;
  }
}
