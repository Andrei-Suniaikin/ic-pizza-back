package com.icpizza.backend.service;

import com.icpizza.backend.dto.blacklist.BlackListRequest;
import com.icpizza.backend.dto.blacklist.BlackListResponse;
import com.icpizza.backend.entity.BlackListCstmr;
import com.icpizza.backend.entity.Customer;
import com.icpizza.backend.repository.BlackListRepository;
import com.icpizza.backend.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlackListService {
    private final BlackListRepository blackListRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public BlackListResponse addToBlackList(BlackListRequest blackListRequest) {
        log.info("[BLACKLIST] Adding a new customer to blacklist... {}",  blackListRequest.telephoneNo());
        Customer customer = customerRepository.findByTelephoneNo(blackListRequest.telephoneNo()).orElseThrow(
                ()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found, check the telephone number and try again.")
        );
        Optional<BlackListCstmr> optionalBlackListCstmr = blackListRepository.findByTelephoneNo(blackListRequest.telephoneNo());
        if (optionalBlackListCstmr.isPresent()) {
            log.info("[BLACKLIST] Customer already exists in blacklist");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Customer already exists in the blacklist");
        }
        BlackListCstmr newBlackListCstmr = new BlackListCstmr();
        newBlackListCstmr.setCustomer(customer);
        blackListRepository.save(newBlackListCstmr);
        return new BlackListResponse(newBlackListCstmr.getCustomer().getTelephoneNo(), newBlackListCstmr.getId());
    }

    @Transactional
    public void deleteFromBlackList(BlackListRequest blackListRequest) {
        log.info("[BLACKLIST] Deleting a customer from blacklist...");
        BlackListCstmr customer = blackListRepository.findByTelephoneNo(blackListRequest.telephoneNo()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found.")
        );
        blackListRepository.delete(customer);
    }

    @Transactional(readOnly = true)
    public List<BlackListResponse> getAll() {
        List<BlackListCstmr> bannedCstmrs =  blackListRepository.findAll();
        return bannedCstmrs.stream().map(cstmr -> {
            return new BlackListResponse(
                    cstmr.getCustomer().getTelephoneNo(),
                    cstmr.getId()
            );
        }).toList();
    }
}
