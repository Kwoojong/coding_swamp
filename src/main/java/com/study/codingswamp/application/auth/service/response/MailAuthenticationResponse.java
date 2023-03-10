package com.study.codingswamp.application.auth.service.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MailAuthenticationResponse {

    private String email;
    private String authCode;
}
