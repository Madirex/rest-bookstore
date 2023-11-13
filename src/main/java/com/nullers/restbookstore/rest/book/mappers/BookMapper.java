package com.nullers.restbookstore.rest.book.mappers;

import com.nullers.restbookstore.NOADD.Publisher;
import com.nullers.restbookstore.rest.book.dto.CreateBookDTO;
import com.nullers.restbookstore.rest.book.dto.GetBookDTO;
import com.nullers.restbookstore.rest.book.dto.UpdateBookDTO;
import com.nullers.restbookstore.rest.book.models.Book;

import java.util.List;

/**
 * Clase BookMapper
 *
 * @Author Madirex
 */
public interface BookMapper {
    /**
     * Mapea un CreateBookDTO en Book
     *
     * @param dto       CreateBookDTO a mapear
     * @param publisher Publisher a mapear
     * @return Book mapeado
     */
    Book toBook(CreateBookDTO dto, Publisher publisher);

    /**
     * Mapea un UpdateBookDTO en Book
     *
     * @param book      Book a mapear
     * @param dto       UpdateBookDTO a mapear
     * @param publisher Publisher a mapear
     * @return Book mapeado
     */
    Book toBook(Book book, UpdateBookDTO dto, Publisher publisher);

    /**
     * Mapea un Book en GetBookDTO
     *
     * @param book Book a mapear
     * @return GetBookDTO mapeado
     */
    GetBookDTO toGetBookDTO(Book book);

    /**
     * Mapea una lista de Book en una lista de GetBookDTO
     *
     * @param dto Book a mapear
     * @return GetBookDTO mapeado
     */
    List<GetBookDTO> toBookList(List<Book> dto);
}
