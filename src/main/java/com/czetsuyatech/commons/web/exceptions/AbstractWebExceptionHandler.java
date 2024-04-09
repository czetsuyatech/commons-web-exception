package com.czetsuyatech.commons.web.exceptions;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Slf4j
public abstract class AbstractWebExceptionHandler extends ResponseEntityExceptionHandler {

  private static final String RAW_EXCEPTION = "Raw Exception: ";
  private static final String SEPARATOR = ", ";

  @ExceptionHandler({WebBaseException.class})
  public final WebBaseException handleWebBaseException(WebBaseException ex, @Nonnull WebRequest req) {

    decorateWebBaseException(ex, req);

    return ex;
  }

  @ExceptionHandler({RuntimeException.class})
  public final ResponseEntity<ProblemDetail> handleRuntimeException(RuntimeException ex, @Nonnull WebRequest req) {

    logRawException(ex);

    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

    decorateProblemDetail(problemDetail, AbstractWebExceptionCodes.RUNTIME, ex.getMessage(), req);

    return ResponseEntity.of(problemDetail).build();
  }

  @ExceptionHandler({InvalidFormatException.class})
  public final ResponseEntity<ProblemDetail> handleInvalidFormatException(InvalidFormatException ex,
      @Nonnull WebRequest request) {

    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    handleInvalidFormatException(ex, problemDetail, request);

    return ResponseEntity.of(problemDetail).build();
  }

