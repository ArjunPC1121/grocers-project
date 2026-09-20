package com.oracle.controller;

import java.net.URI;
import java.util.List;

import com.oracle.dto.TicketServiceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oracle.dto.TicketRequest;
import com.oracle.entity.Ticket;
import com.oracle.service.TicketServiceManager;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketServiceManager ticketService;
    private Ticket ticket;

    public TicketController(TicketServiceManager ticketService) { this.ticketService = ticketService; }


    @PostMapping public ResponseEntity<TicketServiceResponse> createTicket(@RequestBody TicketRequest request ) {
       Ticket ticket = new Ticket();
       ticket.setUserId(request.userId());
       ticket.setLockedReason(request.lockedReason());
       ticketService.createTicket(ticket);
       TicketServiceResponse ticketServiceResponse = new TicketServiceResponse(ticket.getTicketId());
       return ResponseEntity.status(201).body(ticketServiceResponse);
    }
    @GetMapping public List<Ticket> getAllTickets() { return ticketService.getAllTickets(); }
    @GetMapping("/{ticketId}") public ResponseEntity<Ticket> getTicketById(@PathVariable Integer ticketId) {
        return ticketService.getTicketById(ticketId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    @PutMapping("/{ticketId}") public ResponseEntity<Ticket> updateTicket(@PathVariable Integer ticketId, @RequestBody Ticket ticket) {
        return ticketService.updateTicket(ticketId, ticket).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    @DeleteMapping("/{ticketId}") public ResponseEntity<Void> deleteTicket(@PathVariable Integer ticketId) {
        return ticketService.deleteTicket(ticketId) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();

    
    }
}
