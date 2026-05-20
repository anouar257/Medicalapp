package com.medical.practitioner.dto;

public class CheckExistenceRequest {
    private String email;
    private String telephone;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
}
