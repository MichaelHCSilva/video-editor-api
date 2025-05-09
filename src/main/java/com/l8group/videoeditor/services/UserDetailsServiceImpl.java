package com.l8group.videoeditor.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.l8group.videoeditor.models.UserAccount;
import com.l8group.videoeditor.repositories.UserRepository;

import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o nome: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUserName(),
                user.getPassword(),
                Collections.emptyList() 
        );
    }
}