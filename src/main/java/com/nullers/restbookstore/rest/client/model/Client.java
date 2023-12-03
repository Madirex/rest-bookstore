package com.nullers.restbookstore.rest.client.model;

import com.nullers.restbookstore.rest.book.model.Book;
import com.nullers.restbookstore.rest.common.Address;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Daniel
 * @see Book
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clients")
@ToString
public class Client {
    public static final String DEFAULT_IMAGE = "https://via.placeholder.com/150";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(columnDefinition = "TEXT default '" + DEFAULT_IMAGE + "'")
    @Builder.Default
    private String image = DEFAULT_IMAGE;

    @CreatedDate
    private LocalDateTime createdAt;

    @NotNull(message = "La dirección no puede estar vacía")
    @Embedded
    private Address address;
}
