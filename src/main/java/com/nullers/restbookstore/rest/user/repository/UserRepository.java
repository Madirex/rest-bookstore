package com.nullers.restbookstore.rest.user.repository;

import com.nullers.restbookstore.rest.user.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity
 *
 * @Author Binwei Wang
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    /**
     * Find user by username
     *
     * @param username username
     * @return user
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     *
     * @param email email
     * @return user
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username
     *
     * @param username username
     * @return user
     */
    Optional<User> findByUsernameEqualsIgnoreCase(String username);

    /**
     * Find user by username or email
     *
     * @param username username
     * @param email    email
     * @return user
     */
    Optional<User> findByUsernameEqualsIgnoreCaseOrEmailEqualsIgnoreCase(String username, String email);

    /**
     * Find all users by username
     *
     * @param username username
     * @return list of users
     */
    List<User> findAllByUsernameContainingIgnoreCase(String username);

    /**
     * Update isDeleted to true by ID
     *
     * @param id ID
     */
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.isDeleted = true WHERE u.id = :id")
    void updateIsDeletedToTrueById(UUID id);
}
