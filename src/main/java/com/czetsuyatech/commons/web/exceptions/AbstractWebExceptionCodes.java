package com.czetsuyatech.commons.web.exceptions;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.ErrorResponseException;

/**
 * Manage the error codes of the application. The following prefixes are used.
 * <pre>
 *   'A' - application error
 *   'B' - business error
 *   'S' - system error
 *   'E' - entity error
 * </pre>
 */
@Slf4j
public abstract class AbstractWebExceptionCodes extends ErrorResponseException {

  public static final String PARAMS = "params";
  public static final String ERRORS = "errors";
  public static final String CODE = "code";
  public static final String TIMESTAMP = "timestamp";
  public static final String SERVICE = "service";
  public static final String THROWABLE = "S100";
  public static final String RUNTIME = "S101";
  public static final String INTERNAL_SERVER_ERROR = "S102";
  public static final String BAD_REQUEST = "S103";
  public static final String INVALID_FORMAT = "S104";
  public static final String METHOD_NOT_ALLOWED = "S105";
  public static final String FORBIDDEN = "S106";
  public static final String METHOD_ARGUMENT_NOT_VALID = "S107";

  public static final String METHOD_ARGUMENT_NOT_VALID_MESSAGE = "Validation failed for fields {0}";
  public static final String INVALID_FORMAT_MESSAGE = "Invalid format for (field: {0}, value: {1}, type: {2})";

  protected static Map<String, String> codeErrorMessages = getCommonErrorMessages();

  public AbstractWebExceptionCodes(HttpStatusCode status) {
    super(status);
  }

  private static Map<String, String> getCommonErrorMessages() {

    final Map<String, String> map = new LinkedHashMap<>();

    map.put(THROWABLE, THROWABLE);
    map.put(RUNTIME, RUNTIME);
    map.put(INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR);
    map.put(BAD_REQUEST, BAD_REQUEST);
    map.put(INVALID_FORMAT, INVALID_FORMAT);
    map.put(METHOD_NOT_ALLOWED, METHOD_NOT_ALLOWED);
    map.put(METHOD_ARGUMENT_NOT_VALID, METHOD_ARGUMENT_NOT_VALID);
    map.put(FORBIDDEN, FORBIDDEN);

    return Collections.unmodifiableMap(map);
  }

  @SafeVarargs
  public final void registerErrorMap(Map<String, String>... exceptionMap) {

    registerErrorMap(Stream.of(exceptionMap).collect(Collectors.toSet()));
  }

  public static String getMessageByCode(String exceptionCode) {

    return (Objects.isNull(codeErrorMessages))
        ? "Exception codes are not initialized!"
        : codeErrorMessages.getOrDefault(exceptionCode, "Exception code=" + exceptionCode + " is not available.");
  }

  public synchronized void registerErrorMap(Set<Map<String, String>> errorsMapSet) {

    Objects.isNull(errorsMapSet);
    if (errorsMapSet == null || errorsMapSet.isEmpty()) {
      return;
    }

    final Map<String, String> newMap = new LinkedHashMap<>(codeErrorMessages);

    errorsMapSet.forEach(
        errorsMap -> {
          if (!errorsMap.isEmpty()) {
            errorsMap.keySet().forEach(
                errCode -> {
                  if (newMap.get(errCode) != null) {
                    log.warn("Overriding error with code={}, oldValue={}, newValue={}", errCode, newMap.get(errCode), errorsMap.get(errCode)
                    );
                  }
                  newMap.put(errCode, errorsMap.get(errCode));
                }
            );
          }
        }
    );

    codeErrorMessages = Collections.unmodifiableMap(newMap);
  }

  public abstract String getServiceName();
}
