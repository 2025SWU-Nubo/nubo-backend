package com.nubo.auth.dto;

import lombok.Getter;

@Getter
public class GoogleUserInfoDto {

  private String email;
  private String sub;
  private String name;
  private String picture;
}