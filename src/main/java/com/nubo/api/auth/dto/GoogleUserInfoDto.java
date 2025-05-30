package com.nubo.api.auth.dto;

import lombok.Getter;

@Getter
public class GoogleUserInfoDto {

  private String sub;
  private String name;
  private String picture;
}