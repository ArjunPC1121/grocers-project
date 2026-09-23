package com.oracle.service;

import java.util.List;
import java.util.Optional;
import com.oracle.entity.ticket;

public interface TicketServiceManager {
    ticket createTicket(ticket ticket);
    List<ticket> getAllTickets();
    Optional<ticket> getTicketById(Integer ticketId);
    List<ticket> getOpenTickets();
    Optional<ticket> getOpenTicketForUser(Integer userId);
    List<ticket> getTicketHistoryForEmployee(Integer employeeId);
    Optional<ticket> resolveTicket(Integer ticketId, Integer employeeId);
    Optional<ticket> rejectTicket(Integer ticketId, Integer employeeId);
    boolean deleteTicket(Integer ticketId);
}
