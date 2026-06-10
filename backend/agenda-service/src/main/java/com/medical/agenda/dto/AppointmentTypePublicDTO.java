package com.medical.agenda.dto;

import java.math.BigDecimal;

public class AppointmentTypePublicDTO {

  private Long id;
  private String code;
  private String label;
  private String colorCode;
  private int defaultDurationMinutes;
  private int displayOrder;
  private boolean active;
  private BigDecimal price;
  private boolean priceVariable;
  private Long sourcePractitionerId;
  private Long sourceActId;

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

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public boolean isPriceVariable() {
    return priceVariable;
  }

  public void setPriceVariable(boolean priceVariable) {
    this.priceVariable = priceVariable;
  }

  public Long getSourcePractitionerId() {
    return sourcePractitionerId;
  }

  public void setSourcePractitionerId(Long sourcePractitionerId) {
    this.sourcePractitionerId = sourcePractitionerId;
  }

  public Long getSourceActId() {
    return sourceActId;
  }

  public void setSourceActId(Long sourceActId) {
    this.sourceActId = sourceActId;
  }
}
