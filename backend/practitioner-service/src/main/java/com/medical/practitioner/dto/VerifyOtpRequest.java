package com.medical.practitioner.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyOtpRequest {

    @NotBlank
    private String to;

    @NotBlank
    private String code;

    /** "sms" ou "email". */
    @NotBlank
    private String channel;

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
}
