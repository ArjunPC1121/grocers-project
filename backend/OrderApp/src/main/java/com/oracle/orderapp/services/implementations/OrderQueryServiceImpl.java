package com.oracle.orderapp.services.implementations;

import com.oracle.orderapp.dtos.*;
import com.oracle.orderapp.dtos.clients.*;
import com.oracle.orderapp.entities.*;
import com.oracle.orderapp.exceptions.*;
import com.oracle.orderapp.repositories.OrderRepository;
import com.oracle.orderapp.services.abstractions.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderQueryServiceImpl implements OrderQueryService {
    private final OrderRepository orders; private final UserClient users; private final EmployeeClient employees; private final OrderMapper mapper;
    public OrderQueryServiceImpl(OrderRepository orders, UserClient users, EmployeeClient employees, OrderMapper mapper) {
        this.orders=orders; this.users=users; this.employees=employees; this.mapper=mapper;
    }
    public OrderResponse getUserOrder(Integer userId, String number) {
        verifyUser(userId); Order order = find(number);
        if (!userId.equals(order.getUserId())) throw new AccessDeniedException("USER_ACCESS_DENIED", "Order belongs to another user");
        return mapper.toResponse(order);
    }
    public List<OrderResponse> getUserHistory(Integer actor, Integer requested) {
        if (!actor.equals(requested)) throw new AccessDeniedException("USER_ACCESS_DENIED", "Cannot view another user's orders");
        verifyUser(actor); return orders.findByUserIdOrderByOrderedAtDesc(requested).stream().map(mapper::toResponse).toList();
    }
    public List<OrderResponse> getEmployeeOrders(Integer employeeId, OrderStatus status) {
        EmployeeVerificationResponse v=employees.verify(employeeId);
        if(v==null||!v.valid()) throw new AccessDeniedException("INVALID_EMPLOYEE","Employee verification failed");
        List<Order> found=status==null?orders.findAllByOrderByOrderedAtDesc():orders.findByStatusOrderByOrderedAtDesc(status);
        return found.stream().map(mapper::toResponse).toList();
    }
    public OrderReportSummary report(Integer employeeId, LocalDateTime from, LocalDateTime to, Integer userId, Integer productId) {
        verifyEmployee(employeeId);
        if (from == null || to == null || !from.isBefore(to))
            throw new OrderAppException("INVALID_REPORT_RANGE", "from must be before to", org.springframework.http.HttpStatus.BAD_REQUEST);
        Specification<Order> spec=(root,q,cb)->cb.and(cb.greaterThanOrEqualTo(root.get("orderedAt"),from),cb.lessThan(root.get("orderedAt"),to));
        if(userId!=null) spec=spec.and((r,q,cb)->cb.equal(r.get("userId"),userId));
        if(productId!=null) spec=spec.and((r,q,cb)->{q.distinct(true);return cb.equal(r.join("items").get("productId"),productId);});
        List<Order> found=orders.findAll(spec);
        List<OrderReportRow> rows=found.stream().map(o->new OrderReportRow(o.getOrderNumber(),o.getUserId(),o.getStatus(),o.getTotalAmount(),o.getOrderedAt())).toList();
        return new OrderReportSummary(rows.size(),OrderMapper.round2(rows.stream().mapToDouble(OrderReportRow::totalAmount).sum()),rows);
    }
    private void verifyUser(Integer id){UserVerificationResponse v=users.verify(id);if(v==null||!v.valid()||!id.equals(v.userId()))throw new AccessDeniedException("INVALID_USER","User verification failed");}
    private void verifyEmployee(Integer id){EmployeeVerificationResponse v=employees.verify(id);if(v==null||!v.valid()||!id.equals(v.employeeId()))throw new AccessDeniedException("INVALID_EMPLOYEE","Employee verification failed");}
    private Order find(String n){return orders.findByOrderNumber(n).orElseThrow(()->new NotFoundException("ORDER_NOT_FOUND","Order not found"));}
}
