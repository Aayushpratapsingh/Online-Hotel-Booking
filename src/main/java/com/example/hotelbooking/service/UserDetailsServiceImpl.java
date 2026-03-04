/*
 * package com.example.hotelbooking.service;
 * 
 * import com.example.hotelbooking.model.User; import
 * com.example.hotelbooking.repository.UserRepository; import
 * org.springframework.security.core.userdetails.*; import
 * org.springframework.security.core.authority.SimpleGrantedAuthority; import
 * org.springframework.stereotype.Service; import
 * org.springframework.transaction.annotation.Transactional;
 * 
 * import java.util.stream.Collectors;
 * 
 * @Service
 * 
 * @Transactional public class UserDetailsServiceImpl implements
 * UserDetailsService {
 * 
 * private final UserRepository userRepository;
 * 
 * public UserDetailsServiceImpl(UserRepository userRepository) {
 * this.userRepository = userRepository; }
 * 
 * @Override public UserDetails loadUserByUsername(String username) throws
 * UsernameNotFoundException {
 * 
 * User user = userRepository.findByUsername(username) .orElseThrow(() -> new
 * UsernameNotFoundException("User not found with username: " + username));
 * 
 * // Check if user is enabled if (!user.isEnabled()) { throw new
 * UsernameNotFoundException("User account is disabled: " + username); }
 * 
 * return org.springframework.security.core.userdetails.User
 * .withUsername(user.getUsername()) .password(user.getPassword()) .authorities(
 * user.getRoles().stream() .map(role -> new
 * SimpleGrantedAuthority(role.getName())) .collect(Collectors.toList()) )
 * .accountExpired(false) .accountLocked(false) .credentialsExpired(false)
 * .disabled(false) .build(); } }
 */