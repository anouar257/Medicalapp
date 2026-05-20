package com.medical.agenda.repository;

import com.medical.agenda.entity.AppointmentType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentTypeRepository extends JpaRepository<AppointmentType, Long> {

  Optional<AppointmentType> findByCode(String code);

  List<AppointmentType> findAllByOrderByDisplayOrderAscIdAsc();

  List<AppointmentType> findByActiveTrueOrderByDisplayOrderAsc();

  boolean existsByCode(String code);
}
