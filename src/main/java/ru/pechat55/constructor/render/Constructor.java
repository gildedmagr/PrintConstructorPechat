package ru.pechat55.constructor.render;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.regex.Pattern;

import static org.apache.http.util.TextUtils.isEmpty;
import static ru.pechat55.constructor.render.Settings.SELENIUM_GRID_URL;

public class Constructor {

    private static final Logger log = LoggerFactory.getLogger(Constructor.class);

    public RemoteWebDriver driver;
    public ChromeOptions options;
    public volatile URL seleniumGridUrl;
    public volatile String modelName;
    public volatile String version;
    public volatile int width;
    public volatile int height;
    public volatile int polarAngle = 45;
    public volatile long runs = 0;
    public AtomicLong errors = new AtomicLong(0);
    public volatile long maxTime = 0;
    public volatile long lastTime = 0;
    public volatile long start = 0;

    public static final String RENDERER_PATH = "file://" + Settings.CONSTRUCTOR_DIR + "/renderer.html";
    public static final Pattern IMG_SRC_PATTERN = Pattern.compile("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]\\.(jpg|jpeg|png|gif|svg|bmp|JPG|JPEG|PNG|GIF|SVG|BMP)");
    public static final String $ = "\\$";

    private final String setStateScript = Utils.readResource("set_state.js");
    private final String loadModelScript = Utils.readResource("load_model.js");
    private final String resizeScript = Utils.readResource("resize.js");
    private final String preloadImageScript = Utils.readResource("preload_image.js");

    public boolean isBusy() {
        return System.currentTimeMillis() - start < Settings.REQUEST_TIMEOUT_SECONDS * 1000L;
    }

    public Constructor(RemoteWebDriver driver) {
        log.info("Creating Constructor instance, url: {} , render path{}", driver.getCurrentUrl(), RENDERER_PATH);
        this.driver = driver;
        this.driver.get(RENDERER_PATH);
        driver.manage().timeouts().scriptTimeout(Duration.of(Settings.REQUEST_TIMEOUT_SECONDS - 1, ChronoUnit.SECONDS));
        updateVersion();
    }


    public Constructor(ChromeOptions options) {
        this.options = options;
        createDriver(options);
    }

    private void createDriver(ChromeOptions options) {
        try {
            seleniumGridUrl = new URI(SELENIUM_GRID_URL).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            log.error("Unable to create URL from {}", SELENIUM_GRID_URL, e);
            errors.incrementAndGet();
            throw new RuntimeException(e);
        }
        RemoteWebDriver driver = new RemoteWebDriver(seleniumGridUrl, options);
        driver.setLogLevel(Level.INFO);
        log.info("Creating Constructor instance, {}, {}", driver.getCurrentUrl(), RENDERER_PATH);
        this.driver = driver;
        clearCache();
        this.driver.get(RENDERER_PATH);
        log.info("Connected to Selenium Grid at: {}", seleniumGridUrl);
        driver.manage().timeouts().scriptTimeout(Duration.of(Settings.REQUEST_TIMEOUT_SECONDS - 1, ChronoUnit.SECONDS));
        updateVersion();
    }

    public boolean isSessionValid() {
        try {
            driver.getCurrentUrl(); // or driver.getTitle()
            return true;
        } catch (NoSuchSessionException | SessionNotCreatedException e) {
            log.info("Session not found, {}", driver.getSessionId());
            driver.quit();
            return false;
        } catch (Exception e) {
            log.error("Unexpected session check error: {}", e.getMessage());
            return false;
        }
    }

    public void clearCache(){
        this.driver.manage().deleteAllCookies();
    }

    public void startNewSession() {
        log.info("Creating new session");
        createDriver(options);
    }

    public void setSize(int width, int height) {
        if (width != this.width || height != this.height) {
            this.width = width;
            this.height = height;
            String widthValue = String.valueOf(width);
            String heightValue = String.valueOf(height);
            driver.manage().window().setSize(new Dimension(width, height));
            String script = resizeScript
                    .replaceFirst($, widthValue)
                    .replaceFirst($, heightValue);
            driver.executeScript(script);
            for (int i = 0; i < 5; i++) {
                String previewWidth = driver.executeScript("return Constructor.instance.preview.container.clientWidth").toString();
                String previewHeight = driver.executeScript("return Constructor.instance.preview.container.clientHeight").toString();
                log.info("Preview container size = {} x {}", previewWidth, previewHeight);
                if (previewWidth.equals(widthValue) && previewHeight.equals(heightValue)) {
                    return;
                }
                Utils.sleep(10);
            }
        }
    }

    public void setMode3D() {
        driver.executeScript("c.setMode(Mode.Mode3D)");
    }

    public void addSide(int width, int height) {
        driver.executeScript("c.addSide(" + width + ", " + height + ")");
    }

    public Element2D addRectangle() {
        Element2D element = new Element2D(this);
        driver.executeScript(element.id + " = c.addElement(ElementType.RECTANGLE)");
        return element;
    }

    public void updateVersion() {
        version = (String) driver.executeScript("return Constructor.version");
    }

    public void setState(final String json, final String modelName) {
        String state = json;
        for (String domain : Settings.DOMAINS) {
            if (!isEmpty(domain)) {
                state = json.replaceAll(domain, "file://" + Settings.WEB_DIR);
            }
        }

//        Matcher matcher = Constructor.IMG_SRC_PATTERN.matcher(json);
//        while (matcher.find()) {
//            String url = matcher.group();
//            Utils.log("Preloading image:", url);
//            String preloadScript = preloadImageScript.replaceFirst($, url);
//            driver.executeAsyncScript(preloadScript);
//        }

        String script = setStateScript.replaceFirst($, state);
        driver.executeAsyncScript(script);
        driver.executeAsyncScript("var callback = arguments[arguments.length - 1];Constructor.instance.preview.updateSideMaterials(() => callback());");
        this.modelName = modelName;
    }

    public String getState() {
        return (String) driver.executeScript("return c.getState(true)");
    }

    public boolean loadModel(String modelName) {
        try {
            driver.executeAsyncScript(loadModelScript.replaceFirst($, modelName));
            this.modelName = modelName;
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public void setBackground(String value) {
        driver.executeScript("c.preview.renderer.setClearColor('" + value + "');");
        driver.executeScript("document.body.style.background='" + value + "';");
    }

    public void setAzimuthAngle(int value) {
        String angle = String.valueOf(Math.toRadians(value));
        driver.executeScript("c.preview.controls.maxAzimuthAngle = " + angle);
        driver.executeScript("c.preview.controls.minAzimuthAngle = " + angle);
        driver.executeScript("c.preview.controls.update()");
    }

    public void setPolarAngle(int value) {
        if (polarAngle != value) {
            String angle = String.valueOf(Math.toRadians(value));
            driver.executeScript("c.preview.controls.maxPolarAngle = " + angle);
            driver.executeScript("c.preview.controls.minPolarAngle = " + angle);
            this.polarAngle = value;
        }
    }

    public void printLog() {
        LogEntries logEntries = driver.manage().logs().get(LogType.BROWSER);
        for (LogEntry entry : logEntries) {
            System.out.println(new Date(entry.getTimestamp()) + " " + entry.getLevel() + " " + entry.getMessage());
        }
    }

    public String toDataUrl() {
        return (String) driver.executeScript("return c.preview.renderer.domElement.toDataURL('image/jpeg', 0." + Settings.QUALITY + ");");
    }

    @Override
    public String toString() {
        return width + "x" + height + " " + modelName + " " + runs;
    }
}
