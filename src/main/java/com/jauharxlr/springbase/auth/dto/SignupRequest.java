package com.jauharxlr.springbase.auth.dto;

import lombok.Data;

@Data
public class SignupRequest {
    private String email;
    private String password;
    private String projectRef;
}
