package com.example.authdemo.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

        @GetMapping("/auth/github")
        public String loginWithGitHub() {
                return "redirect:/oauth2/authorization/github";
        }
}
