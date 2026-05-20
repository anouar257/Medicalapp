package com.medical.practitioner.dto;

import java.util.ArrayList;
import java.util.List;

/** Options de questionnaire pour le parcours « Nouveau rendez-vous » (spécialité résolue en base). */
public class BookingWizardOptionsDTO {

  private String requestedSpecialtyCode;
  private String resolvedSpecialtyCode;
  private List<BookingWizardChoiceDTO> choicesPrior = new ArrayList<>();
  private List<BookingWizardChoiceDTO> choicesVisit = new ArrayList<>();

  public String getRequestedSpecialtyCode() {
    return requestedSpecialtyCode;
  }

  public void setRequestedSpecialtyCode(String requestedSpecialtyCode) {
    this.requestedSpecialtyCode = requestedSpecialtyCode;
  }

  public String getResolvedSpecialtyCode() {
    return resolvedSpecialtyCode;
  }

  public void setResolvedSpecialtyCode(String resolvedSpecialtyCode) {
    this.resolvedSpecialtyCode = resolvedSpecialtyCode;
  }

  public List<BookingWizardChoiceDTO> getChoicesPrior() {
    return choicesPrior;
  }

  public void setChoicesPrior(List<BookingWizardChoiceDTO> choicesPrior) {
    this.choicesPrior = choicesPrior;
  }

  public List<BookingWizardChoiceDTO> getChoicesVisit() {
    return choicesVisit;
  }

  public void setChoicesVisit(List<BookingWizardChoiceDTO> choicesVisit) {
    this.choicesVisit = choicesVisit;
  }
}
