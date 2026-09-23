package com.oracle.repository;

import com.oracle.entity.ticket;
import com.oracle.entity.ticketstatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<ticket, Integer> {
    List<ticket> findByStatus(ticketstatus status);
    Optional<ticket> findFirstByUserIdAndStatus(Integer userId, ticketstatus status);
}
