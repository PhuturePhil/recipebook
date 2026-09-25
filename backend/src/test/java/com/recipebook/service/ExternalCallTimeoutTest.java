package com.recipebook.service;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** A local server that accepts connections but never answers stands in for a hanging external API. */
class ExternalCallTimeoutTest {

    private ServerSocket server;
    private final List<Socket> accepted = new ArrayList<>();
    private Thread acceptor;

    @BeforeEach
    void startSilentServer() throws IOException {
        server = new ServerSocket(0);
        acceptor = new Thread(() -> {
            try {
                while (!server.isClosed()) {
                    accepted.add(server.accept());
                }
            } catch (IOException ignored) {
                // server closed
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
    }

    @AfterEach
    void stopServer() throws IOException {
        server.close();
        for (Socket s : accepted) s.close();
    }

    private String baseUrl() {
        return "http://localhost:" + server.getLocalPort();
    }

    @Test
    void unsplashGivesUpAfterTimeoutAndReturnsNoImage() {
        UnsplashService unsplash = new UnsplashService(new ObjectMapper(), 1, baseUrl());
        unsplash.setApiKey("test-key");

        long start = System.nanoTime();
        String url = unsplash.findImageUrl("Linsen-Dal");
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertNull(url);
        assertTrue(millis < 5_000, "took " + millis + " ms");
    }

    @Test
    void recipeScanReportsTimeoutSeparately() {
        RecipeScanService scan = new RecipeScanService(new ObjectMapper(), 1, baseUrl());
        scan.setApiKey("test-key");

        long start = System.nanoTime();
        assertThrows(RecipeScanService.ScanTimeoutException.class,
            () -> scan.scanImages(List.of(Map.of("imageData", "AAAA"))));
        long millis = (System.nanoTime() - start) / 1_000_000;

        assertTrue(millis < 8_000, "took " + millis + " ms");
    }
}
