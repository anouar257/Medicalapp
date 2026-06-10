package com.medical.practitioner.dto;

import com.medical.practitioner.entity.PractitionerAct;
import java.math.BigDecimal;

public class PractitionerActDTO {

    private Long id;
    private String name;
    private int durationMinutes;
    private BigDecimal price;
    private boolean isPriceVariable;
    private String agendaTypeCode;

    public static PractitionerActDTO fromEntity(PractitionerAct act) {
        PractitionerActDTO dto = new PractitionerActDTO();
        dto.id = act.getId();
        dto.name = act.getName();
        dto.durationMinutes = act.getDurationMinutes();
        dto.price = act.getPrice();
        dto.isPriceVariable = act.isPriceVariable();
        if (act.getPractitioner() != null && act.getPractitioner().getId() != null && act.getId() != null) {
            dto.agendaTypeCode = buildAgendaTypeCode(act.getPractitioner().getId(), act.getId());
        }
        return dto;
    }

    public static String buildAgendaTypeCode(Long practitionerId, Long actId) {
        if (practitionerId == null || actId == null) {
            return null;
        }
        return ("ACT_" + practitionerId + "_" + actId).toUpperCase();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public boolean isPriceVariable() { return isPriceVariable; }
    public void setPriceVariable(boolean priceVariable) { this.isPriceVariable = priceVariable; }

    public String getAgendaTypeCode() { return agendaTypeCode; }
    public void setAgendaTypeCode(String agendaTypeCode) { this.agendaTypeCode = agendaTypeCode; }
}
