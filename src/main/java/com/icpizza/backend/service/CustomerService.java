package com.icpizza.backend.service;

import com.icpizza.backend.dto.CreateOrderTO;
import com.icpizza.backend.entity.Customer;
import com.icpizza.backend.entity.Order;
import com.icpizza.backend.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private static final ZoneId BAHRAIN = ZoneId.of("Asia/Bahrain");

    Random random = new Random();


    public Optional<Customer> findCustomer(String telephoneNo){
        return customerRepository.findByTelephoneNo(telephoneNo);
    }

    @Transactional
    public Customer createCustomer(CreateOrderTO order){
        Customer customer = new Customer();
        customer.setName(order.customerName());
        customer.setTelephoneNo(order.telephoneNo());
        customer.setAmountOfOrders(1);
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
        System.out.println("Updated values : " + customer.getAmountOfOrders() + customer.getAmountPaid());
    }

    public Customer createWatsappCustomer(String senderPhone) {
        Customer customer = new Customer();
        customer.setTelephoneNo(senderPhone);
        customer.setId(String.format("%08d", ThreadLocalRandom.current().nextInt(1, 100000000)));
        customer.setWaitingForName(1);
        customerRepository.save(customer);
        return customer;
    }

    public boolean isWaitForName(String senderPhone) {
        Optional<Customer> customer = customerRepository.findByTelephoneNo(senderPhone);
        if(customer.isPresent() && customer.get().getWaitingForName()==1) return true;
        else return false;
    }

    public boolean userHasName(String senderPhone) {
        Optional<Customer> customer = customerRepository.findByTelephoneNo(senderPhone);
        if(customer.isPresent() && customer.get().getName()!=null) return true;
        else return false;
    }

    public Customer saveUserName(String senderPhone, String messageText) {
        Optional<Customer> customer = customerRepository.findByTelephoneNo(senderPhone);
        if (customer.isPresent()){
            Customer customerToEdit = customer.get();
            customerToEdit.setName(messageText);
            customerToEdit.setWaitingForName(0);
            customerRepository.saveAndFlush(customerToEdit);
            return customerToEdit;
        }
        else throw new IllegalArgumentException("Customer with phone number "+senderPhone+" is not found");
    }

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
