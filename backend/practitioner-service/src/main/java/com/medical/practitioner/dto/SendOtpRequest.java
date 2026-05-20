package com.medical.practitioner.dto;

import jakarta.validation.constraints.NotBlank;

public class SendOtpRequest {

    @NotBlank
    private String to;

    /** "sms" ou "email". */
    @NotBlank
    private String channel;

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
}
