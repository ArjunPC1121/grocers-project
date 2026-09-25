package com.oracle.chatapp.repositories;
import com.oracle.chatapp.entities.EmployeeAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EmployeeAvailabilityRepository extends JpaRepository<EmployeeAvailability,Integer> { }
