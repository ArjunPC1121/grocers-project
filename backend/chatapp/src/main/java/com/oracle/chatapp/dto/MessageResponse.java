package com.oracle.chatapp.dto;
import com.oracle.chatapp.entities.ChatMessage;
import java.time.Instant;
public record MessageResponse(Long id, Long sessionId, Integer senderId, String senderRole, String content, Instant sentAt) { public static MessageResponse from(ChatMessage m) { return new MessageResponse(m.getId(),m.getSessionId(),m.getSenderId(),m.getSenderRole(),m.getContent(),m.getSentAt()); } }
