package com.oracle.controller;

import java.util.List;

import com.oracle.dto.TicketActionRequest;
import com.oracle.dto.TicketServiceResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.oracle.dto.TicketRequest;
import com.oracle.entity.ticket;
import com.oracle.service.TicketServiceManager;

@RestController
@RequestMapping({"/api/tickets", "/grocers/api/tickets"})
public class TicketController {

    private final TicketServiceManager ticketService;
    private final RestTemplate restTemplate;

    public TicketController(TicketServiceManager ticketService, RestTemplate restTemplate) {
        this.ticketService = ticketService;
        this.restTemplate = restTemplate;
    }


    @PostMapping public ResponseEntity<TicketServiceResponse> createTicket(@Valid @RequestBody TicketRequest request ) {
       ticket ticket = ticketService.createTicket(new ticket(request.userId(), request.lockedReason(), request.requestNote()));
       return ResponseEntity.status(HttpStatus.CREATED).body(new TicketServiceResponse(ticket.getTicketId()));
    }
    @GetMapping public List<ticket> getAllTickets() { return ticketService.getAllTickets(); }
    @GetMapping("/open") public List<ticket> getOpenTickets() { return ticketService.getOpenTickets(); }
    @GetMapping("/user/{userId}/open") public ResponseEntity<ticket> getOpenTicketForUser(@PathVariable Integer userId) {
        return ticketService.getOpenTicketForUser(userId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    @GetMapping("/employee/{employeeId}/history") public List<ticket> getTicketHistoryForEmployee(@PathVariable Integer employeeId) {
        return ticketService.getTicketHistoryForEmployee(employeeId);
    }
    @GetMapping("/{ticketId}") public ResponseEntity<ticket> getTicketById(@PathVariable Integer ticketId) {
        return ticketService.getTicketById(ticketId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    @PostMapping("/{ticketId}/resolve") public ResponseEntity<ticket> resolveTicket(@PathVariable Integer ticketId, @Valid @RequestBody TicketActionRequest request) {
        return ticketService.resolveTicket(ticketId, request.employeeId()).map(ticket -> {
            restTemplate.postForEntity("http://localhost:8080/grocers/api/users/{userId}/unlock", null, Void.class, ticket.getUserId());
            return ResponseEntity.ok(ticket);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
    @PostMapping("/{ticketId}/reject") public ResponseEntity<ticket> rejectTicket(@PathVariable Integer ticketId, @Valid @RequestBody TicketActionRequest request) {
        return ticketService.rejectTicket(ticketId, request.employeeId()).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
