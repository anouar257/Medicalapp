package com.medical.agenda.dto;

import java.util.List;

public class AgendaSlotsDTO {
    private List<String> cabinetHours;
    private List<String> availableSlots;

    public AgendaSlotsDTO() {}

    public AgendaSlotsDTO(List<String> cabinetHours, List<String> availableSlots) {
        this.cabinetHours = cabinetHours;
        this.availableSlots = availableSlots;
    }

    public List<String> getCabinetHours() {
        return cabinetHours;
    }

    public void setCabinetHours(List<String> cabinetHours) {
        this.cabinetHours = cabinetHours;
    }

    public List<String> getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(List<String> availableSlots) {
        this.availableSlots = availableSlots;
    }
}
