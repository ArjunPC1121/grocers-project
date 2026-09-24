package com.oracle.chatapp.entities;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="employee_chat_availability")
public class EmployeeAvailability {
 @Id private Integer employeeId; @Enumerated(EnumType.STRING) @Column(nullable=false) private AvailabilityStatus status=AvailabilityStatus.OFFLINE; private Instant updatedAt=Instant.now();
 public Integer getEmployeeId(){return employeeId;} public void setEmployeeId(Integer v){employeeId=v;} public AvailabilityStatus getStatus(){return status;} public void setStatus(AvailabilityStatus v){status=v;} public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}
