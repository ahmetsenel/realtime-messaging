package com.ahmetsenel.chatservice.security;

import java.security.Principal;

public record UserPrincipal(Long userId, String username) implements Principal {

    @Override
    public String getName() {
        return userId.toString();
    }
}
