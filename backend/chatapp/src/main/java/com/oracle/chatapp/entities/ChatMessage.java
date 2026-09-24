package com.oracle.chatapp.entities;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="support_chat_messages")
public class ChatMessage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long sessionId; @Column(nullable=false) private Integer senderId; @Column(nullable=false, length=16) private String senderRole; @Column(nullable=false,length=2000) private String content; @Column(nullable=false) private Instant sentAt=Instant.now();
 public Long getId(){return id;} public Long getSessionId(){return sessionId;} public void setSessionId(Long v){sessionId=v;} public Integer getSenderId(){return senderId;} public void setSenderId(Integer v){senderId=v;} public String getSenderRole(){return senderRole;} public void setSenderRole(String v){senderRole=v;} public String getContent(){return content;} public void setContent(String v){content=v;} public Instant getSentAt(){return sentAt;}
}
