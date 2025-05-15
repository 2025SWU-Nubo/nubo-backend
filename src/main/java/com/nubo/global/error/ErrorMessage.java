package com.nubo.global.error;

/**
 * 에러 메시지 클래스
 * - 에러 응답을 구성하는데 필요한 에러 코드, 메시지, 그리고 추가 데이터를 포함합니다.
 */
public class ErrorMessage {

  private final String code;

  private final String message;

  private final Object data;

  public ErrorMessage(ErrorCode errorCode) {
    this.code = errorCode.getCode();
    this.message = errorCode.getMessage();
    this.data = null;
  }

  public ErrorMessage(ErrorCode errorCode, Object data) {
    this.code = errorCode.getCode();
    this.message = errorCode.getMessage();
    this.data = data;
  }

  public String getMessage() {
    return message;
  }

  public String getCode() {
    return code;
  }

  public Object getData() {
    return data;
  }
}
