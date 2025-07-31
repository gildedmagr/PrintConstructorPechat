package ru.pechat55.constructor.render;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import static org.apache.http.util.TextUtils.isEmpty;

public class Settings {

    public static final String PROPERTIES_PATH = "/render.properties";
    public static final String SEPARATOR = ";";
    public static String CONSTRUCTOR_DIR;
    public static String DRIVER_PATH;
    public static String WEB_DIR;
    public static String BACKGROUND;
    public static int DEFAULT_FRAMES;
    public static int PORT;
    public static int POOL_SIZE;
    public static int DEFAULT_WIDTH;
    public static int DEFAULT_HEIGHT;
    public static int IDLE_TIME_MS;
    public static int QUALITY;
    public static int REQUEST_TIMEOUT_SECONDS;
    public static String[] DOMAINS;
    public static String[] PRELOADED_MODELS;
    public static String CHROME_HOST = System.getenv("SELENIUM_HOST");
    public static String CHROME_PORT = System.getenv("SELENIUM_PORT");
    public static String SELENIUM_GRID_URL = String.format("http://%s:%s/", CHROME_HOST, CHROME_PORT);
    private static Properties properties;

    static {

        try {
            Files.createDirectories(Paths.get(App.LOG_PATH));
            System.setOut(System.err);
           // System.setOut(new PrintStream(new FileOutputStream(App.LOG_OUT_PATH)));
           // System.setErr(new PrintStream(new FileOutputStream(App.LOG_ERR_PATH)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        properties = new Properties();
        try (FileInputStream is = new FileInputStream(App.JAR_PATH + PROPERTIES_PATH)) {
            properties.load(is);
        } catch (IOException e) {
            System.err.println("File " + App.JAR_PATH + PROPERTIES_PATH + " not found");
        }
        CONSTRUCTOR_DIR = properties.getProperty("constructor_dir", "");
        WEB_DIR = properties.getProperty("web_dir", "");
        DRIVER_PATH = properties.getProperty("driver_path", App.JAR_PATH + "/chromedriver");
        BACKGROUND = properties.getProperty("background", "#eeeeee");
        PORT = parseInt("port", 2018);
        POOL_SIZE = parseInt("pool_size", 8);
        DOMAINS = parseArray("domains");
        PRELOADED_MODELS = parseArray("preloaded_models");
        DEFAULT_FRAMES = parseInt("animation_steps", 16);
        if (DEFAULT_FRAMES > 64) DEFAULT_FRAMES = 64;
        DEFAULT_WIDTH = parseInt("default_width", 512);
        DEFAULT_HEIGHT = parseInt("default_height", 512);
        IDLE_TIME_MS = parseInt("idle_time_ms", 100);
        QUALITY = parseInt("quality", 80);
        REQUEST_TIMEOUT_SECONDS = parseInt("request_timeout_seconds", 10);
        if (QUALITY < 1 || QUALITY > 99) QUALITY = 80;
    }

    private static int parseInt(String property, int defaultValue) {
        String value = properties.getProperty(property);
        if (isEmpty(value)) return defaultValue;
        return Integer.parseInt(value);
    }

    private static String[] parseArray(String property) {
        String value = properties.getProperty(property);
        if (isEmpty(value)) return new String[0];
        return value.split(SEPARATOR);
    }

}