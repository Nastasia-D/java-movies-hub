package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;
    private final int port;
    private final MoviesStore store;

    public MoviesServer(MoviesStore store, int port) {
        this.port = port;
        this.store = store;
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
<<<<<<< HEAD
            server.createContext("/movies", new MoviesHandler(store));
=======
            server.createContext( "/movies", new MoviesHandler(store));
>>>>>>> 4684ffd6f5658968c85a3a697461fd131a9025fa

        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

<<<<<<< HEAD
    public void start() {
=======
    public void start(){
>>>>>>> 4684ffd6f5658968c85a3a697461fd131a9025fa
        server.start();
        System.out.println("Сервер запущен");
    }

    public void stop() {
        // остановите сервер
        server.stop(0);
        System.out.println("Сервер остановлен");
    }
}