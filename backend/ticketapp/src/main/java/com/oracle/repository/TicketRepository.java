package com.oracle.repository;

import com.oracle.entity.ticket;
import com.oracle.entity.ticketstatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<ticket, Integer> {
    List<ticket> findByStatus(ticketstatus status);
}
