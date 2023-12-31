package com.nullers.restbookstore.rest.book.controllers;


import com.nullers.restbookstore.rest.book.dto.CreateBookDTO;
import com.nullers.restbookstore.rest.book.dto.GetBookDTO;
import com.nullers.restbookstore.rest.book.dto.PatchBookDTO;
import com.nullers.restbookstore.rest.book.dto.UpdateBookDTO;
import com.nullers.restbookstore.rest.book.exceptions.BookNotFoundException;
import com.nullers.restbookstore.rest.book.exceptions.BookNotValidIDException;
import com.nullers.restbookstore.rest.publisher.exceptions.PublisherIDNotValid;
import com.nullers.restbookstore.rest.publisher.exceptions.PublisherNotFound;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Interface BookRestController
 *
 * @Author Madirex
 */
public interface BookRestController {

    ResponseEntity<GetBookDTO> getBookById(@Valid @PathVariable Long id)
            throws BookNotValidIDException, BookNotFoundException;

    ResponseEntity<GetBookDTO> postBook(@Valid @RequestBody CreateBookDTO book)
            throws PublisherNotFound, PublisherIDNotValid;

    ResponseEntity<GetBookDTO> putBook(@Valid @PathVariable Long id, @Valid @RequestBody UpdateBookDTO book)
            throws BookNotValidIDException, BookNotFoundException, PublisherNotFound, PublisherIDNotValid;

    ResponseEntity<GetBookDTO> patchBook(@Valid @PathVariable Long id, @Valid @RequestBody PatchBookDTO book)
            throws BookNotValidIDException, BookNotFoundException, PublisherNotFound, PublisherIDNotValid;

    ResponseEntity<String> deleteBook(@Valid @PathVariable Long id) throws BookNotValidIDException, BookNotFoundException;

}
