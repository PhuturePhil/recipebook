package com.recipebook.service;

import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** Unsplash und Pexels gegen einen lokalen HTTP-Server mit aufgezeichneten Antworten. */
class StockPhotoSearchTest {

    private HttpServer server;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final List<String> authHeaders = new CopyOnWriteArrayList<>();
    private volatile int status = 200;
    private volatile String body = "{}";

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            requests.add(exchange.getRequestURI().toString());
            authHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private UnsplashService unsplash() {
        UnsplashService unsplash = new UnsplashService(new ObjectMapper(), 5, baseUrl());
        unsplash.setApiKey("u-key");
        return unsplash;
    }

    @Test
    void unsplashAsksForFiveLandscapePhotosAndParsesDescriptions() {
        body = """
            {"results":[
              {"alt_description":"a bowl of dal","description":"Dal with rice",
               "urls":{"regular":"https://images.unsplash.com/photo-1?w=1080"},
               "links":{"download_location":"https://api.unsplash.com/photos/1/download"}},
              {"alt_description":null,"description":"Lentils",
               "urls":{"regular":"https://images.unsplash.com/photo-2?w=1080"},"links":{}},
              {"alt_description":"no url","urls":{}}
            ]}""";

        List<StockPhoto> photos = unsplash().search("red lentil dal");

        assertEquals(2, photos.size());
        assertEquals(new StockPhoto("https://images.unsplash.com/photo-1?w=1080", "a bowl of dal | Dal with rice",
            "https://api.unsplash.com/photos/1/download"), photos.get(0));
        assertEquals("Lentils", photos.get(1).description());
        assertNull(photos.get(1).downloadLocation());
        String request = requests.get(0);
        assertTrue(request.startsWith("/search/photos?"), request);
        assertTrue(request.contains("query=red%20lentil%20dal"), request);
        assertTrue(request.contains("per_page=5"), request);
        assertTrue(request.contains("orientation=landscape"), request);
        assertEquals("Client-ID u-key", authHeaders.get(0));
    }

    @Test
    void unsplashErrorsGiveNoPhotos() {
        status = 403;
        body = "Rate Limit Exceeded";
        assertEquals(List.of(), unsplash().search("dal"));
    }

    @Test
    void unsplashWithoutKeyDoesNotCall() {
        UnsplashService unsplash = new UnsplashService(new ObjectMapper(), 5, baseUrl());
        assertFalse(unsplash.isConfigured());
        assertEquals(List.of(), unsplash.search("dal"));
        assertTrue(requests.isEmpty());
    }

    @Test
    void unsplashDownloadIsTracked() throws InterruptedException {
        unsplash().trackDownload(new StockPhoto("https://img", "", baseUrl() + "/photos/abc/download?ixid=1"));
        for (int i = 0; i < 50 && requests.isEmpty(); i++) Thread.sleep(20);
        assertEquals(List.of("/photos/abc/download?ixid=1"), requests);
        assertEquals("Client-ID u-key", authHeaders.get(0));
    }

    @Test
    void pexelsParsesLargeImageAndAlt() {
        body = """
            {"photos":[{"alt":"Pumpkin risotto in a bowl","src":{"large2x":"https://images.pexels.com/p/1.jpeg?w=1880"}}]}""";
        PexelsService pexels = new PexelsService(new ObjectMapper(), "p-key", 5, baseUrl());

        List<StockPhoto> photos = pexels.search("pumpkin risotto");

        assertEquals(List.of(new StockPhoto("https://images.pexels.com/p/1.jpeg?w=1880", "Pumpkin risotto in a bowl", null)), photos);
        assertTrue(requests.get(0).startsWith("/v1/search?query=pumpkin%20risotto&per_page=5&orientation=landscape"), requests.get(0));
        assertEquals("p-key", authHeaders.get(0));
    }

    @Test
    void pexelsWithoutKeyIsSkipped() {
        PexelsService pexels = new PexelsService(new ObjectMapper(), "", 5, baseUrl());
        assertFalse(pexels.isConfigured());
        assertEquals(List.of(), pexels.search("dal"));
        assertTrue(requests.isEmpty());
    }
}
