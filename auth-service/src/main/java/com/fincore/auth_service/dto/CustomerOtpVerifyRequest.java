package com.fincore.auth_service.dto;

public class CustomerOtpVerifyRequest {

    private String code;

    public CustomerOtpVerifyRequest() {}

    public String getCode() { return code; }

    public void setCode(String code) { this.code = code; }
}