package ru.pechat55.constructor.render.rest;

import com.sun.net.httpserver.HttpExchange;
import org.apache.http.HttpStatus;
import ru.pechat55.constructor.render.App;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static ru.pechat55.constructor.render.App.LOG_ERR_PATH_COPY;
import static ru.pechat55.constructor.render.App.LOG_OUT_PATH_COPY;

public class LogEndpoint extends Endpoint {

    public LogEndpoint(App app) {
        super(app);
    }

    @Override
    public HttpResponse processRequest(HttpExchange exchange) throws Exception {

        StringBuilder body = new StringBuilder("=================STANDARD OUTPUT:\n\n");
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(App.LOG_OUT_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body
                        .append(line)
                        .append("\n");
            }
        }

        body.append("\n\n\n=================ERROR OUTPUT:\n\n");
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(App.LOG_ERR_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body
                        .append(line)
                        .append("\n");
            }
        }

        return new HttpResponse(
                HttpStatus.SC_OK,
                body.toString()
        );

    }

}
