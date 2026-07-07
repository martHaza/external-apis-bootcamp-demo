package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class BookRestClientImpl implements BookRestClient {

    private RestClient restClient;

    public BookRestClientImpl(RestClient.Builder builder, ExternalServiceProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public BookDto getBook(Long id) {
        try {
            BookApiResponse response = restClient.get()
                    .uri("/books/{id}", id)
                    .retrieve()
                    .body(BookApiResponse.class);

            return toDto(response);
        } catch (HttpClientErrorException e) {
            throw new ClientException("Client error fetching book " + id + ": " + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            throw new ClientException("External service error fetching book " + id + ": " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new ClientException("Failed to connect to book service " + id, e);
        } catch (Exception e) {
            throw new ClientException("Error fetching book " + id, e);
        }
    }

    @Override
    public List<BookDto> getAllBooks() {
        try {
            BookApiResponse[] responses = restClient.get()
                    .uri("/books")
                    .retrieve()
                    .body(BookApiResponse[].class);

            if (responses == null) {
                return List.of();
            }

            return List.of(responses).stream()
                    .map(this::toDto)
                    .toList();

        } catch (HttpClientErrorException e) {
            throw new ClientException("Client error fetching books: " + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            throw new ClientException("External service error fetching books: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new ClientException("Failed to connect to book service ", e);
        } catch (Exception e) {
            throw new ClientException("Error fetching books ", e);
        }
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
