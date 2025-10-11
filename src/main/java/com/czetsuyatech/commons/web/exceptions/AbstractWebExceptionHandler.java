package com.czetsuyatech.commons.web.exceptions;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
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
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tools.jackson.databind.exc.InvalidFormatException;

@Slf4j
public abstract class AbstractWebExceptionHandler extends ResponseEntityExceptionHandler {

  private static final String RAW_EXCEPTION = "Raw Exception: ";
  private static final String SEPARATOR = ", ";

  @ExceptionHandler({WebBaseException.class})
  public final WebBaseException handleWebBaseException(WebBaseException ex, @Nonnull WebRequest req) {

    decorateWebBaseException(ex, req);

    return ex;
  }

  @ExceptionHandler({InvalidFormatException.class})
  public final ResponseEntity<ProblemDetail> handleInvalidFormatException(InvalidFormatException ex,
      @Nonnull WebRequest request) {

    logRawException(ex);

    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    handleInvalidFormatException(ex, problemDetail, request);

    return ResponseEntity.of(problemDetail).build();
  }

  @ExceptionHandler({ConstraintViolationException.class})
  public final ResponseEntity<ProblemDetail> handleConstraintViolationException(ConstraintViolationException ex,
      @Nonnull WebRequest request) {

    logRawException(ex);

    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    handleConstraintViolationException(ex, problemDetail, request);

    return ResponseEntity.of(problemDetail).build();
  }

  @ExceptionHandler(ConcurrencyFailureException.class)
  public ResponseEntity<ProblemDetail> handleConcurrencyFailure(@Nonnull ConcurrencyFailureException ex,
      @Nonnull NativeWebRequest request) {

    logRawException(ex);

    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);

    decorateProblemDetail(problemDetail, NativeWebExceptionEnumCodes.CONCURRENCY_VIOLATION.getErrorCode(), request);

