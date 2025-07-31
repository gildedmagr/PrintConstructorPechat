package ru.pechat55.constructor.render.rest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.pechat55.constructor.render.App;
import ru.pechat55.constructor.render.Parameters;

import java.io.IOException;
import java.io.OutputStream;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class Endpoint implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(Endpoint.class);

    public App app;
    private boolean logRequest;

    public Endpoint(App app) {
        this.app = app;
        this.logRequest = true;
    }

    public Endpoint doNotLogRequests() {
        this.logRequest = false;
        return this;
    }

    @Override
    public void handle(final HttpExchange exchange) {
        if (logRequest) {
            log.info("Received request {}, {}", exchange.getRequestMethod(), exchange.getRequestURI());
        }
        HttpResponse httpResponse;
        try {
            httpResponse = processRequest(exchange);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            StringBuilder stackTrace = new StringBuilder();
            stackTrace.append(e.getMessage());
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                stackTrace.append(stackTraceElement).append(System.lineSeparator());
            }
            httpResponse = new HttpResponse(HTTP_INTERNAL_ERROR, stackTrace.toString());
        }
        sendResponse(exchange, httpResponse);
    }

    public abstract HttpResponse processRequest(HttpExchange exchange) throws Exception;

    static void sendResponse(final HttpExchange exchange, final HttpResponse httpResponse) {
        byte[] bytes = null;
        try {
            bytes = httpResponse.body.getBytes(UTF_8);
            exchange.sendResponseHeaders(httpResponse.status, bytes.length);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        try (OutputStream os = exchange.getResponseBody()) {
            if (bytes != null) {
                os.write(bytes);
            }
            os.flush();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    Parameters getParameters(HttpExchange exchange) {
        return new Parameters(exchange);
    }

}
