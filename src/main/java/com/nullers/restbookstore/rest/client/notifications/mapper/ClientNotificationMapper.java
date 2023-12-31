package com.nullers.restbookstore.rest.client.notifications.mapper;

import com.nullers.restbookstore.rest.client.model.Client;
import com.nullers.restbookstore.rest.client.notifications.dto.ClientNotificationResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper para ClientNotificationResponse
 *
 * @author Daniel
 * @see ClientNotificationResponse
 * @see Client
 */
@Component
public class ClientNotificationMapper {

    /**
     * Mapea un objeto Client a un objeto ClientNotificationResponse
     *
     * @param client Objeto Client
     * @return ClientNotificationResponse objeto
     */
    public ClientNotificationResponse toClientNotificationResponse(Client client) {
        return new ClientNotificationResponse(
                client.getId(),
                client.getName(),
                client.getSurname(),
                client.getEmail(),
                client.getPhone(),
                client.getAddress(),
                client.getImage()
        );
    }
}
