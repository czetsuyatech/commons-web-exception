package com.czetsuyatech.commons.web.exceptions;

import feign.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.util.StringUtils;

@Slf4j
public class WebExceptionUtils {

  private static final String FIELD_TYPE = "type";
  private static final String FIELD_TITLE = "title";
  private static final String FIELD_DETAIL = "detail";
  private static final String FIELD_INSTANCE = "instance";
  private static final String FIELD_PROPERTIES = "properties";
  private static final String FIELD_ERRORS = "errors";

  private WebExceptionUtils() {
  }

  public static WebBaseException handleResponseException(Response response, String body, String serviceName) {
    JSONObject jsonObject = parseJson(body);
    if (log.isDebugEnabled()) {
      log.debug("Error payload: {}", jsonObject);
    }

    HttpStatus status = HttpStatus.resolve(response.status());
    if (status == null) {
      log.warn("Unsupported HTTP status: {}. Falling back to NO_HANDLER", response.status());
      return new WebBaseException(serviceName, NativeWebExceptionEnumCodes.NO_HANDLER);
    }

    switch (status) {
      case NOT_FOUND:
        return buildFromJsonOrFallback(jsonObject, HttpStatus.NOT_FOUND, serviceName,
            NativeWebExceptionEnumCodes.NO_RESOURCE_FOUND_EXCEPTION);
      case BAD_REQUEST:
        return buildFromJsonOrFallback(jsonObject, HttpStatus.BAD_REQUEST, serviceName,
            NativeWebExceptionEnumCodes.BAD_REQUEST);
      case INTERNAL_SERVER_ERROR:
        return buildFromJsonOrFallback(jsonObject, HttpStatus.INTERNAL_SERVER_ERROR, serviceName,
            NativeWebExceptionEnumCodes.INTERNAL_SERVER_ERROR);
      case METHOD_NOT_ALLOWED:
        return buildFromJsonOrFallback(jsonObject, HttpStatus.METHOD_NOT_ALLOWED, serviceName,
            NativeWebExceptionEnumCodes.METHOD_NOT_ALLOWED);
      case FORBIDDEN:
        return buildFromJsonOrFallback(jsonObject, HttpStatus.FORBIDDEN, serviceName,
            NativeWebExceptionEnumCodes.FORBIDDEN);
      default:
        log.warn("No handler found for HTTP status: {}", status);
        return new WebBaseException(serviceName, NativeWebExceptionEnumCodes.NO_HANDLER);
    }
  }

  private static WebBaseException buildFromJsonOrFallback(JSONObject jsonObject,
      HttpStatus status,
      String serviceName,
      NativeWebExceptionEnumCodes fallbackCode) {
    return (jsonObject != null)
        ? handleException(jsonObject, status)
        : new WebBaseException(serviceName, fallbackCode);
  }

  private static WebBaseException handleException(JSONObject jsonObject, HttpStatus status) {

    String type = jsonObject.optString(FIELD_TYPE);
    String title = jsonObject.optString(FIELD_TITLE);
    String detail = jsonObject.optString(FIELD_DETAIL);
    String instance = jsonObject.optString(FIELD_INSTANCE);
    URI instanceUri = createURI(instance);

    String code = jsonObject.optString(AbstractWebExceptions.CODE);
    String service = jsonObject.optString(AbstractWebExceptions.SERVICE);

    // Try extracting from "properties" if top-level is missing/blank
    JSONObject props = jsonObject.optJSONObject(FIELD_PROPERTIES);
    if (props != null) {
      if (!StringUtils.hasText(code)) {
        code = props.optString(AbstractWebExceptions.CODE);
      }
      if (!StringUtils.hasText(service)) {
        service = props.optString(AbstractWebExceptions.SERVICE);
      }
    }

    List<String> errors = new ArrayList<>();
    // errors can be either top-level or inside "properties"
    JSONArray errorsArray = jsonObject.optJSONArray(FIELD_ERRORS);
    if (errorsArray == null && props != null) {
      errorsArray = props.optJSONArray(FIELD_ERRORS);
    }
    if (errorsArray != null) {
      for (int i = 0; i < errorsArray.length(); i++) {
        String e = errorsArray.optString(i, null);
        if (StringUtils.hasText(e)) {
          errors.add(e);
        }
      }
    }

    if (log.isTraceEnabled()) {
      log.trace("Collected errors: {}", errors);
    }

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
    URI typeUri = createURI(type);
    if (typeUri != null) {
      problemDetail.setType(typeUri);
    }
    problemDetail.setTitle(title);
    problemDetail.setProperty(AbstractWebExceptions.TIMESTAMP, Instant.now());
    if (StringUtils.hasText(code)) {
      problemDetail.setProperty(AbstractWebExceptions.CODE, code);
    }
    if (instanceUri != null) {
      problemDetail.setInstance(instanceUri);
    }
    if (StringUtils.hasText(service)) {
      problemDetail.setProperty(AbstractWebExceptions.SERVICE, service);
    }
    if (!errors.isEmpty()) {
      problemDetail.setProperty(AbstractWebExceptions.ERRORS, errors);
    }

    return new WebBaseException(status, problemDetail);
  }

  public static URI createURI(String path) {
    if (!StringUtils.hasText(path)) {
      return null;
    }
    try {
      return new URI(path);
    } catch (URISyntaxException e) {
      if (log.isTraceEnabled()) {
        log.trace("Invalid URI: {}", path, e);
      } else {
        log.trace("Invalid URI: {}", path);
      }
      return null;
    }
  }

  private static JSONObject parseJson(String json) {
    if (!StringUtils.hasText(json)) {
      return null;
    }
    try {
      return new JSONObject(json);
    } catch (JSONException e) {
      log.warn("Could not parse JSON body");
      if (log.isTraceEnabled()) {
        log.trace("JSON parsing error for body: {}", json, e);
      }
      return null;
    }
  }
}
