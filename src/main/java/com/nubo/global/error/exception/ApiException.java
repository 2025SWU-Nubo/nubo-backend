package com.nubo.global.error.exception;


import com.nubo.global.error.ErrorCode;

public class ApiException extends RuntimeException {

  private ErrorCode errorCode;

  public ApiException(String message, ErrorCode errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  public ApiException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
