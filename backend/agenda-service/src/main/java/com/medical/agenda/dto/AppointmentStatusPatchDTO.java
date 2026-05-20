package com.medical.agenda.dto;

import com.medical.agenda.entity.AppointmentStatus;

public class AppointmentStatusPatchDTO {

  private AppointmentStatus status;

  public AppointmentStatus getStatus() {
    return status;
  }

  public void setStatus(AppointmentStatus status) {
    this.status = status;
  }
}
