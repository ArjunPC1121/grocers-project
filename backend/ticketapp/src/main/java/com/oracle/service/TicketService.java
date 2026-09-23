package com.oracle.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.oracle.entity.ticket;
import com.oracle.entity.ticketstatus;
import com.oracle.repository.TicketRepository;

@Service
public class TicketService implements TicketServiceManager {
    private final TicketRepository ticketRepository;
    public TicketService(TicketRepository ticketRepository) { this.ticketRepository = ticketRepository; }
    @Override public ticket createTicket(ticket ticket) {
        return ticketRepository.findFirstByUserIdAndStatus(ticket.getUserId(), ticketstatus.OPEN)
                .orElseGet(() -> ticketRepository.save(ticket));
    }
    @Override public List<ticket> getAllTickets() { return ticketRepository.findAll(); }
    @Override public Optional<ticket> getTicketById(Integer ticketId) { return ticketRepository.findById(ticketId); }
    @Override public List<ticket> getOpenTickets() { return ticketRepository.findByStatus(ticketstatus.OPEN); }
    @Override public Optional<ticket> getOpenTicketForUser(Integer userId) { return ticketRepository.findFirstByUserIdAndStatus(userId, ticketstatus.OPEN); }
    @Override public List<ticket> getTicketHistoryForEmployee(Integer employeeId) { return ticketRepository.findByEmployeeIdOrderByUpdatedAtDesc(employeeId); }
    @Override public Optional<ticket> resolveTicket(Integer ticketId, Integer employeeId) {
        return ticketRepository.findById(ticketId).map(existing -> {
            if (existing.getStatus() != ticketstatus.OPEN) throw new IllegalStateException("Only open tickets can be resolved");
            existing.resolve(employeeId);
            return ticketRepository.save(existing);
        });
    }
    @Override public Optional<ticket> rejectTicket(Integer ticketId, Integer employeeId) {
        return ticketRepository.findById(ticketId).map(existing -> {
            if (existing.getStatus() != ticketstatus.OPEN) throw new IllegalStateException("Only open tickets can be rejected");
            existing.reject(employeeId);
            return ticketRepository.save(existing);
        });
    }
    @Override public boolean deleteTicket(Integer ticketId) {
        if (!ticketRepository.existsById(ticketId)) return false;
        ticketRepository.deleteById(ticketId);
        return true;
    }
}
