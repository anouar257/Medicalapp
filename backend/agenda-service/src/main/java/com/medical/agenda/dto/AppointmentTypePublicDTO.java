package com.medical.agenda.dto;

public class AppointmentTypePublicDTO {

  private Long id;
  private String code;
  private String label;
  private String colorCode;
  private int defaultDurationMinutes;
  private int displayOrder;
  private boolean active;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getColorCode() {
    return colorCode;
  }

  public void setColorCode(String colorCode) {
    this.colorCode = colorCode;
  }

  public int getDefaultDurationMinutes() {
    return defaultDurationMinutes;
  }

  public void setDefaultDurationMinutes(int defaultDurationMinutes) {
    this.defaultDurationMinutes = defaultDurationMinutes;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
