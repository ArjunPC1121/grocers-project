package com.oracle.chatapp.services;

import com.oracle.chatapp.dto.*;
import com.oracle.chatapp.entities.*;
import com.oracle.chatapp.repositories.*;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class ChatService {
 private final ChatSessionRepository sessions; private final ChatMessageRepository messages; private final EmployeeAvailabilityRepository availability;
 public ChatService(ChatSessionRepository sessions, ChatMessageRepository messages, EmployeeAvailabilityRepository availability) { this.sessions=sessions; this.messages=messages; this.availability=availability; }
 private void requireRole(String role, String... allowed) { if (Arrays.stream(allowed).noneMatch(r->r.equals(role))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You do not have access to chat support."); }
 private ChatSession session(Long id) { return sessions.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Chat session not found.")); }
 private void participant(ChatSession s,Integer id,String role) { boolean customer=("USER".equals(role)||"CUSTOMER".equals(role))&&s.getUserId().equals(id); boolean employee="EMPLOYEE".equals(role)&&id.equals(s.getEmployeeId()); if(!customer&&!employee) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This chat is not assigned to you."); }
 private boolean inactive(ChatSession s) { if(s.getStatus()!=ChatStatus.ACTIVE||s.getAssignedAt()==null)return false;Instant lastCustomerActivity=messages.findFirstBySessionIdAndSenderRoleOrderBySentAtDesc(s.getId(),"CUSTOMER").map(ChatMessage::getSentAt).orElse(s.getAssignedAt());return Duration.between(lastCustomerActivity,Instant.now()).toSeconds()>=60; }
 private void releaseEmployee(ChatSession s) { if(s.getEmployeeId()==null)return;EmployeeAvailability a=availability.findById(s.getEmployeeId()).orElse(null);if(a!=null){a.setStatus(AvailabilityStatus.AVAILABLE);a.setUpdatedAt(Instant.now());availability.save(a);} }
 private boolean endIfInactive(ChatSession s) { if(!inactive(s))return false;s.setStatus(ChatStatus.ENDED);s.setEndedAt(Instant.now());s.setEndedBy("INACTIVITY");releaseEmployee(s);sessions.save(s);return true; }
 @Transactional public ChatResponse start(Integer userId,String role) { requireRole(role,"USER","CUSTOMER"); return sessions.findFirstByUserIdAndStatusInOrderByCreatedAtDesc(userId,List.of(ChatStatus.QUEUED,ChatStatus.ACTIVE)).map(ChatResponse::from).orElseGet(()->{ ChatSession s=new ChatSession();s.setUserId(userId);return ChatResponse.from(sessions.save(s));}); }
 public List<ChatResponse> mine(Integer id,String role) { requireRole(role,"USER","CUSTOMER","EMPLOYEE");return ("EMPLOYEE".equals(role)?sessions.findByEmployeeIdOrderByCreatedAtDesc(id):sessions.findByUserIdOrderByCreatedAtDesc(id)).stream().map(ChatResponse::from).toList(); }
 public List<ChatResponse> incoming(Integer employeeId,String role) { requireRole(role,"EMPLOYEE"); if(availability.findById(employeeId).map(EmployeeAvailability::getStatus).orElse(AvailabilityStatus.OFFLINE)!=AvailabilityStatus.AVAILABLE) return List.of(); return sessions.findByStatusOrderByCreatedAtAsc(ChatStatus.QUEUED).stream().map(ChatResponse::from).toList(); }
 @Transactional public ChatResponse accept(Long chatId,Integer employeeId,String role,String employeeName) { requireRole(role,"EMPLOYEE"); EmployeeAvailability state=availability.findById(employeeId).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Set yourself available before accepting a customer chat.")); if(state.getStatus()!=AvailabilityStatus.AVAILABLE) throw new ResponseStatusException(HttpStatus.CONFLICT,"You are not currently available for chats."); boolean hasActive=sessions.findByEmployeeIdOrderByCreatedAtDesc(employeeId).stream().anyMatch(s->s.getStatus()==ChatStatus.ACTIVE); if(hasActive) throw new ResponseStatusException(HttpStatus.CONFLICT,"Finish your current chat before accepting another."); ChatSession s=session(chatId); if(s.getStatus()!=ChatStatus.QUEUED) throw new ResponseStatusException(HttpStatus.CONFLICT,"Another employee already accepted this chat."); s.setEmployeeId(employeeId);s.setEmployeeName(employeeName.trim());s.setStatus(ChatStatus.ACTIVE);s.setAssignedAt(Instant.now());state.setStatus(AvailabilityStatus.BUSY);state.setUpdatedAt(Instant.now());availability.save(state);return ChatResponse.from(sessions.save(s)); }
 public List<MessageResponse> messages(Long chatId,Integer id,String role) { ChatSession s=session(chatId);participant(s,id,role);return messages.findBySessionIdOrderBySentAtAsc(chatId).stream().map(MessageResponse::from).toList(); }
 @Transactional public MessageResponse send(Long chatId,Integer id,String role,SendMessageRequest request) { ChatSession s=session(chatId);participant(s,id,role);if(s.getStatus()!=ChatStatus.ACTIVE)throw new ResponseStatusException(HttpStatus.CONFLICT,"This chat is closed or still waiting for an employee.");ChatMessage m=new ChatMessage();m.setSessionId(chatId);m.setSenderId(id);m.setSenderRole("EMPLOYEE".equals(role)?"EMPLOYEE":"CUSTOMER");m.setContent(request.content().trim());return MessageResponse.from(messages.save(m)); }
 @Transactional public ChatResponse end(Long chatId,Integer id,String role) { ChatSession s=session(chatId);participant(s,id,role);if(s.getStatus()==ChatStatus.ENDED)return ChatResponse.from(s);s.setStatus(ChatStatus.ENDED);s.setEndedAt(Instant.now());s.setEndedBy("EMPLOYEE".equals(role)?"EMPLOYEE":"CUSTOMER");releaseEmployee(s);return ChatResponse.from(sessions.save(s)); }
 @Scheduled(fixedDelay=2000)
 @Transactional public void closeInactiveChats() { sessions.findByStatusOrderByCreatedAtAsc(ChatStatus.ACTIVE).forEach(this::endIfInactive); }
 @Transactional public AvailabilityStatus setAvailability(Integer employeeId,String role,AvailabilityRequest request) { requireRole(role,"EMPLOYEE"); EmployeeAvailability a=availability.findById(employeeId).orElseGet(()->{EmployeeAvailability x=new EmployeeAvailability();x.setEmployeeId(employeeId);return x;});a.setStatus(request.status());a.setUpdatedAt(Instant.now());return availability.save(a).getStatus(); }
 public AvailabilityStatus availability(Integer employeeId,String role) { requireRole(role,"EMPLOYEE");return availability.findById(employeeId).map(EmployeeAvailability::getStatus).orElse(AvailabilityStatus.OFFLINE); }
}
