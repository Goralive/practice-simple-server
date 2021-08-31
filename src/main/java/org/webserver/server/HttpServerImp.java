package org.webserver.server;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class HttpServerImp implements IServer {
    private final Logger logger = LoggerFactory.getLogger(HttpServerImp.class);
    private HttpServer httpServer;
    private int id = 1;
    Map<String, String> data = new HashMap<>();


    @Override
    public void start(int port) {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            handlePostTxts();
            logger.info("Start Http server on port {}", port);
            httpServer.start();
        } catch (IOException e) {
            logger.error("Http server error during start {}", e.toString());
        }
    }

    @Override
    public void shutDown(int port) {
        httpServer.stop(port);
    }

    private HttpContext handlePostTxts() {
        String url = "/app/txts";
        HttpHandler httpHandler =
                exchange -> {
                    try {
                        logger.info("Processing: {} request to {}", exchange.getRequestMethod(), exchange.getRequestURI());
                        String requestBody = readToString(exchange.getRequestBody());
                        logger.info("Body from request {}", requestBody);

                        Optional<String> key = keys(data, requestBody).findFirst();
                        if (key.isPresent()) {
                            String response = "The data is already in the map. The key is " + key.get();
                            exchange.sendResponseHeaders(200, response.length());
                            exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
                            logger.info("Response: {}", response);
                            exchange.close();
                        }
                        else if (requestBody != null && !requestBody.trim().isEmpty()) {
                            // Can be changed to UUID.randomUUID().toString();
                            String uuid = Integer.toString(id);
                            id++;
                            data.put(uuid, requestBody);
                            logger.info("Saved data {}\nWith key {}", requestBody, uuid);
                            String dataWasSavedResponse = "Data was saved with key " + uuid;
                            exchange.sendResponseHeaders(200, dataWasSavedResponse.length());
                            exchange.getResponseBody().write(dataWasSavedResponse.getBytes(StandardCharsets.UTF_8));
                            exchange.close();
                        } else {
                            String errorEmptyBody = "ERROR. Was received empty body";
                            exchange.sendResponseHeaders(400, errorEmptyBody.length());
                            exchange.getResponseBody().write(errorEmptyBody.getBytes(StandardCharsets.UTF_8));
                            exchange.close();
                        }
                    } catch (Exception e) {
                        logger.error("Error during handle request {}", e.toString());
                    }
                };
        logger.info("Handle request for endpoint {}", url);
        return httpServer.createContext(url, httpHandler);
    }

    private String readToString(InputStream stream) {
        try (InputStreamReader reader = new InputStreamReader(stream);
             BufferedReader br = new BufferedReader(reader)) {

            return br.lines().collect(Collectors.joining("\n"));

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private <K, V> Stream<K> keys(Map<K, V> map, V value) {
        return map
                .entrySet()
                .stream()
                .filter(entry -> value.equals(entry.getValue()))
                .map(Map.Entry::getKey);
    }
}
