package com.icpizza.backend.service;

import com.icpizza.backend.dto.CreateOrderResponse;
import com.icpizza.backend.dto.CreateOrderTO;
import com.icpizza.backend.entity.*;
import com.icpizza.backend.enums.WorkLoadLevel;
import com.icpizza.backend.repository.*;
import com.icpizza.backend.testutil.BaseIT;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class OrderServiceIT extends BaseIT {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private ComboItemRepository comboItemRepository;

    @Test
    void createWebsiteOrder_withNewTelephone_createsCustomerOrderAndItems() {
        // given
        Branch branch = new Branch();

        branch.setWorkLoadLevel(WorkLoadLevel.IDLE);
        branch.setAddress("Al-Hidd");
        branch.setExternalId("IC-PIZZA-BH-AL-HIDD");
        branch.setBranchNumber(2);
        branch.setBranchName("IC-PIZZA-BH-AL-HIDD");


        branchRepository.saveAndFlush(branch);

        String telephone = "973111111111";
        assertThat(customerRepository.findByTelephoneNo(telephone)).isEmpty();

        CreateOrderTO createOrderTO = new CreateOrderTO(
                telephone,
                "Test User",
                BigDecimal.valueOf(100.00),
                List.of(new CreateOrderTO.OrderItemsTO(
                                BigDecimal.valueOf(100.00),
                                "Combo Items",
                                "Pizza with no cheese pls",
                                BigDecimal.ZERO,
                                false,
                                false,
                                "Pizza Combo S",
                                1,
                                "S",
                                List.of(new CreateOrderTO.OrderItemsTO.ComboItemsTO(
                                                "Pizzas",
                                                "Pizza Pepperoni",
                                                "S",
                                                true,
                                                false,
                                                1,
                                                "Pizza with no cheese"),
                                        new CreateOrderTO.OrderItemsTO.ComboItemsTO(
                                                "Sauces",
                                                "BBQ",
                                                null,
                                                false,
                                                false,
                                                1,
                                                null),
                                        new CreateOrderTO.OrderItemsTO.ComboItemsTO(
                                                "Beverages",
                                                "Coca Cola",
                                                null,
                                                false,
                                                false,
                                                1,
                                                null
                                        )
                                )
                        )
                ),
                "Benefit",
                "Pick Up",
                null,
                null,
                false,
                branch.getBranchNumber()
        );

        // when
        CreateOrderResponse response = orderService.createWebsiteOrder(createOrderTO);

        // then
        Customer customer = customerRepository.findByTelephoneNo(telephone)
                .orElseThrow(() -> new AssertionError("Customer not persisted"));

        Order order = orderRepository.findById(response.id())
                .orElseThrow(() -> new AssertionError("Order not persisted"));

        assertThat(order.getCustomer().getTelephoneNo()).isEqualTo(customer.getTelephoneNo());
        assertThat(order.getBranch().getBranchNumber()).isEqualTo(branch.getBranchNumber());

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        assertThat(orderItems).isNotEmpty();

        List<ComboItem> comboItems = comboItemRepository.findByOrderItemId(orderItems.getFirst().getId());
        assertThat(comboItems).isNotEmpty();

        assertThat(response.telephoneNo()).isEqualTo(telephone);
        assertThat(response.orderItems().size()).isEqualTo(orderItems.size());
    }

    @Test
    void createWebsiteOrder_withExistingTelephone_createsOrderItems() {
        //given
        Branch branch = new Branch();

        branch.setWorkLoadLevel(WorkLoadLevel.IDLE);
        branch.setAddress("Al-Hidd");
        branch.setExternalId("IC-PIZZA-BH-AL-HIDD");
        branch.setBranchNumber(2);
        branch.setBranchName("IC-PIZZA-BH-AL-HIDD");


        branchRepository.saveAndFlush(branch);

        Customer customer = new Customer();
        customer.setTelephoneNo("97311111111");
        customer.setAddress("Al-Hidd");
        customer.setAmountPaid(BigDecimal.ZERO);
        customer.setAmountOfOrders(0);
        customer.setId("12345678");
        customer.setName("Test");

        BigDecimal oldAmountPaid = customer.getAmountPaid();
        Integer oldAmountOfOrders = customer.getAmountOfOrders();

        customerRepository.saveAndFlush(customer);
        assertThat(customerRepository.findByTelephoneNo(customer.getTelephoneNo())).isNotEmpty();

        CreateOrderTO createOrderTO = new CreateOrderTO(
                customer.getTelephoneNo(),
                "Test User",
                BigDecimal.valueOf(100.00),
                List.of(new CreateOrderTO.OrderItemsTO(
                                BigDecimal.valueOf(100.00),
                                "Combo Items",
                                "Pizza with no cheese pls",
                                BigDecimal.ZERO,
                                false,
                                false,
                                "Pizza Combo S",
                                1,
                                "S",
                                List.of(new CreateOrderTO.OrderItemsTO.ComboItemsTO(
                                                "Pizzas",
                                                "Pizza Pepperoni",
                                                "S",
                                                true,
                                                false,
                                                1,
                                                "Pizza with no cheese"),
                                        new CreateOrderTO.OrderItemsTO.ComboItemsTO(
                                                "Sauces",
                                                "BBQ",
                                                null,
                                                false,
                                                false,
                                                1,
                                                null),
                                        new CreateOrderTO.OrderItemsTO.ComboItemsTO(
                                                "Beverages",
                                                "Coca Cola",
                                                null,
                                                false,
                                                false,
                                                1,
                                                null
                                        )
                                )
                        )
                ),
                "Benefit",
                "Pick Up",
                null,
                null,
                false,
                branch.getBranchNumber()
        );

        //when

        CreateOrderResponse response = orderService.createWebsiteOrder(createOrderTO);

        //then

        Customer thenCustomer = customerRepository.findByTelephoneNo(response.telephoneNo())
                        .orElseThrow(() -> new AssertionError("Customer is not persisted"));

        Order order = orderRepository.findById(response.id())
                .orElseThrow(() -> new AssertionError("Order not persisted"));

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        assertThat(orderItems).isNotEmpty();
        assertThat(comboItemRepository.findByOrderItemId(orderItems.getFirst().getId())).isNotEmpty();
//        assertThat(thenCustomer.getAmountPaid()).isGreaterThan(oldAmountPaid);
//        assertThat(thenCustomer.getAmountOfOrders()).isGreaterThan(oldAmountOfOrders);
        assertThat(response.telephoneNo()).isEqualTo(thenCustomer.getTelephoneNo());
        assertThat(response.orderItems().size()).isEqualTo(orderItems.size());
    }
}
