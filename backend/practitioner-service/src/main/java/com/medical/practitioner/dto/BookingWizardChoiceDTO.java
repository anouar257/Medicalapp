package com.medical.practitioner.dto;

import com.medical.practitioner.entity.SpecialtyBookingStep;

public class BookingWizardChoiceDTO {

  private SpecialtyBookingStep step;
  private String code;
  private String labelFr;
  private int sortOrder;

  public SpecialtyBookingStep getStep() {
    return step;
  }

  public void setStep(SpecialtyBookingStep step) {
    this.step = step;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getLabelFr() {
    return labelFr;
  }

  public void setLabelFr(String labelFr) {
    this.labelFr = labelFr;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }
}
