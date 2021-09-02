package org.webserver.server;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOException;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.webserver.endpoints.ENDPOINT.APP_TXTS;


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
        HttpHandler httpHandler =
                exchange -> {
                    try {
                        String requestMethod = exchange.getRequestMethod();
                        String requestedUri = exchange.getRequestURI().toString();

                        Pattern pattern = Pattern.compile("(?<=\\/txts\\/)(\\d+)");
                        Matcher matcher = pattern.matcher(requestedUri);


                        logger.info("Processing: {} request to {}", requestMethod, requestedUri);
                        String requestBody = readToString(exchange.getRequestBody());

                        if (requestMethod.equals("POST")) {
                            if (matcher.find()) {
                                updateData(exchange, requestBody, matcher.group(1));
                            } else {
                                saveData(exchange, requestBody);
                            }
                        } else if (requestMethod.equals("GET")) {
                            if (matcher.find()) {
                                getDataByKey(exchange, matcher.group(1));
                            } else {
                                getData(exchange);
                            }
                        } else if (requestMethod.equals("DELETE")) {
                            if (matcher.find()) {
                                deleteDataByKey(exchange, matcher.group(1));
                            }
                        } else {
                            String notImplemented = "ERROR.Method not impeleneted";
                            serverResponse(exchange, 500, notImplemented);
                        }
                    } catch (Exception e) {
                        logger.error("Error during handle request {}", e.toString());
                    }
                };

        logger.info("Handle request for endpoint {}", APP_TXTS.getURL());
        return httpServer.createContext(APP_TXTS.getURL(), httpHandler);
    }


    private void getDataByKey(HttpExchange exchange, String key) throws IOException {
        serverResponse(exchange, 200, String.format("Data by key %s is %s", key, data.get(key)));
    }

    private void getData(HttpExchange exchange) throws IOException {
        StringBuilder response = new StringBuilder();
        data.forEach((key, value) -> {
            response.append(key + "=" + value + "\n");
        });

        byte[] respByte = response.toString().getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(200, response.length());
        exchange.getResponseBody().write(respByte);
        exchange.close();
    }

    private void deleteDataByKey(HttpExchange exchange, String key) throws IOException {
        data.remove(key);
        serverResponse(exchange, 200, String.format("Data was deleted on key %s", key));
    }

    private void serverResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
        logger.info("Response: {}", response);
        exchange.close();
    }

    private void saveData(HttpExchange exchange, String requestBody) throws IOException {
        logger.info("Body from request {}", requestBody);
        Optional<String> key = keys(data, requestBody).findFirst();
        if (key.isPresent()) {
            String response = "The data is already in the map. The key is " + key.get();
            serverResponse(exchange, 200, response);
        } else if (requestBody != null && !requestBody.trim().isEmpty()) {
            // Can be changed to UUID.randomUUID().toString();
            String uuid = Integer.toString(id);
            id++;
            data.put(uuid, requestBody);
            logger.info("Saved data {}\nWith key {}", requestBody, uuid);
            String dataWasSavedResponse = "Data was saved with key " + uuid;
            serverResponse(exchange, 201, dataWasSavedResponse);
        } else {
            String errorEmptyBody = "ERROR. Was received empty body";
            serverResponse(exchange, 400, errorEmptyBody);
        }
    }

    private void updateData(HttpExchange exchange, String requestBody, String key) throws IOException {
        if (data.containsKey(key)) {
            data.put(key, requestBody);
            serverResponse(exchange, 200, String.format("Data was updated for %s with data %s", key, requestBody));
        } else {
            serverResponse(exchange, 400, String.format("Invalid request for %s", key));
        }
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
