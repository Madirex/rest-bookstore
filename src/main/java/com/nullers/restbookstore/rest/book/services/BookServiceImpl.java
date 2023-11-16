package com.nullers.restbookstore.rest.book.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nullers.restbookstore.config.websockets.WebSocketConfig;
import com.nullers.restbookstore.config.websockets.WebSocketHandler;
import com.nullers.restbookstore.notifications.models.Notification;
import com.nullers.restbookstore.rest.book.dto.CreateBookDTO;
import com.nullers.restbookstore.rest.book.dto.GetBookDTO;
import com.nullers.restbookstore.rest.book.dto.PatchBookDTO;
import com.nullers.restbookstore.rest.book.dto.UpdateBookDTO;
import com.nullers.restbookstore.rest.book.exceptions.BookNotFoundException;
import com.nullers.restbookstore.rest.book.exceptions.BookNotValidIDException;
import com.nullers.restbookstore.rest.book.mappers.BookMapperImpl;
import com.nullers.restbookstore.rest.book.mappers.BookNotificationMapper;
import com.nullers.restbookstore.rest.book.models.Book;
import com.nullers.restbookstore.rest.book.notifications.BookNotificationResponse;
import com.nullers.restbookstore.rest.book.repositories.BookRepository;
import com.nullers.restbookstore.rest.category.exceptions.CategoriaNotFoundException;
import com.nullers.restbookstore.rest.category.models.Categoria;
import com.nullers.restbookstore.rest.category.repository.CategoriasRepositoryJpa;
import com.nullers.restbookstore.rest.publisher.exceptions.PublisherIDNotValid;
import com.nullers.restbookstore.rest.publisher.exceptions.PublisherNotFound;
import com.nullers.restbookstore.rest.publisher.mappers.PublisherMapper;
import com.nullers.restbookstore.rest.publisher.services.PublisherService;
import com.nullers.restbookstore.storage.service.StorageService;
import com.nullers.restbookstore.utils.Util;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Clase BookServiceImpl
 *
 * @Author Madirex
 */
@Slf4j
@Service
@CacheConfig(cacheNames = "books")
public class BookServiceImpl implements BookService {

    public static final String BOOK_NOT_FOUND_MSG = "No se ha encontrado el Book con el ID indicado";
    public static final String NOT_VALID_FORMAT_ID_MSG = "El ID no tiene un formato válido";

    private final BookRepository bookRepository;
    private final BookMapperImpl bookMapperImpl;
    private final PublisherMapper publisherMapper;
    private final WebSocketConfig webSocketConfig;

    @Setter
    private WebSocketHandler webSocketService;
    private final StorageService storageService;
    private final PublisherService publisherService;
    private final ObjectMapper mapper;
    private final BookNotificationMapper bookNotificationMapper;
    private final CategoriasRepositoryJpa categoriasRepositoryJpa;


    /**
     * Constructor BookServiceImpl
     *
     * @param bookRepository         BookRepositoryImpl
     * @param bookMapperImpl         BookMapper
     * @param publisherMapper        PublisherMapper
     * @param webSocketConfig        WebSocketConfig
     * @param storageService         StorageService
     * @param publisherService       PublisherService
     * @param bookNotificationMapper BookNotificationMapper
     */
    @Autowired
    public BookServiceImpl(BookRepository bookRepository, BookMapperImpl bookMapperImpl,
                           PublisherMapper publisherMapper, WebSocketConfig webSocketConfig, StorageService storageService,
                           PublisherService publisherService, BookNotificationMapper bookNotificationMapper, CategoriasRepositoryJpa categoriasRepositoryJpa) {
        this.bookRepository = bookRepository;
        this.bookMapperImpl = bookMapperImpl;
        this.publisherMapper = publisherMapper;
        this.webSocketConfig = webSocketConfig;
        this.webSocketService = webSocketConfig.webSocketHandler();
        this.storageService = storageService;
        this.publisherService = publisherService;
        this.bookNotificationMapper = bookNotificationMapper;
        this.mapper = new ObjectMapper();
        this.categoriasRepositoryJpa = categoriasRepositoryJpa;
    }

