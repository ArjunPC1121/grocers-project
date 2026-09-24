package com.oracle.chatapp.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "support_chat_sessions")
public class ChatSession {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Integer userId;
 private Integer employeeId;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private ChatStatus status = ChatStatus.QUEUED;
 @Column(nullable=false, updatable=false) private Instant createdAt = Instant.now();
 private Instant assignedAt; private Instant endedAt; private String endedBy; private String employeeName;
 @Version private Long version;
 public Long getId(){return id;} public Integer getUserId(){return userId;} public void setUserId(Integer v){userId=v;} public Integer getEmployeeId(){return employeeId;} public void setEmployeeId(Integer v){employeeId=v;} public ChatStatus getStatus(){return status;} public void setStatus(ChatStatus v){status=v;} public Instant getCreatedAt(){return createdAt;} public Instant getAssignedAt(){return assignedAt;} public void setAssignedAt(Instant v){assignedAt=v;} public Instant getEndedAt(){return endedAt;} public void setEndedAt(Instant v){endedAt=v;} public String getEndedBy(){return endedBy;} public void setEndedBy(String v){endedBy=v;} public String getEmployeeName(){return employeeName;} public void setEmployeeName(String v){employeeName=v;}
}