    return ResponseEntity.of(problemDetail).build();
  }

  @Override
  protected final ResponseEntity<Object> handleHttpMessageNotReadable(@Nonnull HttpMessageNotReadableException ex,
      @Nonnull HttpHeaders headers, @Nonnull HttpStatusCode status,
      @Nonnull WebRequest request) {

    ResponseEntity<Object> result = super.handleHttpMessageNotReadable(ex, headers, status, request);

    if (Objects.requireNonNull(result).getBody() instanceof ProblemDetail problemDetail) {
      if (ex.getCause() instanceof InvalidFormatException invalidFormatException) {
        handleInvalidFormatException(invalidFormatException, problemDetail, request);
      }
    }

    return result;
  }

  @Override
  protected final ResponseEntity<Object> handleMethodArgumentNotValid(@Nonnull MethodArgumentNotValidException ex,
      @Nonnull HttpHeaders headers, @Nonnull HttpStatusCode status,
      @Nonnull WebRequest request) {

    ResponseEntity<Object> result = super.handleMethodArgumentNotValid(ex, headers, status, request);

    if (Objects.requireNonNull(result).getBody() instanceof ProblemDetail problemDetail) {

      BindingResult bindingResult = ex.getBindingResult();

      List<String> params = bindingResult.getFieldErrors().stream()
          .map(fe -> String.format("object:%s, field:%s, message:%s", fe.getObjectName(), fe.getField(), fe.getCode()))
          .collect(Collectors.toList());

      String formattedMessage = params.stream().map(p -> String.format("(%s)", p)).collect(Collectors.joining(";"));
      String parameterizedMessage = MessageFormat.format(
          NativeWebExceptionEnumCodes.METHOD_ARGUMENT_NOT_VALID.getMessage(),
          formattedMessage);

      decorateProblemDetail(problemDetail, NativeWebExceptionEnumCodes.METHOD_ARGUMENT_NOT_VALID.getErrorCode(),
          parameterizedMessage,
          params, request);
    }

    return result;
  }

  @Override
  protected final ResponseEntity<Object> handleExceptionInternal(@Nonnull Exception ex, Object body,
      @Nonnull HttpHeaders headers,
      @Nonnull HttpStatusCode statusCode,
      @Nonnull WebRequest request) {

    logRawException(ex);

    ResponseEntity<Object> result = super.handleExceptionInternal(ex, body, headers, statusCode, request);

    if (Objects.requireNonNull(result).getBody() instanceof ProblemDetail problemDetail) {
      if (ex instanceof ErrorResponse errorResponse) {
        if (Objects.isNull(problemDetail.getProperties())
            || problemDetail.getProperties().isEmpty()
            || Objects.isNull(problemDetail.getProperties().get(AbstractWebExceptions.CODE))) {

          // get from mapping
          NativeWebExceptionEnumCodes exception = NativeWebExceptionEnumCodes.getByClassName(
              camelToUnderlinedName(errorResponse.getClass().getSimpleName()));
          decorateProblemDetail(problemDetail, exception.getErrorCode(), request);
        }
      }
    }

    return result;
  }

  @Override
  protected final @Nonnull ProblemDetail createProblemDetail(@Nonnull Exception ex, @Nonnull HttpStatusCode status,
      @Nonnull String defaultDetail, String detailMessageCode, Object[] detailMessageArguments,
      @Nonnull WebRequest request) {

    ProblemDetail problemDetail = super.createProblemDetail(ex, status, defaultDetail, detailMessageCode,
        detailMessageArguments, request);
    decorateProblemDetail(problemDetail, request);

    return problemDetail;
  }

  protected void decorateWebBaseException(WebBaseException ex, WebRequest req) {

    decorateProblemDetail(ex.getBody(), req);
  }

  @SuppressWarnings({"unchecked"})
  protected void decorateProblemDetail(ProblemDetail problemDetail, WebRequest req) {

    String errCode = "";
    if (!Objects.isNull(problemDetail.getProperties()) && !problemDetail.getProperties().isEmpty()) {
      errCode = String.valueOf(Optional.ofNullable(problemDetail.getProperties()).orElse(Collections.EMPTY_MAP)
          .getOrDefault(AbstractWebExceptions.CODE, "UNKNOWN"));
    }

    decorateProblemDetail(problemDetail, errCode, req);
  }

  protected void decorateProblemDetail(ProblemDetail problemDetail, String errCode, WebRequest req) {
    decorateProblemDetail(problemDetail, errCode, null, req);
  }

  @SuppressWarnings({"unchecked"})
  protected void decorateProblemDetail(ProblemDetail problemDetail, String errCode, String detail, WebRequest req) {

    decorateProblemDetail(problemDetail, errCode, detail, Collections.EMPTY_LIST, req);
  }

  protected void decorateProblemDetail(ProblemDetail problemDetail, String errCode, String detail,
      List<String> formattedMessage, WebRequest req) {

    String baseUrl = getBaseUrl(req);

    problemDetail.setProperty(AbstractWebExceptions.CODE, errCode);
    if (!Objects.isNull(detail)) {
      problemDetail.setDetail(detail);
    }
    problemDetail.setProperty(AbstractWebExceptions.SERVICE, getServiceName());
    problemDetail.setType(URI.create(String.format(baseUrl + "/errors/%s", errCode)));
    problemDetail.setProperty(AbstractWebExceptions.TIMESTAMP, Instant.now());
    problemDetail.setProperty(AbstractWebExceptions.ERRORS, formattedMessage);
  }

  public void handleInvalidFormatException(InvalidFormatException ex, ProblemDetail problemDetail,
      @Nonnull WebRequest request) {

    logRawException(ex);

    String targetType =
        Objects.isNull(ex.getTargetType()) ? ex.getClass().getSimpleName() : ex.getTargetType().getSimpleName();
    Object value = ex.getValue();
    String fieldName = (CollectionUtils.isEmpty(ex.getPath()) ? "unknown" : ex.getPath().get(0).getPropertyName());

    String messageNotParametrized = NativeWebExceptionEnumCodes.INVALID_FORMAT.getMessage();

    List<String> formattedMessage = Collections.singletonList(
        String.format("field:%s, value:%s, type:%s", fieldName, value, targetType));
    String parameterizedMessage = MessageFormat.format(messageNotParametrized, fieldName, value, targetType);

    decorateProblemDetail(problemDetail, NativeWebExceptionEnumCodes.INVALID_FORMAT.getErrorCode(),
        parameterizedMessage,
        formattedMessage, request);
  }

  private void handleConstraintViolationException(ConstraintViolationException ex, ProblemDetail problemDetail,
      WebRequest request) {

    logRawException(ex);

    decorateProblemDetail(problemDetail, NativeWebExceptionEnumCodes.CONSTRAINT_VIOLATION.getErrorCode(),
        ex.getMessage(), request);
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

  @SuppressWarnings({"unused"})
  protected HttpServletResponse getServletResponse(WebRequest req) {
    return ((ServletWebRequest) req).getResponse();
  }

  public abstract String getServiceName();

  private void logRawException(Exception ex) {
    log.error(RAW_EXCEPTION + "{}", extractClassNamesFromException(ex), ex);
  }

  private String extractClassNamesFromException(Throwable ex) {

    StringBuilder result = new StringBuilder();

    if (ex.getCause() != null) {
      result.append(SEPARATOR)
          .append(ex.getCause().getClass().getSimpleName());
    }

    result.append(SEPARATOR);
    result.append(ex.getClass().getSimpleName());

    return result.substring(1);
  }

  public static String camelToUnderlinedName(String name) {

    StringBuilder sb = new StringBuilder();
    boolean lastWasUnderline = false;
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      boolean isCaps = Character.isUpperCase(c);
      if (isCaps) {
        if (i > 0 && !lastWasUnderline) {
          sb.append('_');
        }
        c = Character.toLowerCase(c);
      }
      sb.append(c);

      lastWasUnderline = (c == '_');
    }
    return sb.toString();
  }
}
