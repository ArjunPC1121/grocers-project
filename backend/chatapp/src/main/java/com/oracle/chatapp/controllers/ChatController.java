package com.oracle.chatapp.controllers;
import com.oracle.chatapp.dto.*;
import com.oracle.chatapp.entities.AvailabilityStatus;
import com.oracle.chatapp.services.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/grocers/api/chats")
public class ChatController {
 private final ChatService service; public ChatController(ChatService service){this.service=service;}
 @PostMapping public ChatResponse start(@RequestHeader("X-Authenticated-User-Id") Integer id,@RequestHeader("X-Authenticated-Role") String role){return service.start(id,role);}
 @GetMapping("/mine") public List<ChatResponse> mine(@RequestHeader("X-Authenticated-User-Id") Integer id,@RequestHeader("X-Authenticated-Role") String role){return service.mine(id,role);}
 @GetMapping("/employee/incoming") public List<ChatResponse> incoming(@RequestHeader("X-Authenticated-User-Id") Integer id,@RequestHeader("X-Authenticated-Role") String role){return service.incoming(id,role);}
 @GetMapping("/availability") public Map<String,String> availability(@RequestHeader("X-Authenticated-User-Id") Integer id,@RequestHeader("X-Authenticated-Role") String role){return Map.of("status",service.availability(id,role).name());}
 @PatchMapping("/availability") public Map<String,String> availability(@RequestHeader("X-Authenticated-User-Id") Integer id,@RequestHeader("X-Authenticated-Role") String role,@Valid @RequestBody AvailabilityRequest request){return Map.of("status",service.setAvailability(id,role,request).name());}
 @PostMapping("/{chatId}/accept") public ChatResponse accept(@PathVariable Long chatId,@RequestHeader("X-Authenticated-User-Id") Integer id,@RequestHeader("X-Authenticated-Role") String role,@Valid @RequestBody AcceptChatRequest request){return service.accept(chatId,id,role,request.employeeName());}
 @GetMapping("/{chatId}/messages") public List<MessageResponse> messages(@PathVariable Long chatId,@RequestHeader("X-Authenticated-User-Id") Integer id,@RequestHeader("X-Authenticated-Role") String role){return service.messages(chatId,id,role);}
 @PostMapping("/{chatId}/messages") public MessageResponse send(@PathVariable Long chatId,@RequestHeader("X-Authenticated-User-Id") Integer id,@RequestHeader("X-Authenticated-Role") String role,@Valid @RequestBody SendMessageRequest request){return service.send(chatId,id,role,request);}
 @PostMapping("/{chatId}/end") public ChatResponse end(@PathVariable Long chatId,@RequestHeader("X-Authenticated-User-Id") Integer id,@RequestHeader("X-Authenticated-Role") String role){return service.end(chatId,id,role);}
}
