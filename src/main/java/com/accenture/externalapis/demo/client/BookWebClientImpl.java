package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class BookWebClientImpl implements BookWebClient {

    private WebClient webClient;

    public BookWebClientImpl(WebClient.Builder builder, ExternalServiceProperties properties) {
        this.webClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public Mono<BookDto> getBookAsync(Long id) {
        return webClient.get()
                .uri("/books/{id}", id)
                .retrieve()
                .bodyToMono(BookApiResponse.class)
                .switchIfEmpty(Mono.error(new ClientException("No book found for id " + id)))
                .map(this::toDto)
                .onErrorMap(WebClientResponseException.class, e -> new ClientException(
                        "Error response fetching book " + id + ": " + e.getStatusCode(), e))
                .onErrorMap(WebClientRequestException.class, e -> new ClientException(
                        "Failed to connect to book service " + id, e))
                .onErrorMap(Exception.class, e -> new ClientException("Error fetching book " + id, e));
    }

    @Override
    public Flux<BookDto> getAllBooksAsync() {
        return webClient.get()
                .uri("/books")
                .retrieve()
                .bodyToFlux(BookApiResponse.class)
                .map(this::toDto)
                .onErrorMap(WebClientResponseException.class, e -> new ClientException(
                        "Error response fetching books: " + e.getStatusCode(), e))
                .onErrorMap(WebClientRequestException.class, e -> new ClientException(
                        "Failed to connect to book service ", e))
                .onErrorMap(Exception.class, e -> new ClientException("Error fetching books ", e));
    }

    @Override
    public Mono<List<BookDto>> getBooksInParallel(Long id1, Long id2) {
        Mono<BookDto> book1 = getBookAsync(id1);
        Mono<BookDto> book2 = getBookAsync(id2);

        return Mono.zip(book1, book2).map(tuple -> List.of(tuple.getT1(), tuple.getT2()));
    }

    private BookDto toDto(BookApiResponse response) {
        if (response == null) {
            throw new ClientException("External service returned empty response");
        }
        return new BookDto(
                response.title(),
                response.author(),
                response.genre(),
                response.price().doubleValue()
        );
    }
}
