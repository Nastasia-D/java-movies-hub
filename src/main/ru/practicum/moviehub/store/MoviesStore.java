package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();
    private int idCounter = 0;

    public Movie save(Movie movie) {
        idCounter++;
        movie.setId(idCounter);
        movies.put(idCounter, movie);
        return movie;
    }

    public List<Movie> findAll() {
        return new ArrayList<>(movies.values());
    }

    public Optional<Movie> findById(int id) {
        return Optional.ofNullable(movies.get(id));
    }

    public boolean deleteById(int id) {
        return movies.remove(id) != null;
    }

    public void clear() {
        movies.clear();
        idCounter = 1;
    }

    public Optional<Movie> findById(Integer id) {
        return Optional.ofNullable(movies.get(id));
    }

    public List<Movie> findByYear(int year) {
        return findAll().stream()
                .filter(movie -> movie.getYear() == year)
                .collect(Collectors.toList());
    }
}