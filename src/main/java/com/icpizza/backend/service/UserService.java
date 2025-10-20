package com.icpizza.backend.service;

import com.icpizza.backend.dto.UserTO;
import com.icpizza.backend.mapper.UserMapper;
import com.icpizza.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserTO getUser(Long userId){
        return userMapper.toUserTO(userRepository.findById(userId).get());
    }
}
