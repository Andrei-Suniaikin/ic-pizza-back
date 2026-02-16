package com.icpizza.backend.mapper;

import com.icpizza.backend.dto.order.UserTO;
import com.icpizza.backend.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserTO toUserTO(User user){
        return new UserTO(
                user.getId(),
                user.getUserName()
        );
    }
}