    /**
     * Obtiene todos los Books
     *
     * @param publisher Publisher por la que filtrar
     * @param maxPrice  Precio máximo por el que filtrar
     * @param pageable  Paginación
     * @return Lista de Books
     */
    @Cacheable
    @Override
    public Page<GetBookDTO> getAllBook(Optional<String> publisher, Optional<Double> maxPrice, Optional<String> category, PageRequest pageable) {
        Specification<Book> specType = (root, query, criteriaBuilder) ->
                publisher.map(m -> {
                    try {
                        return criteriaBuilder.equal(criteriaBuilder.upper(root.get("publisher").get("name")),
                                m.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return criteriaBuilder.isTrue(criteriaBuilder.literal(false));
                    }
                }).orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true)));

        Specification<Book> specMaxPrice = (root, query, criteriaBuilder) ->
                maxPrice.map(p -> criteriaBuilder.lessThanOrEqualTo(root.get("price"), p))
                        .orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true)));

        Specification<Book> specCategory = (root, query, criteriaBuilder) -> category.map(c -> {

            return criteriaBuilder.equal(criteriaBuilder.upper(root.get("category").get("nombre")),
                    c.toUpperCase());

        }).orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true)));

        System.out.println(specCategory);

        Specification<Book> criterion = Specification.where(specType)
                .and(specMaxPrice)
                .and(specCategory);

        Page<Book> bookPage = bookRepository.findAll(criterion, pageable);
        List<GetBookDTO> dtoList = bookPage.getContent().stream()
                .map(e -> bookMapperImpl.toGetBookDTO(e, publisherMapper.toPublisherData(e.getPublisher())))
                .toList();

        return new PageImpl<>(dtoList, bookPage.getPageable(), bookPage.getTotalElements());
    }

    /**
     * Obtiene un Book por su ID
     *
     * @param id ID del Book a obtener
     * @return Book con el ID indicado
     * @throws BookNotValidIDException Si el ID no tiene un formato válido
     * @throws BookNotFoundException   Si no se ha encontrado el Book con el ID indicado
     */
    @Cacheable(key = "#result.id")
    @Override
    public GetBookDTO getBookById(Long id) throws BookNotValidIDException, BookNotFoundException {
        try {
            var f = bookRepository.findById(id).orElseThrow(() ->
                    new BookNotFoundException(BOOK_NOT_FOUND_MSG));
            return bookMapperImpl.toGetBookDTO(f, publisherMapper.toPublisherData(f.getPublisher()));
        } catch (IllegalArgumentException e) {
            throw new BookNotValidIDException(NOT_VALID_FORMAT_ID_MSG);
        }
    }

    /**
     * Crea un Book
     *
     * @param book CreateBookDTO con los datos del Book a crear
     * @return Book creado
     * @throws PublisherNotFound   Si no se ha encontrado la publisher con el ID indicado
     * @throws PublisherIDNotValid Si el ID no tiene un formato válido
     */
    @CachePut(key = "#result.id")
    @Override
    public GetBookDTO postBook(CreateBookDTO book) throws PublisherNotFound, PublisherIDNotValid {
        var publisher = publisherMapper.toPublisher(publisherService.findById(book.getPublisherId()));
        var category = checkCategory(book.getCategory());
        var f = bookRepository.save(bookMapperImpl.toBook(book, publisher, category));
        var bookDTO = bookMapperImpl.toGetBookDTO(f, publisherMapper.toPublisherData(f.getPublisher()));
        onChange(Notification.Type.CREATE, bookDTO);
        return bookDTO;
    }

    /**
     * Actualiza un Book
     *
     * @param id   Id del Book a actualizar
     * @param book UpdateBookDTO con los datos a actualizar
     * @return Book actualizado
     * @throws BookNotValidIDException Si el ID no tiene un formato válido
     * @throws PublisherNotFound       Si no se ha encontrado el publisher con el ID indicado
     * @throws PublisherIDNotValid     Si el ID no tiene un formato válido
     * @throws BookNotFoundException   Si no se ha encontrado el Book con el ID indicado
     */
    @CachePut(key = "#result.id")
    @Override
    public GetBookDTO putBook(Long id, UpdateBookDTO book) throws BookNotValidIDException,
            PublisherNotFound, PublisherIDNotValid, BookNotFoundException {
        try {
            Book existingBook = bookRepository.findById(id)
                    .orElseThrow(() -> new BookNotFoundException("Book no encontrado"));
            Categoria category = checkCategory(book.getCategory());
            var publisher = publisherMapper.toPublisher(publisherService.findById(book.getPublisherId()));
            Book f = bookMapperImpl.toBook(existingBook, book, publisher, category);
            f.setId(id);
            var modified = bookRepository.save(f);
            var bookDTO = bookMapperImpl.toGetBookDTO(modified, publisherMapper.toPublisherData(modified.getPublisher()));
            onChange(Notification.Type.UPDATE, bookDTO);
            return bookDTO;
        } catch (IllegalArgumentException e) {
            throw new BookNotValidIDException(NOT_VALID_FORMAT_ID_MSG);
        }
    }

    /**
     * Actualiza un Book
     *
     * @param id   Id del Book a actualizar
     * @param book Book con los datos a actualizar
     * @return Book actualizado
     * @throws BookNotValidIDException Si el ID no tiene un formato válido
     * @throws BookNotFoundException   Si no se ha encontrado el Book con el ID indicado
     * @throws PublisherNotFound       Si no se ha encontrado la publisher con el ID indicado
     * @throws PublisherIDNotValid     Si el ID no tiene un formato válido
     */
    @CachePut(key = "#result.id")
    @Override
    public GetBookDTO patchBook(Long id, PatchBookDTO book) throws BookNotValidIDException, BookNotFoundException,
            PublisherNotFound, PublisherIDNotValid {
        try {
            var opt = bookRepository.findById(id);
            if (opt.isEmpty()) {
                throw new BookNotFoundException(BOOK_NOT_FOUND_MSG);
            }
            BeanUtils.copyProperties(book, opt.get(), Util.getNullPropertyNames(book));
            opt.get().setId(id);
            opt.get().setUpdatedAt(LocalDateTime.now());
            opt.get().setPublisher(publisherMapper
                    .toPublisher(publisherService.findById(book.getPublisherId())));
            Book modified = bookRepository.save(opt.get());
            var bookDTO = bookMapperImpl.toGetBookDTO(modified, publisherMapper.toPublisherData(modified.getPublisher()));
            onChange(Notification.Type.UPDATE, bookDTO);
            return bookDTO;
        } catch (IllegalArgumentException e) {
            throw new BookNotValidIDException(NOT_VALID_FORMAT_ID_MSG);
        }
    }

    /**
     * Elimina un Book
     *
     * @param id ID del Book a eliminar
     * @throws BookNotFoundException   Si no se ha encontrado el Book con el ID indicado
     * @throws BookNotValidIDException Si el ID no tiene un formato válido
     */
    @CacheEvict(key = "#id")
    @Override
    public void deleteBook(Long id) throws BookNotFoundException, BookNotValidIDException {
        try {
            var opt = bookRepository.findById(id);
            if (opt.isEmpty()) {
                throw new BookNotFoundException(BOOK_NOT_FOUND_MSG);
            }
            patchBook(id, PatchBookDTO.builder().active(false).build());
            var result = opt.get();
            onChange(Notification.Type.DELETE, bookMapperImpl.toGetBookDTO(result,
                    publisherMapper.toPublisherData(result.getPublisher())));
        } catch (IllegalArgumentException e) {
            throw new BookNotValidIDException(NOT_VALID_FORMAT_ID_MSG);
        }
    }

    /**
     * Actualiza la imagen de un Book
     *
     * @param id      Id del Book a actualizar
     * @param image   Imagen a actualizar
     * @param withUrl Si se quiere devolver la URL de la imagen
     * @return Book actualizado
     * @throws BookNotFoundException   Si no se ha encontrado el Book con el ID indicado
     * @throws BookNotValidIDException Si el ID no tiene un formato válido
     * @throws PublisherNotFound       Si no se ha encontrado la publisher con el ID indicado
     * @throws PublisherIDNotValid     Si el ID no tiene un formato válido
     * @throws IOException             Si se produce un error al guardar la imagen
     */
    @Override
    @CachePut(key = "#result.id")
    @Transactional
    public GetBookDTO updateImage(Long id, MultipartFile image, Boolean withUrl) throws BookNotFoundException,
            BookNotValidIDException, PublisherNotFound, PublisherIDNotValid, IOException {
        try {
            var actualBook = bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(String.valueOf(id)));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSSSSS");
            String imageStored = storageService.store(image, List.of("jpg", "jpeg", "png"), id
                    + "-" + LocalDateTime.now().format(formatter));
            String imageUrl = Boolean.FALSE.equals(withUrl) ? imageStored : storageService.getUrl(imageStored);
            if (actualBook.getImage() != null && !actualBook.getImage().equals(Book.IMAGE_DEFAULT)) {
                storageService.delete(actualBook.getImage());
            }
            return patchBook(id, PatchBookDTO.builder()
                    .image(imageUrl)
                    .build());
        } catch (IllegalArgumentException e) {
            throw new BookNotValidIDException(NOT_VALID_FORMAT_ID_MSG);
        }
    }

    /**
     * Método para enviar una notificación a los clientes ws
     *
     * @param type Tipo de notificación
     * @param data Datos de la notificación
     */
    public void onChange(Notification.Type type, GetBookDTO data) {
        log.debug("Servicio de productos onChange con tipo: " + type + " y datos: " + data);
        if (webSocketService == null) {
            log.warn("No se ha podido enviar la notificación a los clientes ws, no se ha encontrado el servicio");
            webSocketService = this.webSocketConfig.webSocketHandler();
        }

        try {
            Notification<BookNotificationResponse> notification = new Notification<>(
                    "BOOKS",
                    type,
                    bookNotificationMapper.toBookNotificationDto(data),
                    LocalDateTime.now().toString()
            );

            String json = mapper.writeValueAsString(notification);

            log.info("Enviando mensaje a los clientes ws");
            Thread senderThread = new Thread(() -> {
                try {
                    webSocketService.sendMessage(json);
                } catch (Exception e) {
                    log.error("Error al enviar el mensaje a través del servicio WebSocket", e);
                }
            });
            senderThread.start();
        } catch (JsonProcessingException e) {
            log.error("Error al convertir la notificación a JSON", e);
        }
    }

    public Categoria checkCategory(String category) {
        var res = categoriasRepositoryJpa.findByNombre(category);
        if (res.isEmpty() || !res.get().isActiva()) {
            throw new CategoriaNotFoundException("La categoría no existe o no esta activa");
        }
        return res.get();
    }
}