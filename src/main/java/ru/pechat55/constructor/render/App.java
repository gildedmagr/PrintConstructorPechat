package ru.pechat55.constructor.render;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import ru.pechat55.constructor.render.rest.*;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;

import static ru.pechat55.constructor.render.Settings.PORT;

public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static final DateTimeFormatter LOG_FILE_NAME_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
    public static final String START_DATE_TIME = LOG_FILE_NAME_PATTERN.format(LocalDateTime.now(ZoneId.of("Europe/Moscow")));
    public static final String JAR_PATH = new File(App.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getAbsolutePath();
    public static final String LOG_PATH = App.JAR_PATH + "/log";
    public static final String LOG_OUT_PATH = App.LOG_PATH + "/" + START_DATE_TIME + ".out";
    public static final String LOG_OUT_PATH_COPY = App.LOG_PATH + "/copy.out";
    public static final String LOG_ERR_PATH = App.LOG_PATH + "/" + START_DATE_TIME + ".err";
    public static final String LOG_ERR_PATH_COPY = App.LOG_PATH + "/copy.err";

    public final long started;

    public ConstructorPool constructorPool;
    Executor executor = Executors.newFixedThreadPool(Settings.POOL_SIZE * 2);

    public HttpServer server;
    public Endpoint statusEndpoint;
    public Endpoint logEndpoint;
    public Endpoint previewEndpoint;
    public Endpoint animationEndpoint;
    public Endpoint quitEndpoint;
    public Endpoint reloadEndpoint;
    public Endpoint testEndpoint;

    public App() {
        log.info("Starting application at port: {}", PORT);
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        started = System.currentTimeMillis();
        constructorPool = new ConstructorPool();
        statusEndpoint = new StatusEndpoint(this).doNotLogRequests();
        previewEndpoint = new PreviewEndpoint(this);
        animationEndpoint = new AnimationEndpoint(this);
        quitEndpoint = new QuitEndpoint(this);
        logEndpoint = new LogEndpoint(this).doNotLogRequests();
        reloadEndpoint = new ReloadEndpoint(this);
        executor.execute(this::listen);
        Utils.trace("init");
        Utils.copyResource("renderer.html", Settings.CONSTRUCTOR_DIR + "/renderer.html");
        constructorPool.init();
        Utils.trace("init");
    }

    public void exit() {
        server.stop(0);
        System.exit(0);
    }


    public String render(Parameters parameters) {
        Constructor constructor = constructorPool.get(parameters.getModelName());
        StringBuilder frames;
        long s = System.currentTimeMillis();
        if (!constructor.isSessionValid()) {
            constructor.startNewSession();
        }
        try {
            constructor.setState(parameters.getJson(), parameters.getModelName());
            constructor.setSize(parameters.getWidth(), parameters.getHeight());
            constructor.setPolarAngle(45);
            frames = new StringBuilder();
            for (int angle = 0, i = 0; angle < 360; angle += 360 / parameters.getFrames(), i++) {
                constructor.setAzimuthAngle(angle);
                String frame = constructor.toDataUrl();
                frames.append(frame);
                if (parameters.getFrames() > 1 && i < parameters.getFrames()) {
                    frames.append("_");
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            constructor.errors.incrementAndGet();
            throw e;
        } finally {
            constructor.runs++;
        }
        long time = System.currentTimeMillis() - s;
        if (time > constructor.maxTime) constructor.maxTime = time;
        constructor.lastTime = time;
        return frames.toString();
    }

    public void listen() {
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/", statusEndpoint);
            server.createContext("/status", statusEndpoint);
            server.createContext("/version", statusEndpoint);
            server.createContext("/preview", previewEndpoint);
            server.createContext("/icon", animationEndpoint);
            server.createContext("/animation", animationEndpoint);
            server.createContext("/quit", quitEndpoint);
            server.createContext("/exit", quitEndpoint);
            server.createContext("/log", logEndpoint);
            server.createContext("/logs", logEndpoint);
            server.createContext("/reload", reloadEndpoint);
            server.setExecutor(executor);
            server.start();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            System.exit(0);
        }
    }

    public static boolean isServerRunning() {
        return !Utils.isPortAvailable(PORT);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            if (!isServerRunning()) {
                log.info("Constructor Rendering Server. Version = {}", Version.getVersion());
                new App();
            } else {
                log.info("Server is already running");
                System.exit(0);
            }
        }
        if (args.length == 1) {
            if (args[0].startsWith("-v")) {
                System.out.println(Version.getVersion());
                System.exit(0);
            }
        }
    }

    void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

}
