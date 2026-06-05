package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    private static MoviesServer server;
    private static HttpClient client;
    private static final String BASE = "http://localhost:8080";
    private static MoviesStore store;
    private static Movie movie;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        movie = new Movie(null, "Inception", 2010);
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies"))
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenHasMovies_returnsMoviesArray() throws Exception {
        movie = new Movie(null, "Inception", 2010);
        store.save(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies"))
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        List<Movie> movies = new Gson().fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(1, movies.size(), "Список должен содержать один фильм");
        assertEquals("Inception", movies.get(0).getTitle(), "Название фильма должно совпадать");
        assertEquals(2010, movies.get(0).getYear(), "Год фильма должен совпадать");
    }

    @Test
    void getMovie_whenIdExists_returnsMovie200() throws Exception {

        Movie movieToSave = new Movie(null, "Inception", 2010);
        Movie savedMovie = store.save(movieToSave);
        int id = savedMovie.getId();

        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies/" + id))
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertTrue(resp.body().contains("\"id\":" + id), "Ответ должен содержать ID:" + id);
        assertTrue(resp.body().contains("\"title\":\"Inception\""));
    }

    @Test
    void getMovie_whenNotFound_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies/999"))
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(404, resp.statusCode());
    }

    @Test
    void getMovie_whenIdIsNotNumber_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies/abc"))
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(400, resp.statusCode());
    }

    @Test
    void getMoviesByYear_returnsMovies() throws Exception {
        store.save(new Movie(null, "Люди в чёрном", 1997));
        store.save(new Movie(null, "Валериан и город тысячи планет", 2017));
        store.save(new Movie(null, "Собачья жизнь", 2017));
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies?year=2017"))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(200, resp.statusCode());
        List<Movie> movies = new Gson().fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(2, movies.size(), "Должно вернуться 2 фильма за 2017 год");
        assertTrue(movies.stream().allMatch(m -> m.getYear() == 2017));
    }

    @Test
    void getMoviesByYear_returnsEmptyArray() throws Exception {
        store.save(new Movie(null, "Люди в чёрном", 1997));
        store.save(new Movie(null, "Валериан и город тысячи планет", 2017));
        store.save(new Movie(null, "Собачья жизнь", 2017));
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies?year=2010"))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(200, resp.statusCode());
        List<Movie> movies = new Gson().fromJson(resp.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(0, movies.size(), "Должен вернуться пустой список");
        assertTrue(movies.stream().allMatch(m -> m.getYear() == 2010));
    }

    @Test
    void getMoviesByYear_returnsYearNotNumb() throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies?year=abc"))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(400, resp.statusCode(), "При нечисловом годе должен быть 400 Bad Request");
    }

    @Test
    void postMovie_whenTitleIsEmpty_returns422() throws Exception {
        String invalidMovieJson = "{\"title\":\"\",\"year\":2010}";

        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(invalidMovieJson))
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(422, resp.statusCode(), "POST /movies с пустым названием должен вернуть статус 400");
    }

    @Test
    void postMovie_whenYearIsInvalid_returns422() throws Exception {
        String invalidMovieJson = "{\"title\":\"Inception\",\"year\":1800}";

        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(invalidMovieJson))
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(422, resp.statusCode(), "POST /movies с некорректным годом должен вернуть статус 422");
    }

    @Test
    void postMovie_whenMovieIsValid_returns201() throws Exception {
        String validMovieJson = "{\"title\":\"Мой любимый марсианин\",\"year\":1990}";

        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(validMovieJson))
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(201, resp.statusCode(), "При корректных данных должен быть статус 201");
        String body = resp.body();
        assertTrue(body.contains("\"id\":"), "В ответе должен быть ID");
        assertTrue(body.contains("\"title\":\"Мой любимый марсианин\""), "Название фильма должно совпадать");
    }

    @Test
    void postMovie_whenMovieIsValid_returns422() throws Exception {
        String longTitle = "А".repeat(101);
        String json = "{\"title\":\"" + longTitle + "\",\"year\":2020}";

        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(422, resp.statusCode(), "Длинное название должно возвращать 422");
    }

    @Test
    void postMovie_whenJsonIsInvalid_returns400() throws Exception {
        String invalidJson = "{\"title\":\"Фильм\", \"year\":2020";

        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(400, resp.statusCode(), "Некорректный JSON должен возвращать 400");

        assertTrue(resp.body().contains("Некорректный JSON"), "В ответе должно быть описание ошибки");
    }

    @Test
    void postMovie_whenJsonIsInvalid_returns415() throws Exception {
        String invalidJson = "{\"title\":\"Хроники Нарнии\",\"year\":2005}";

        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(415, resp.statusCode(), "Ожидался application/json");

    }

    @Test
    void deleteMovie_whenExists_returns204() throws Exception {
        Movie saved = store.save(new Movie(null, "Убить Билла", 2003));
        int id = saved.getId();

        HttpRequest req = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(BASE + "/movies/" + id))
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(204, resp.statusCode());
        assertTrue(store.findById(id).isEmpty(), "Фильм должен быть удален из store");
    }

    @Test
    void deleteMovie_whenNotFound_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(BASE + "/movies/999"))
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(404, resp.statusCode());
    }

    @Test
    void deleteMovie_whenIdIsNotNumber_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(BASE + "/movies/abc"))
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(400, resp.statusCode());
    }

    @Test
    void unsupportedMethod_returns405() throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .method("PUT", HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(BASE + "/movies"))
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(405, resp.statusCode());
    }

}