package com.icpizza.backend.service;

import com.icpizza.backend.dto.CheckCustomerResponse;
import com.icpizza.backend.dto.CreateOrderTO;
import com.icpizza.backend.entity.Customer;
import com.icpizza.backend.entity.Order;
import com.icpizza.backend.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {
    private final CustomerRepository customerRepository;
    private static final ZoneId BAHRAIN = ZoneId.of("Asia/Bahrain");

    @Transactional
    public Optional<Customer> findCustomer(String telephoneNo){
        return customerRepository.findByTelephoneNo(telephoneNo);
    }

    @Transactional
    public CheckCustomerResponse checkCustomer(String telephoneNo) {
        Optional<Customer> customer = customerRepository.findByTelephoneNo(telephoneNo);
        Boolean isNewCustomer = customer.isPresent() ?
                ((customer.get().getAmountOfOrders() > 0) ?
                        Boolean.FALSE :
                        Boolean.TRUE) :
                Boolean.TRUE;
        return new CheckCustomerResponse(isNewCustomer);
    }

    @Transactional
    public Customer createCustomer(CreateOrderTO order){
        Customer customer = new Customer();
        customer.setName(order.customerName());
        customer.setTelephoneNo(order.telephoneNo());
        customer.setAmountOfOrders(0);
        customer.setAmountPaid(order.amountPaid());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        customer.setLastOrder(LocalDateTime.now(BAHRAIN).format(fmt));
        customer.setId(String.format("%08d", ThreadLocalRandom.current().nextInt(1, 100000000)));
        customer.setAddress(null);
        customer.setWaitingForName(null);

        customerRepository.save(customer);
        return customer;
    }

    @Transactional
    public void updateCustomer(Order order, Customer customer){
        customer.setAmountOfOrders(customer.getAmountOfOrders() + 1);
        customer.setAmountPaid(customer.getAmountPaid().add(order.getAmountPaid()));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        customer.setLastOrder(LocalDateTime.now(BAHRAIN).format(fmt));
        customerRepository.save(customer);
        log.info("Updated values : " + customer.getAmountOfOrders() + customer.getAmountPaid());
    }

    @Transactional
    public Customer createWatsappCustomer(String senderPhone) {
        Customer customer = new Customer();
        customer.setTelephoneNo(senderPhone);
        customer.setId(String.format("%08d", ThreadLocalRandom.current().nextInt(1, 100000000)));
        customer.setWaitingForName(1);
        customer.setAmountPaid(BigDecimal.ZERO);
        customerRepository.save(customer);
        return customer;
    }

    @Transactional(readOnly = true)
    public boolean isWaitForName(String senderPhone) {
        return customerRepository.findByTelephoneNoWithoutLock(senderPhone)
                .map(Customer::getWaitingForName)
                .map(v -> v == 1)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean userHasName(String senderPhone) {
        Optional<Customer> customer = customerRepository.findByTelephoneNoWithoutLock(senderPhone);
        if(customer.isPresent() && customer.get().getName()!=null) return true;
        else return false;
    }

    @Transactional(readOnly = true)
    public Customer saveUserName(String senderPhone, String messageText) {
        Optional<Customer> customer = customerRepository.findByTelephoneNoWithoutLock(senderPhone);
        if (customer.isPresent()){
            Customer customerToEdit = customer.get();
            customerToEdit.setName(messageText);
            customerToEdit.setWaitingForName(0);
            customerRepository.saveAndFlush(customerToEdit);
            return customerToEdit;
        }
        else throw new IllegalArgumentException("Customer with phone number "+senderPhone+" is not found");
    }

    @Transactional
    public void setWaitForName(String senderPhone, boolean b) {
        Optional<Customer> customer = customerRepository.findByTelephoneNo(senderPhone);
        if (customer.isPresent()){
            Customer customerToEdit = customer.get();
            customerToEdit.setWaitingForName(b ? 1 : 0);
            customerRepository.save(customerToEdit);
        }
        else throw new IllegalArgumentException("Customer with phone number "+senderPhone+" is not found");
    }
}
