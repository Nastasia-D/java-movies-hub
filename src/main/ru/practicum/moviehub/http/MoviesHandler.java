package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;
    private final Gson gson = new GsonBuilder().serializeNulls().create();
    Type listType = new TypeToken<List<Movie>>() {}.getType();

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {
            String path = ex.getRequestURI().getPath();
            String[] parts = path.split("/");

            if (parts.length == 2) {
                String query = ex.getRequestURI().getQuery();

                if (query != null && query.startsWith("year=")) {
                    try {
                        int year = Integer.parseInt(query.substring(5));
                        List<Movie> filtered = store.findAll().stream()
                                .filter(movie -> movie.getYear() == year)
                                .collect(Collectors.toList());
                        sendJson(ex, 200, gson.toJson(filtered, listType));
                    } catch (NumberFormatException e) {
                        sendJson(ex, 400, "{\"error\": \"Год должен быть числом\"}");
                    }
                } else {
                    List<Movie> allMovies = store.findAll();
                    sendJson(ex, 200, gson.toJson(allMovies));
                }
                return;
            }
            if (parts.length == 3) {
                String idPart = parts[2];
                try {
                    int id = Integer.parseInt(idPart);
                    Optional<Movie> movieOpt = store.findById(id);
                    if (movieOpt.isPresent()) {
                        sendJson(ex, 200, gson.toJson(movieOpt.get()));
                    } else {
                        sendJson(ex, 404, "{\"error\":\"Фильм не найден\"}");
                    }
                } catch (NumberFormatException e) {
                    sendJson(ex, 400, "{\"error\":\"ID должен быть числом\"}");
                }
                return;
            }
            sendJson(ex, 404, "{\"error\":\"Путь не найден\"}");
        } else if (method.equalsIgnoreCase("POST")) {

            String contentType = ex.getRequestHeaders().getFirst("Content-Type");
            if (!"application/json".equals(contentType)) {
                sendJson(ex, 415, "{\"error\":\"Ожидался application/json\"}");
                return;
            }

            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            int currentYear = LocalDate.now().getYear();
            List<String> errors = new ArrayList<>();

            Movie movie;
            try {
                movie = gson.fromJson(body, Movie.class);
            } catch (JsonSyntaxException e) {
                sendJson(ex, 400, "{\"error\":\"Некорректный JSON\"}");
                return;
            }
            if (movie == null) {
                sendJson(ex, 400, "{\"error\":\"Некорректный JSON\"}");
                return;
            }

            if (movie.getTitle() == null || movie.getTitle().isBlank()) {
                errors.add("Название не должно быть пустым");
            }

            if (movie.getYear() < 1888 || movie.getYear() > (currentYear + 1)) {
                errors.add("Год должен быть между 1888 и " + (currentYear + 1));
            }

            if (movie.getTitle() != null && movie.getTitle().length() > 100) {
                errors.add("Название не должно превышать 100 символов");
            }

            if (!errors.isEmpty()) {
                ErrorResponse response = new ErrorResponse("Ошибка валидации", errors);
                sendJson(ex, 422, gson.toJson(response));
                return;
            }

            Movie saveMovie = store.save(movie);
            String jsonResponse = gson.toJson(saveMovie);
            sendJson(ex, 201, jsonResponse);
        } else if (method.equalsIgnoreCase("DELETE")) {
            String path = ex.getRequestURI().getPath();
            String[] parts = path.split("/");

            if (parts.length == 3) {
                String idPart = parts[2];
                try {
                    int id = Integer.parseInt(idPart);
                    Optional<Movie> movieOpt = store.findById(id);
                    if (movieOpt.isPresent()) {
                        store.deleteById(id);
                        sendNoContent(ex);
                    } else {
                        sendJson(ex, 404, "{\"error\":\"Фильм не найден\"}");
                    }
                } catch (NumberFormatException e) {
                    sendJson(ex, 400, "{\"error\":\"ID должен быть числом\"}");
                }
                return;
            }
            sendJson(ex, 404, "{\"error\":\"Путь не найден\"}");

        } else {
            ex.sendResponseHeaders(405, -1);
            ex.close();
        }
    }

}
