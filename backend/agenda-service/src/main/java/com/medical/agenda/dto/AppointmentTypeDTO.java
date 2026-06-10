package com.medical.agenda.dto;

import java.math.BigDecimal;

/**
 * Contrat JSON public du catalogue de types de visite (table {@code appointment_types}).
 */
public class AppointmentTypeDTO {

  private Long id;
  private String code;
  private String label;
  private String colorCode;
  private Integer defaultDurationMinutes;
  private Integer displayOrder;
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

  public Integer getDefaultDurationMinutes() {
    return defaultDurationMinutes;
  }

  public void setDefaultDurationMinutes(Integer defaultDurationMinutes) {
    this.defaultDurationMinutes = defaultDurationMinutes;
  }

  public Integer getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(Integer displayOrder) {
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
