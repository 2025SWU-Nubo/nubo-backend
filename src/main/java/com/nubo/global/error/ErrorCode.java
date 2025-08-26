package com.nubo.global.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.boot.logging.LogLevel;

/**
 * 에러 코드 enum - 애플리케이션에서 발생할 수 있는 다양한 에러를 코드와 메시지로 정의합니다. - 각 에러 코드는 상태 코드, 코드 값(도메인 별 넘버링), 메시지, 로그
 * 레벨을 포함합니다.
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ErrorCode {

  // COMMON
  INVALID_INPUT_VALUE(400, "C001", "Invalid input value", LogLevel.ERROR),
  METHOD_NOT_ALLOWED(405, "C002", "Method not allowed", LogLevel.ERROR),
  NO_HANDLER_FOUND(404, "C003", "No handler found", LogLevel.ERROR),
  NO_RESOURCE_FOUND(404, "C004", "Resource not found", LogLevel.ERROR),
  HANDLE_ACCESS_DENIED(403, "C005", "Access denied", LogLevel.ERROR),
  INTERNAL_SERVER_ERROR(500, "C006", "Internal server error", LogLevel.ERROR),
  INVALID_TYPE_VALUE(400, "C007", "Invalid Type Value", LogLevel.ERROR),
  UNAUTHORIZED_ACCESS(403, "C008", "user id mismatch", LogLevel.ERROR),
  POSITIVE_VALUE_REQUIRED(400, "C009", "Value must be positive", LogLevel.ERROR),

  FORBIDDEN(403, "C010", "Forbidden", LogLevel.WARN),

  // ENTITY
  ENTITY_NOT_FOUND(404, "E001", "Entity not found", LogLevel.WARN),
  DUPLICATE_RESOURCE(409, "E002", "Resource already exists", LogLevel.WARN),
  ILLEGAL_STATE(400, "E003", "Illegal state", LogLevel.ERROR),

  // VALIDATION
  FIELD_REQUIRED(400, "V001", "Required field is missing", LogLevel.WARN),
  INVALID_FORMAT(400, "V002", "Invalid format", LogLevel.WARN),
  FIELD_INVALID(400, "V003", "Invalid field value", LogLevel.WARN),

  /* 필요한 도메인 에러코드 추가 */

  // CARD
  DUPLICATE_CARD(409, "CD001", "card already exists", LogLevel.WARN),

  // SECURITY
  INVALID_JWT_SECRET(500, "SC001", "JWT secret key must be at least 32 characters long",
    LogLevel.ERROR),
  INVALID_JWT_TOKEN(401, "SC002", "Invalid or malformed JWT token", LogLevel.WARN),
  UNAUTHENTICATED(401, "SC003", "User not authenticated", LogLevel.WARN),
  UNAUTHORIZED_CLIENT(403, "SC004", "User not authorized", LogLevel.WARN),
  ACCESS_DENIED(403, "SC005", "Access denied for this resource", LogLevel.WARN),

  // VIDEO
  INVALID_VIDEO_ID(400, "V001", "Video ID is missing or invalid", LogLevel.WARN),
  UNSUPPORTED_PLATFORM(400, "V002", "Unsupported video platform", LogLevel.WARN);;

  private final int status;
  private final String code;
  private final String message;
  private final LogLevel logLevel;

  ErrorCode(final int status, final String code, final String message, LogLevel logLevel) {
    this.status = status;
    this.message = message;
    this.code = code;
    this.logLevel = logLevel;
  }

  public String getMessage() {
    return this.message;
  }

  public String getCode() {
    return code;
  }

  public int getStatus() {
    return status;
  }

  public LogLevel getLogLevel() {
    return logLevel;
  }
}
