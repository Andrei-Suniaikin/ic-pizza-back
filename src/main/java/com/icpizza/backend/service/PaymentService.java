package com.icpizza.backend.service;

import com.icpizza.backend.dto.OrderPaymentTO;
import com.icpizza.backend.entity.Branch;
import com.icpizza.backend.entity.Order;
import com.icpizza.backend.entity.Transaction;
import com.icpizza.backend.enums.OrderStatus;
import com.icpizza.backend.repository.BranchRepository;
import com.icpizza.backend.repository.OrderRepository;
import com.icpizza.backend.repository.TransactionRepository;
import com.icpizza.backend.websocket.OrderEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;
    private final BranchRepository branchRepository;
    private static final ZoneId BAHRAIN = ZoneId.of("Asia/Bahrain");
    private final OrderEvents orderEvents;

    @Transactional
    public void orderPayment(OrderPaymentTO orderPaymentTO) {
        Order order = orderRepository.findById(Long.valueOf(orderPaymentTO.orderId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order with id "+orderPaymentTO.orderId()+" not found"));

        Branch branch = branchRepository.findByBranchNumber(orderPaymentTO.branchId());

        Transaction transaction = new Transaction();
        transaction.setOrder(order);
        transaction.setType(orderPaymentTO.type());
        transaction.setAmount(orderPaymentTO.amount());
        transaction.setBranch(branch);
        transaction.setDateTime(LocalDateTime.now(BAHRAIN));

        transactionRepository.save(transaction);

        order.setIsPaid(true);
        orderRepository.save(order);
        orderEvents.pushPaid(order.getId(), order.getBranch().getId());
    }
}
