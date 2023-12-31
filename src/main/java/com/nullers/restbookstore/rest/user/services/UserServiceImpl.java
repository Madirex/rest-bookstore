package com.nullers.restbookstore.rest.user.services;

import com.nullers.restbookstore.rest.orders.repositories.OrderRepository;
import com.nullers.restbookstore.rest.user.dto.UserInfoResponse;
import com.nullers.restbookstore.rest.user.dto.UserRequest;
import com.nullers.restbookstore.rest.user.dto.UserResponse;
import com.nullers.restbookstore.rest.user.exceptions.UserNameOrEmailExists;
import com.nullers.restbookstore.rest.user.exceptions.UserNotFound;
import com.nullers.restbookstore.rest.user.mapper.UserMapper;
import com.nullers.restbookstore.rest.user.models.User;
import com.nullers.restbookstore.rest.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Servicio para la entidad User
 *
 * @Author Binwei Wang
 */
@Service
@Slf4j
@CacheConfig(cacheNames = {"users"})
public class UserServiceImpl implements UserService {
    public static final String USER_NOT_FOUND_MSG = "Usuario no encontrado";
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncode;

    /**
     * Constructor de la clase
     *
     * @param userRepository  repositorio de usuarios
     * @param orderRepository repositorio Order
     * @param userMapper      mapper de usuarios
     * @param passwordEncode  encoder de contraseñas
     */
    public UserServiceImpl(UserRepository userRepository, OrderRepository orderRepository, UserMapper userMapper, PasswordEncoder passwordEncode) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.userMapper = userMapper;
        this.passwordEncode = passwordEncode;
    }

    /**
     * Busca todos los usuarios
     *
     * @param username  nombre de usuario
     * @param email     email del usuario
     * @param isDeleted si el usuario está borrado
     * @param pageable  paginación
     * @return Page de UserResponse
     */
    @Override
    @Cacheable
    public Page<UserResponse> findAll(Optional<String> username, Optional<String> email, Optional<Boolean> isDeleted, Pageable pageable) {
        log.info("Buscando todos los usuarios con username: " + username + " y borrados: " + isDeleted);
        // Criterio de búsqueda por nombre
        Specification<User> specUsernameUser = (root, query, criteriaBuilder) ->
                username.map(m -> criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), "%" + m.toLowerCase() + "%"))
                        .orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true)));

        // Criterio de búsqueda por email
        Specification<User> specEmailUser = (root, query, criteriaBuilder) ->
                email.map(m -> criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), "%" + m.toLowerCase() + "%"))
                        .orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true)));

        // Criterio de búsqueda por borrado
        Specification<User> specIsDeleted = (root, query, criteriaBuilder) ->
                isDeleted.map(m -> criteriaBuilder.equal(root.get("isDeleted"), m))
                        .orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true)));

        // Combinamos las especificaciones
        Specification<User> criterio = Specification.where(specUsernameUser)
                .and(specEmailUser)
                .and(specIsDeleted);

        // Debe devolver un Page, por eso usamos el findAll de JPA
        return userRepository.findAll(criterio, pageable).map(userMapper::toUserResponse);
    }

    /**
     * Busca un usuario por ID
     *
     * @param id id del usuario
     * @return usuario encontrado
     */

    @Override
    @Cacheable(key = "#id")
    public UserInfoResponse findById(UUID id) {
        log.info("Buscando usuario por id: " + id);
        var user = userRepository.findById(id).orElseThrow(() -> new UserNotFound(USER_NOT_FOUND_MSG));
        var order = orderRepository.findOrderIdsByClientId(id).stream().map(p -> p.getId().toHexString()).toList();
        return userMapper.toUserInfoResponse(user, order);
    }

    /**
     * Guarda un usuario en la base de datos
     *
     * @param userRequest usuario a guardar
     * @return usuario guardado
     */
    @Override
    @Cacheable(key = "#result.id")
    public UserResponse save(UserRequest userRequest) {
        log.info("Guardando usuario: " + userRequest);
        userRepository.findByUsernameEqualsIgnoreCaseOrEmailEqualsIgnoreCase(userRequest.getUsername(),
                        userRequest.getEmail())
                .ifPresent(user -> {
                    throw new UserNameOrEmailExists("El usuario ya existe");
                });
        userRequest.setPassword(passwordEncode.encode(userRequest.getPassword()));
        return userMapper.toUserResponse(userRepository.save(userMapper.toUser(userRequest)));
    }

    /**
     * Actualiza un usuario en la base de datos
     *
     * @param id          id del usuario a actualizar
     * @param userRequest usuario a actualizar
     * @return usuario actualizado
     */
    @Override
    @CachePut(key = "#result.id")
    public UserResponse update(UUID id, UserRequest userRequest) {
        log.info("Actualizando usuario: " + userRequest);
        var actualUser = userRepository.findById(id).orElseThrow(() -> new UserNotFound(USER_NOT_FOUND_MSG));
        if (actualUser != null) {
            userRepository.findByUsernameEqualsIgnoreCaseOrEmailEqualsIgnoreCase(userRequest.getUsername(), userRequest.getEmail())
                    .ifPresent(user -> {
                        throw new UserNameOrEmailExists("El usuario ya existe");
                    });
        }
        userRequest.setPassword(passwordEncode.encode(userRequest.getPassword()));
        return userMapper.toUserResponse(userRepository.save(userMapper.toUser(userRequest, id)));
    }

    /**
     * Actualiza parcialmente un usuario en la base de datos
     *
     * @param id          Id del usuario a actualizar
     * @param userRequest Usuario a actualizar parcialmente
     * @return Usuario actualizado parcialmente
     */
    public UserResponse patch(UUID id, UserRequest userRequest) {
        log.info("Actualizando usuario: " + userRequest);
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            throw new UserNotFound(USER_NOT_FOUND_MSG);
        }
        if (userRequest.getPassword() != null) {
            userRequest.setPassword(passwordEncode.encode(userRequest.getPassword()));
        }
        return userMapper.toUserResponse(userRepository.save(userMapper.toUser(userRequest, id)));
    }

    /**
     * Borra un usuario de la base de datos
     *
     * @param id ID del usuario a borrar
     */
    @Override
    @Transactional
    @CacheEvict(key = "#id")
    public void deleteById(UUID id) {
        log.info("Borrando usuario por id: " + id);
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFound(USER_NOT_FOUND_MSG));
        if (orderRepository.existsByUserId(id)) {
            userRepository.updateIsDeletedToTrueById(id);
        } else {
            userRepository.delete(user);
        }
    }
}
