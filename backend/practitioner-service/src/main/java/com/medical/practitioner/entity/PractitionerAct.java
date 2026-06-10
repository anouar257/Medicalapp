package com.medical.practitioner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * Acte ou type de visite proposé par un praticien (ex. Consultation, Chirurgie).
 */
@Entity
@Table(name = "practitioner_acts")
public class PractitionerAct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false)
    private PractitionerProfile practitioner;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 15;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_price_variable", nullable = false)
    private boolean isPriceVariable = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PractitionerProfile getPractitioner() { return practitioner; }
    public void setPractitioner(PractitionerProfile practitioner) { this.practitioner = practitioner; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public boolean isPriceVariable() { return isPriceVariable; }
    public void setPriceVariable(boolean priceVariable) { this.isPriceVariable = priceVariable; }
}
