package com.medical.practitioner.service;

import com.medical.practitioner.dto.BookingWizardChoiceDTO;
import com.medical.practitioner.dto.BookingWizardOptionsDTO;
import com.medical.practitioner.entity.SpecialtyBookingChoice;
import com.medical.practitioner.entity.SpecialtyBookingStep;
import com.medical.practitioner.repository.SpecialtyBookingChoiceRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingWizardService {

  private final SpecialtyBookingChoiceRepository choiceRepository;

  public BookingWizardService(SpecialtyBookingChoiceRepository choiceRepository) {
    this.choiceRepository = choiceRepository;
  }

  @Transactional(readOnly = true)
  public BookingWizardOptionsDTO optionsForSpecialtyCode(String rawCode) {
    String requested = rawCode == null || rawCode.isBlank() ? "DEFAULT" : rawCode.trim().toUpperCase();
    List<SpecialtyBookingChoice> rows = choiceRepository.findBySpecialtyCode(requested);
    String resolved = requested;
    if (rows.isEmpty() && !"DEFAULT".equals(requested)) {
      rows = choiceRepository.findBySpecialtyCode("DEFAULT");
      resolved = "DEFAULT";
    }
    if (rows.isEmpty()) {
      rows = choiceRepository.findBySpecialtyCode("MEDECINE_GENERALE");
      resolved = "MEDECINE_GENERALE";
    }

    BookingWizardOptionsDTO dto = new BookingWizardOptionsDTO();
    dto.setRequestedSpecialtyCode(requested);
    dto.setResolvedSpecialtyCode(resolved);
    for (SpecialtyBookingChoice c : rows) {
      BookingWizardChoiceDTO item = new BookingWizardChoiceDTO();
      item.setStep(c.getStep());
      item.setCode(c.getCode());
      item.setLabelFr(c.getLabelFr());
      item.setSortOrder(c.getSortOrder());
      if (c.getStep() == SpecialtyBookingStep.PRIOR_CARE) {
        dto.getChoicesPrior().add(item);
      } else {
        dto.getChoicesVisit().add(item);
      }
    }
    return dto;
  }
}
