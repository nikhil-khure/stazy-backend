package com.stazy.backend.profile.repository;

import com.stazy.backend.profile.entity.City;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, Long> {

    Optional<City> findByNameIgnoreCaseAndStateIgnoreCase(String name, String state);

    Optional<City> findByNameIgnoreCaseAndStateIgnoreCaseAndCountryIgnoreCase(String name, String state, String country);
}