  @Override
  protected final ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
      HttpHeaders headers, HttpStatusCode status,
      @Nonnull WebRequest request) {

    ResponseEntity<Object> result = super.handleHttpMessageNotReadable(ex, headers, status, request);

    if (result.getBody() instanceof ProblemDetail problemDetail) {
      if (ex.getCause() instanceof InvalidFormatException invalidFormatException) {
        handleInvalidFormatException(invalidFormatException, problemDetail, request);
      }
    }

    return result;
  }

  @Override
  protected final ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
      HttpHeaders headers, HttpStatusCode status,
      @Nonnull WebRequest request) {

    ResponseEntity<Object> result = super.handleMethodArgumentNotValid(ex, headers, status, request);

    if (result.getBody() instanceof ProblemDetail problemDetail) {

      BindingResult bindingResult = ex.getBindingResult();

      List<String> params = bindingResult.getFieldErrors().stream()
          .map(fe -> String.format("object:%s, field:%s, message:%s", fe.getObjectName(), fe.getField(), fe.getCode()))
          .collect(Collectors.toList());

      String formattedMessage = params.stream().map(p -> String.format("(%s)", p)).collect(Collectors.joining(";"));
      String parameterizedMessage = MessageFormat.format(AbstractWebExceptionCodes.METHOD_ARGUMENT_NOT_VALID_MESSAGE,
          formattedMessage);

      decorateProblemDetail(problemDetail, AbstractWebExceptionCodes.METHOD_ARGUMENT_NOT_VALID, parameterizedMessage,
          params, request);
    }

    return result;
  }

  @Override
  protected final ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
      HttpStatusCode statusCode,
      @Nonnull WebRequest request) {

    logRawException(ex);

    ResponseEntity<Object> result = super.handleExceptionInternal(ex, body, headers, statusCode, request);

    if (result.getBody() instanceof ProblemDetail problemDetail) {
      if (ex instanceof ErrorResponse errorResponse) {
        if (Objects.isNull(problemDetail.getProperties())
            || problemDetail.getProperties().isEmpty()
            || Objects.isNull(problemDetail.getProperties().get(AbstractWebExceptionCodes.CODE))) {
          decorateProblemDetail(problemDetail, errorResponse.getClass().getSimpleName().toUpperCase(), request);
        }
      }
    }

    return result;
  }

  @Override
  protected final ProblemDetail createProblemDetail(Exception ex, HttpStatusCode status, String defaultDetail,
      String detailMessageCode,
      Object[] detailMessageArguments, @Nonnull WebRequest request) {

    ProblemDetail problemDetail = super.createProblemDetail(ex, status, defaultDetail, detailMessageCode,
        detailMessageArguments, request);
    decorateProblemDetail(problemDetail, request);

    return problemDetail;
  }

  protected void decorateWebBaseException(WebBaseException ex, WebRequest req) {

    decorateProblemDetail(ex.getBody(), req);
  }

  protected void decorateProblemDetail(ProblemDetail problemDetail, WebRequest req) {

    String errCode = "";
    if (!Objects.isNull(problemDetail.getProperties()) && !problemDetail.getProperties().isEmpty()) {
      errCode = String.valueOf(Optional.ofNullable(problemDetail.getProperties()).get()
          .getOrDefault(AbstractWebExceptionCodes.CODE, "BLANK"));
    }

    decorateProblemDetail(problemDetail, errCode, req);
  }

  protected void decorateProblemDetail(ProblemDetail problemDetail, String errCode, WebRequest req) {
    decorateProblemDetail(problemDetail, errCode, null, req);
  }

  protected void decorateProblemDetail(ProblemDetail problemDetail, String errCode, String detail, WebRequest req) {

    decorateProblemDetail(problemDetail, errCode, detail, Collections.EMPTY_LIST, req);
  }

  protected void decorateProblemDetail(ProblemDetail problemDetail, String errCode, String detail,
      List<String> formattedMessage, WebRequest req) {

    String baseUrl = getBaseUrl(req);

    problemDetail.setProperty(AbstractWebExceptionCodes.CODE, errCode);
    if (!Objects.isNull(detail)) {
      problemDetail.setDetail(detail);
    }
    problemDetail.setProperty(AbstractWebExceptionCodes.SERVICE, getServiceName());
    problemDetail.setType(URI.create(String.format(baseUrl + "/errors/%s", errCode)));
    problemDetail.setProperty(AbstractWebExceptionCodes.TIMESTAMP, Instant.now());
    problemDetail.setProperty(AbstractWebExceptionCodes.ERRORS, formattedMessage);
  }

  public void handleInvalidFormatException(InvalidFormatException ex, ProblemDetail problemDetail,
      @Nonnull WebRequest request) {

    logRawException(ex);

    String targetType =
        Objects.isNull(ex.getTargetType()) ? ex.getClass().getSimpleName() : ex.getTargetType().getSimpleName();
    Object value = ex.getValue();
    String fieldName = (CollectionUtils.isEmpty(ex.getPath()) ? "unknown" : ex.getPath().get(0).getFieldName());

    String messageNotParametrized = AbstractWebExceptionCodes.INVALID_FORMAT_MESSAGE;

    List<String> formattedMessage = Arrays.asList(
        String.format("field:%s, value:%s, type:%s", fieldName, value, targetType));
    String parameterizedMessage = MessageFormat.format(messageNotParametrized, fieldName, value, targetType);

    decorateProblemDetail(problemDetail, AbstractWebExceptionCodes.INVALID_FORMAT, parameterizedMessage,
        formattedMessage, request);
  }

  private String getBaseUrl(WebRequest req) {

    return ServletUriComponentsBuilder.fromRequestUri(getServletRequest(req))
        .replacePath(null)
        .build()
        .toUriString();
  }

  protected HttpServletRequest getServletRequest(WebRequest req) {
    return ((ServletWebRequest) req).getRequest();
  }

  protected HttpServletResponse getServletResponse(WebRequest req) {
    return ((ServletWebRequest) req).getResponse();
  }

  public abstract String getServiceName();

  private void logRawException(Exception ex) {
    log.error(RAW_EXCEPTION + extractClassNamesFromException(ex), ex);
  }

  private String extractClassNamesFromException(Throwable ex) {

    StringBuilder result = new StringBuilder("");

    if (ex.getCause() != null) {
      result = result.append(SEPARATOR + ex.getCause().getClass().getSimpleName());
    }

    result = result.append(SEPARATOR + ex.getClass().getSimpleName());

    return result.substring(1);
  }
}