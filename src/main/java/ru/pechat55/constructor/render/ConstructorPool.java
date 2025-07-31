package ru.pechat55.constructor.render;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

import static ru.pechat55.constructor.render.Settings.CHROME_HOST;
import static ru.pechat55.constructor.render.Settings.CHROME_PORT;

public class ConstructorPool {

    private static final Logger log = LoggerFactory.getLogger(ConstructorPool.class);

    public volatile Constructor[] constructors = new Constructor[Settings.POOL_SIZE];
    public static ChromeOptions options;

    public ConstructorPool() {
        System.setProperty("jdk.internal.httpclient.disableHostnameVerification", "true");
        System.setProperty(ChromeDriverService.CHROME_DRIVER_LOG_PROPERTY, "chromedriver.log");
        System.setProperty(ChromeDriverService.CHROME_DRIVER_VERBOSE_LOG_PROPERTY, "true");
        options = new ChromeOptions();
        if (Objects.isNull(System.getenv("IS_HEADLESS")) || Boolean.TRUE.toString().equalsIgnoreCase(System.getenv("IS_HEADLESS"))) {
            System.out.println("Create pool of web drivers in headless mode");
            options.addArguments("--headless");
        } else {
            System.out.println("Create pool of web drivers in normal mode");
        }
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-file-access-from-files");
        options.addArguments("--allow-file-access");
        options.addArguments("--no-sandbox");
        options.addArguments("--silent");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-site-isolation-trials");
        options.addArguments("--log-level=3");
        LoggingPreferences loggingPreferences = new LoggingPreferences();
        loggingPreferences.enable(LogType.BROWSER, Level.OFF);
        //options.setCapability(CapabilityType.LOGGING_PREFS, loggingPreferences);
    }

    public void init() {

        for (int i = 0, j = 0; i < Settings.POOL_SIZE; i++, j++) {
            if (j >= Settings.PRELOADED_MODELS.length) j = 0;
            final String modelName = Settings.PRELOADED_MODELS.length == 0 ? null : Settings.PRELOADED_MODELS[j];
            final int index = i;
            new Thread(() -> {
                Constructor constructor = new Constructor(options);
                if (modelName != null) constructor.loadModel(modelName);
                constructor.setSize(Settings.DEFAULT_WIDTH, Settings.DEFAULT_HEIGHT);
                constructor.setBackground(Settings.BACKGROUND);
                constructor.setMode3D();
                constructors[index] = constructor;
            }).start();
        }
        while (size() < Settings.POOL_SIZE) Utils.sleep(100);
    }

    public void reinit(int index) {
        constructors[index].driver.quit();
        RemoteWebDriver driver = new RemoteWebDriver(options);
        driver.setLogLevel(Level.INFO);
        Constructor constructor = new Constructor(driver);
        constructor.setSize(Settings.DEFAULT_WIDTH, Settings.DEFAULT_HEIGHT);
        constructor.setBackground(Settings.BACKGROUND);
        constructor.setMode3D();
        constructors[index] = constructor;
    }

    public synchronized Constructor get() {
        return get(null);
    }

    public synchronized Constructor get(String modelName) {
        Constructor sameModelCandidate = null;
        Constructor nullModelCandidate = null;
        Constructor otherModelCandidate = null;
        Constructor winner;

        for (Constructor candidate : constructors) {
            if (candidate != null && !candidate.isBusy()) {
                if (candidate.modelName == null) {
                    if (nullModelCandidate == null || nullModelCandidate.runs > candidate.runs) {
                        nullModelCandidate = candidate;
                    }
                } else if (candidate.modelName.equals(modelName)) {
                    if (sameModelCandidate == null || sameModelCandidate.runs > candidate.runs) {
                        sameModelCandidate = candidate;
                    }
                } else {
                    if (otherModelCandidate == null || otherModelCandidate.runs > candidate.runs) {
                        otherModelCandidate = candidate;
                    }
                }
            }
        }

        if (sameModelCandidate != null) {
            winner = sameModelCandidate;
        } else if (nullModelCandidate != null) {
            winner = nullModelCandidate;
        } else {
            winner = otherModelCandidate;
        }

        if (winner != null) {
            winner.start = System.currentTimeMillis();
            return winner;
        }
        log.info("Waiting for an idle Constructor");
        Utils.sleep(100);
        return get(modelName);
    }

    private synchronized int size() {
        int size = 0;
        for (Constructor constructor : constructors) {
            if (constructor != null) size++;
        }
        return size;
    }


}
