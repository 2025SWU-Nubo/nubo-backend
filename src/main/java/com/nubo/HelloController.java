package com.nubo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

  @GetMapping("/")
  public String home() {
    return "✨ Hello, API is running! ✨";
  }

  @GetMapping("/favicon.ico")
  public void favicon() {
  }
}