package com.ivan.Flowers.Shop.services.impls;

import com.ivan.Flowers.Shop.enums.RoleType;
import com.ivan.Flowers.Shop.models.dtos.UserLoginDTO;
import com.ivan.Flowers.Shop.models.dtos.UserRegisterDTO;
import com.ivan.Flowers.Shop.models.entities.Cart;
import com.ivan.Flowers.Shop.models.entities.Role;
import com.ivan.Flowers.Shop.models.entities.User;
import com.ivan.Flowers.Shop.repositories.CartRepository;
import com.ivan.Flowers.Shop.repositories.RoleRepository;
import com.ivan.Flowers.Shop.repositories.UserRepository;
import com.ivan.Flowers.Shop.services.UserService;
import com.ivan.Flowers.Shop.session.LoggedUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoggedUser loggedUser;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, CartRepository cartRepository, PasswordEncoder passwordEncoder, LoggedUser loggedUser) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.cartRepository = cartRepository;
        this.passwordEncoder = passwordEncoder;
        this.loggedUser = loggedUser;
    }

    @Override
    public boolean register(UserRegisterDTO userRegisterDTO) {

        Optional<User> optionalUser = userRepository.findByUsername(userRegisterDTO.getUsername());

        if (optionalUser.isPresent()) {
            return false;
        }

        if (!userRegisterDTO.getPassword().equals(userRegisterDTO.getConfirmPassword())) {
            return false;
        }

        User user = new User();
        user.setOrders(new ArrayList<>());

        Cart cart = new Cart();
        cart.setBouquets(new ArrayList<>());

        cartRepository.saveAndFlush(cart);
        user.setCart(cart);



        user.setUsername(userRegisterDTO.getUsername());
        user.setEmail(userRegisterDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userRegisterDTO.getPassword()));

        Role role = roleRepository.findByType(RoleType.USER);

        if (userRepository.count() == 0) {
            role = roleRepository.findByType(RoleType.ADMIN);
        }

        user.setRole(role);

        userRepository.saveAndFlush(user);

        return true;
    }

    @Override
    public boolean login(UserLoginDTO userLoginDTO) {

        Optional<User> optionalUser = userRepository.findByUsername(userLoginDTO.getUsername());

        if (optionalUser.isEmpty()) {
            return false;
        }

        boolean isMatches = passwordEncoder.matches(userLoginDTO.getPassword(), optionalUser.get().getPassword());

        if (!isMatches) {
            return false;
        }

        loggedUser.setUsername(userLoginDTO.getUsername());
        loggedUser.setId(optionalUser.get().getId());
        loggedUser.setLogged(true);

        return true;
    }
}
