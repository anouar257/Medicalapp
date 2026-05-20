package com.medical.practitioner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Réponse possible à une étape du questionnaire RDV, rattachée à une spécialité du référentiel.
 *
 * <p>Les libellés des questions (« déjà consulté », « motif ») sont constants côté frontend i18n ;
 * seules les <em>options</em> de réponse varient selon la spécialité (ex. dentiste vs audioprothésiste).
 */
@Entity
@Table(name = "specialty_booking_choices")
public class SpecialtyBookingChoice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "specialty_id", nullable = false)
  private Specialty specialty;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private SpecialtyBookingStep step;

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "label_fr", nullable = false, length = 512)
  private String labelFr;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder = 0;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Specialty getSpecialty() {
    return specialty;
  }

  public void setSpecialty(Specialty specialty) {
    this.specialty = specialty;
  }

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
