package com.oracle.service;

import java.util.List;
import java.util.Optional;
import com.oracle.entity.Ticket;

public interface TicketServiceManager {
    Ticket createTicket(Ticket ticket);
    List<Ticket> getAllTickets();
    Optional<Ticket> getTicketById(Integer ticketId);
    Optional<Ticket> updateTicket(Integer ticketId, Ticket ticket);
    boolean deleteTicket(Integer ticketId);
}
