package ru.pechat55.constructor.render;

import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Parameters extends HashMap<String, Object> {

    private static final Logger log = LoggerFactory.getLogger(Parameters.class);


    public static final char NEWLINE_CHAR = System.lineSeparator().charAt(0);
    public static final String JSON = "json";
    public static final String MODEL = "model";
    public static final String FRAMES = "frames";
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";

    public Parameters(HttpExchange exchange) {
        super();
        for (String param : exchange.getRequestURI().getQuery().split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                put(entry[0], Integer.parseInt(entry[1]));
            } else {
                put(entry[0], null);
            }
        }

        StringBuilder body = new StringBuilder();
        try (InputStreamReader in = new InputStreamReader(exchange.getRequestBody(), UTF_8);
             BufferedReader br = new BufferedReader(in)) {
            int b;
            while ((b = br.read()) != -1) {
                char c = (char) b;
                if (c != NEWLINE_CHAR) {
                    body.append(c);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        String json = body.toString().replaceAll("\\\\n", "<br>");
        log.info(json);
        String modelName;

        modelName = Utils.parseModelName(json);
        put(JSON, json);
        put(MODEL, modelName);
        if (!isValid()) throw new IllegalArgumentException();
    }

    private boolean isValid() {
        return getFrames() < 64
                && getHeight() < 8192
                && getWidth() < 8192
                && getJson() != null
                && getJson().length() < 8 * 1024 * 1024;
    }

    public Parameters(String json, int width, int height, int frames) {
        super();
        put(JSON, json);
        put(MODEL, Utils.parseModelName(json));
        put(WIDTH, width);
        put(HEIGHT, height);
        put(FRAMES, frames);
    }

    public Parameters(String json, int width, int height) {
        this(json, width, height, 1);
    }

    public void setFrames(int count) {
        put(FRAMES, count);
    }

    public int getFrames() {
        return getInt(FRAMES, Settings.DEFAULT_FRAMES);
    }

    public int getWidth() {
        return getInt(WIDTH, Settings.DEFAULT_WIDTH);
    }

    public int getHeight() {
        return getInt(HEIGHT, Settings.DEFAULT_HEIGHT);
    }

    public String getJson() {
        return (String) get(JSON);
    }

    public String getModelName() {
        return (String) get(MODEL);
    }

    private int getInt(String key, int defaultValue) {
        Integer value = (Integer) get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();
        forEach((key, value) -> string.append(key)
                .append(" : ")
                .append(value)
                .append(System.lineSeparator()));
        return string.toString();
    }

}
