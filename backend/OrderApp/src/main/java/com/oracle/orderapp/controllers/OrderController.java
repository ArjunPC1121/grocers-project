package com.oracle.orderapp.controllers;

import com.oracle.orderapp.dtos.*;import com.oracle.orderapp.entities.OrderStatus;import com.oracle.orderapp.services.abstractions.*;
import jakarta.validation.Valid;import org.springframework.format.annotation.DateTimeFormat;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;import java.util.List;

@RestController
@RequestMapping("/grocers/api/orders")
public class OrderController {
 private final CheckoutService checkout;private final OrderQueryService queries;private final OrderManagementService management;
 public OrderController(CheckoutService checkout,OrderQueryService queries,OrderManagementService management){this.checkout=checkout;this.queries=queries;this.management=management;}
 @PostMapping("/checkout") public ResponseEntity<OrderResponse> checkout(@RequestHeader("X-User-Id") Integer user,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CheckoutRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(checkout.checkout(user,key,request));}
 @GetMapping("/{number}") public OrderResponse get(@RequestHeader("X-User-Id") Integer user,@PathVariable String number){return queries.getUserOrder(user,number);}
 @GetMapping("/users/{userId}") public List<OrderResponse> history(@RequestHeader("X-User-Id") Integer actor,@PathVariable Integer userId){return queries.getUserHistory(actor,userId);}
 @GetMapping("/employee") public List<OrderResponse> employee(@RequestHeader("X-Employee-Id") Integer employee,@RequestParam(required=false) OrderStatus status){return queries.getEmployeeOrders(employee,status);}
 @PatchMapping("/{number}/status") public OrderResponse status(@RequestHeader("X-Employee-Id") Integer employee,@PathVariable String number,@Valid @RequestBody OrderStatusUpdateRequest request){return management.updateStatus(employee,number,request);}
 @PostMapping("/{number}/cancel") public OrderResponse cancel(@RequestHeader("X-Employee-Id") Integer employee,@RequestHeader("Idempotency-Key") String key,@PathVariable String number,@Valid @RequestBody OrderCancellationRequest request){return management.cancel(employee,key,number,request);}
 @GetMapping("/reports") public OrderReportSummary report(@RequestHeader("X-Employee-Id") Integer employee,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,@RequestParam(required=false) Integer userId,@RequestParam(required=false) Integer productId){return queries.report(employee,from,to,userId,productId);}
}
