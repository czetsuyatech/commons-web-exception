package com.czetsuyatech.commons.web.exceptions;

import java.util.Arrays;
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

  public static final String SERVICE = "service";
  public static final String CODE = "code";
  public static final String TIMESTAMP = "timestamp";
  public static final String ERRORS = "errors";

  protected static Map<String, String> exceptionCodes;

  public AbstractWebExceptionCodes(HttpStatusCode status) {
    super(status);
  }

  static {
    final Map<String, String> map = new LinkedHashMap<>();

    Arrays.stream(NativeWebExceptionEnumCodes.values())
        .forEach(e -> map.put(e.getErrorCode(), e.name()));

    exceptionCodes = Collections.unmodifiableMap(map);
  }

  /**
   * Register the service specific exceptions.
   *
   * @param exceptionMap list of new exceptions
   */
  @SafeVarargs
  public final void registerExceptionMap(Map<String, String>... exceptionMap) {

    registerExceptionMap(Stream.of(exceptionMap).collect(Collectors.toSet()));
  }

  public static String getExceptionByCode(String exceptionCode) {

    return (Objects.isNull(exceptionCodes))
        ? "Exception codes are not initialized!"
        : exceptionCodes.getOrDefault(exceptionCode, "Exception code=" + exceptionCode + " is not available.");
  }

  public synchronized void registerExceptionMap(Set<Map<String, String>> errorsMapSet) {

    if (errorsMapSet == null || errorsMapSet.isEmpty()) {
      return;
    }

    final Map<String, String> newMap = new LinkedHashMap<>(exceptionCodes);

    errorsMapSet.forEach(
        errorsMap -> {
          if (!errorsMap.isEmpty()) {
            errorsMap.keySet().forEach(
                errCode -> {
                  if (newMap.get(errCode) != null) {
                    log.warn("Overriding error with code={}, oldValue={}, newValue={}", errCode, newMap.get(errCode),
                        errorsMap.get(errCode)
                    );
                  }
                  newMap.put(errCode, errorsMap.get(errCode));
                }
            );
          }
        }
    );

    exceptionCodes = Collections.unmodifiableMap(newMap);
  }

  public abstract String getServiceName();
}
