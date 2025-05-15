package com.nubo.global.error;

import com.nubo.global.error.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 전역 예외 처리 핸들러
 * - 애플리케이션의 모든 컨트롤러에서 발생하는 예외를 처리합니다.
 * - 각 메서드는 특정 예외 타입을 처리하고, 적절한 HTTP 응답을 반환합니다.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * * @RequestBody, @RequestPart 등의 요청 본문 바인딩에서 발생한 유효성 검사 실패를 처리합니다.
   *
   * @param e MethodArgumentNotValidException 예외 객체
   * @return 잘못된 요청 입력에 대한 에러 응답 (HTTP 400 Bad Request)
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
    MethodArgumentNotValidException e) {
    log.error("handleMethodArgumentNotValidException", e);
    final ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE,
      e.getBindingResult());
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  /**
   * * @ModelAttribute 바인딩에서 발생한 오류를 처리합니다.
   *
   * @param e BindException 예외 객체
   * @return 잘못된 요청 입력에 대한 에러 응답 (HTTP 400 Bad Request)
   */
  @ExceptionHandler(BindException.class)
  protected ResponseEntity<ErrorResponse> handleBindException(BindException e) {
    log.error("handleBindException", e);
    final ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE,
      e.getBindingResult());
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  /**
   * 요청 파라미터의 타입이 맞지 않아 바인딩에 실패한 경우를 처리합니다.
   *
   * @param e MethodArgumentTypeMismatchException 예외 객체
   * @return 잘못된 요청 입력에 대한 에러 응답 (HTTP 400 Bad Request)
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  protected ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
    MethodArgumentTypeMismatchException e) {
    log.error("handleMethodArgumentTypeMismatchException", e);
    final ErrorResponse response = ErrorResponse.of(e);
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  /**
   * 지원하지 않는 HTTP 메서드가 호출된 경우를 처리합니다.
   *
   * @param e HttpRequestMethodNotSupportedException 예외 객체
   * @return 지원하지 않는 메서드에 대한 에러 응답 (HTTP 405 Method Not Allowed)
   */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  protected ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
    HttpRequestMethodNotSupportedException e) {
    log.error("handleHttpRequestMethodNotSupportedException", e);
    final ErrorResponse response = ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED);
    return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
  }

  /**
   * 존재하지 않는 URL이 요청된 경우를 처리합니다.
   *
   * @param e NoHandlerFoundException 예외 객체
   * @return 존재하지 않는 URL에 대한 에러 응답 (HTTP 404 Not Found)
   */
  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException e) {
    log.error("handleNoHandlerFoundException", e);
    final ErrorResponse response = ErrorResponse.of(ErrorCode.NO_HANDLER_FOUND);
    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
  }

  /**
   * 요청한 리소스가 존재하지 않는 경우를 처리합니다.
   *
   * @param e NoResourceFoundException 예외 객체
   * @return 존재하지 않는 리소스에 대한 에러 응답 (HTTP 404 Not Found)
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException e) {
    log.error("handleNoResourceFoundException", e);
    final ErrorResponse response = ErrorResponse.of(ErrorCode.NO_RESOURCE_FOUND);
    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
  }

  /**
   * 권한이 부족하여 접근이 거부된 경우를 처리합니다.
   *
   * @param e AccessDeniedException 예외 객체
   * @return 권한 거부에 대한 에러 응답 (HTTP 403 Forbidden)
   */
  @ExceptionHandler(AccessDeniedException.class)
  protected ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
    log.error("handleAccessDeniedException", e);
    final ErrorResponse response = ErrorResponse.of(ErrorCode.HANDLE_ACCESS_DENIED);
    return new ResponseEntity<>(response,
      HttpStatus.valueOf(ErrorCode.HANDLE_ACCESS_DENIED.getStatus()));
  }

  /**
   * 도메인별 비즈니스 로직에서 발생한 커스텀 예외를 처리합니다.
   *
   * @param e ApiException 예외 객체
   * @return 비즈니스 로직에서 발생한 에러에 대한 응답
   */
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
    final ErrorCode errorCode = e.getErrorCode();

    // 로깅 수준에 따라 로그를 기록합니다.
    switch (errorCode.getLogLevel()) {
      case ERROR -> log.error(errorCode.getMessage());
      case WARN -> log.warn(errorCode.getMessage());
      default -> log.info(errorCode.getMessage());
    }
    final ErrorResponse response = ErrorResponse.of(errorCode);
    return new ResponseEntity<>(response, HttpStatus.valueOf(errorCode.getStatus()));
  }

  /**
   * 위에서 처리되지 않은 모든 예외를 처리합니다.
   *
   * @param e Exception 예외 객체
   * @return 서버 내부 오류에 대한 에러 응답 (HTTP 500 Internal Server Error)
   */
  @ExceptionHandler(Exception.class)
  protected ResponseEntity<ErrorResponse> handleException(Exception e) {
    log.error("ServerInternalException", e);
    final ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
