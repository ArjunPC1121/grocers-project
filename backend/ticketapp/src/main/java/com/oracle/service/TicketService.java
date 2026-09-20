package com.oracle.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.oracle.dto.TicketRequest;
import com.oracle.entity.Ticket;
import com.oracle.repository.TicketRepository;

@Service
public class TicketService implements TicketServiceManager {
    private final TicketRepository ticketRepository;
    public TicketService(TicketRepository ticketRepository) { this.ticketRepository = ticketRepository; }
    @Override public Ticket createTicket(Ticket ticket) {
        
        return ticketRepository.save(ticket); 
    }
    @Override public List<Ticket> getAllTickets() { return ticketRepository.findAll(); }
    @Override public Optional<Ticket> getTicketById(Integer ticketId) { return ticketRepository.findById(ticketId); }
    @Override public Optional<Ticket> updateTicket(Integer ticketId, Ticket ticket) {
        return ticketRepository.findById(ticketId).map(existing -> {
            existing.setUserId(ticket.getUserId());
            existing.setEmployeeId(ticket.getEmployeeId());
            existing.setStatus(ticket.getStatus());
           
            return ticketRepository.save(existing);
        });
    }
    @Override public boolean deleteTicket(Integer ticketId) {
        if (!ticketRepository.existsById(ticketId)) return false;
        ticketRepository.deleteById(ticketId);
        return true;
    }
}
