package com.oracle.chatapp.dto;
import com.oracle.chatapp.entities.ChatSession;
import java.time.Instant;
public record ChatResponse(Long id, Integer userId, Integer employeeId, String employeeName, String status, Instant createdAt, Instant assignedAt, Instant endedAt, String endedBy) { public static ChatResponse from(ChatSession s) { return new ChatResponse(s.getId(),s.getUserId(),s.getEmployeeId(),s.getEmployeeName(),s.getStatus().name(),s.getCreatedAt(),s.getAssignedAt(),s.getEndedAt(),s.getEndedBy()); } }
