package com.medical.agenda.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

/**
 * Type de visite — catalogue dynamique stocké en base ({@code appointment_types}).
 *
 * <p>Le {@code code} reste le repère stable pour les filtres frontend (ex. {@code CONSULTATION},
 * {@code CONTROL}), tandis que {@code label}, {@code colorCode} et {@code defaultDurationMinutes}
 * sont librement éditables depuis l’admin sans migration.
 */
@Entity
@Table(
    name = "appointment_types",
    uniqueConstraints = {@UniqueConstraint(name = "uk_appointment_types_code", columnNames = "code")})
public class AppointmentType {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Identifiant technique stable (UPPERCASE), ex. {@code CONSULTATION}. */
  @Column(nullable = false, length = 64)
  private String code;

  /** Libellé d’affichage, ex. {@code Consultation}. */
  @Column(nullable = false, length = 128)
  private String label;

  /** Couleur hex utilisée pour le badge (ex. {@code #ef4444}). */
  @Column(name = "color_code", nullable = false, length = 16)
  private String colorCode;

  /** Durée par défaut suggérée à la saisie (minutes). */
  @Column(name = "default_duration_minutes", nullable = false)
  private Integer defaultDurationMinutes;

  /** Ordre d’affichage dans la sidebar / select (ascendant). */
  @Column(name = "display_order", nullable = false)
  private Integer displayOrder;

  /** Désactivé = caché des sélecteurs (mais toujours utilisable pour les RDV historiques). */
  @Column(nullable = false)
  private boolean active;

  @Column(precision = 10, scale = 2)
  private BigDecimal price;

  @Column(name = "price_variable", nullable = false)
  private boolean priceVariable;

  @Column(name = "source_practitioner_id")
  private Long sourcePractitionerId;

  @Column(name = "source_act_id")
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
