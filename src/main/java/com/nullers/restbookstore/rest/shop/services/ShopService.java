package com.nullers.restbookstore.rest.shop.services;

import com.nullers.restbookstore.rest.shop.dto.CreateShopDto;
import com.nullers.restbookstore.rest.shop.dto.GetShopDto;
import com.nullers.restbookstore.rest.shop.dto.UpdateShopDto;
import com.nullers.restbookstore.rest.shop.exceptions.ShopNotFoundException;

import java.util.UUID;

/**
 * Interfaz ShopService
 *
 * @author alexdor00
 */
public interface ShopService {

    /**
     * Obtiene los detalles de una tienda específica por su identificador.
     *
     * @param id El identificador de la tienda.
     * @return GetShopDto con los detalles de la tienda.
     * @throws ShopNotFoundException Si la tienda no se encuentra.
     */
    GetShopDto getShopById(UUID id) throws ShopNotFoundException;

    /**
     * Crea una nueva tienda basada en los datos proporcionados.
     *
     * @param shopDto DTO con la información para crear la tienda.
     * @return GetShopDto con los detalles de la tienda creada.
     */
    GetShopDto createShop(CreateShopDto shopDto);

    /**
     * Actualiza una tienda existente con los datos proporcionados.
     *
     * @param id      El identificador de la tienda a actualizar.
     * @param shopDto DTO con los datos actualizados para la tienda.
     * @return GetShopDto con los detalles de la tienda actualizada.
     * @throws ShopNotFoundException Si la tienda no se encuentra.
     */
    GetShopDto updateShop(UUID id, UpdateShopDto shopDto);

    /**
     * Elimina una tienda por su identificador.
     *
     * @param id El identificador de la tienda a eliminar.
     * @throws ShopNotFoundException Si la tienda no se encuentra.
     */
    void deleteShop(UUID id) throws ShopNotFoundException;

    /**
     * Añade un libro a la tienda
     *
     * @param id     Shop id
     * @param bookId Book id
     * @return GetShopDto
     */
    GetShopDto addBookToShop(UUID id, Long bookId);

    /**
     * Elimina un libro de la tienda
     *
     * @param id     Shop id
     * @param bookId Book id
     * @return GetShopDto
     */
    GetShopDto removeBookFromShop(UUID id, Long bookId);

    /**
     * Añade un cliente a la tienda
     *
     * @param id       Shop id
     * @param clientId Client id
     * @return GetShopDto
     */
    GetShopDto addClientToShop(UUID id, UUID clientId);

    /**
     * Elimina un cliente de la tienda
     *
     * @param id       Shop id
     * @param clientId Client id
     * @return GetShopDto
     */
    GetShopDto removeClientFromShop(UUID id, UUID clientId);

}
