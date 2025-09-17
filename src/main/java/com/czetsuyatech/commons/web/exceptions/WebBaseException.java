package com.czetsuyatech.commons.web.exceptions;

import java.util.List;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.util.StringUtils;
import org.springframework.web.ErrorResponseException;

@Getter
@ToString
public class WebBaseException extends ErrorResponseException {

  public WebBaseException(NativeWebExceptionEnumCodes exceptionCode) {
    this(null, exceptionCode.getHttpStatus(), exceptionCode.getErrorCode());
  }

  public WebBaseException(HttpStatusCode status, String code) {
    this(null, status, code, AbstractWebExceptions.getExceptionByCode(code));
  }

  public WebBaseException(String serviceName, NativeWebExceptionEnumCodes exceptionCode) {
    this(serviceName, exceptionCode.getHttpStatus(), exceptionCode.getErrorCode(), exceptionCode.getMessage());
  }

  public WebBaseException(String serviceName, HttpStatusCode status, String code) {
    this(serviceName, status, code, AbstractWebExceptions.getExceptionByCode(code));
  }

  public WebBaseException(HttpStatusCode status, String code, String message) {
    this(null, status, code, message);
  }

  public WebBaseException(String serviceName, HttpStatusCode status, String code, String message) {
    super(status, asProblemDetail(serviceName, status, code, message, null), null);
  }

  public WebBaseException(String serviceName, HttpStatusCode status, String code, String message, List<String> errors) {
    super(status, asProblemDetail(serviceName, status, code, message, errors), null);
  }

  public WebBaseException(HttpStatusCode status, ProblemDetail problemDetail) {
    super(status, problemDetail, null);
  }

  private static ProblemDetail asProblemDetail(HttpStatusCode status, String code, String message) {
    return asProblemDetail(null, status, code, message, null);
  }

  private static ProblemDetail asProblemDetail(String serviceName, HttpStatusCode status, String code, String message,
      List<String> errors) {

    String title = AbstractWebExceptions.getExceptionByCode(code);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, message);
    problemDetail.setTitle(title);
    problemDetail.setProperty(AbstractWebExceptions.CODE, code);
    if (StringUtils.hasText(serviceName)) {
      problemDetail.setProperty(AbstractWebExceptions.SERVICE, serviceName);
    }
    if (errors != null && !errors.isEmpty()) {
      problemDetail.setProperty(AbstractWebExceptions.ERRORS, errors);
    }
    return problemDetail;
  }

  @Override
  public String getMessage() {
    return getBody().getDetail();
  }
}
