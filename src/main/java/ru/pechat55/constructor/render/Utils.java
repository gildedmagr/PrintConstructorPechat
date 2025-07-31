package ru.pechat55.constructor.render;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    private static Map<String, Long> times = new HashMap<>();
    private static final Pattern MODEL_PATTERN = Pattern.compile("\"model\"\\s?:\\s?\"([^\'^\"]{1,50})\"");
    public static final DateTimeFormatter LOG_DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

    public static String getCurrentLogDateTime() {
        return LOG_DATE_TIME_PATTERN.format(LocalDateTime.now(ZoneId.of("Europe/Moscow")));
    }

    public static String readResource(String path) {
        try {
            return new String(readBytes(path));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream getStream(String path) {
        InputStream is = App.class.getResourceAsStream("/resources/" + path);
        if (is == null) {
            is = App.class.getClassLoader().getResourceAsStream(path);
        }
        return is;
    }

    public static byte[] readBytes(String path) throws IOException {
        InputStream is = getStream(path);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    public static void copyResource(String source, String target) {
        copyResource(source, target, false);
    }

    public static void copyResource(String source, String target, boolean setExecutable) {
        Path path = Paths.get(target);
        File file = path.toFile();
        if (!file.exists()) {
            try {
                Files.copy(getStream(source), path, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (setExecutable && !Files.isExecutable(path)) {
            path.toFile().setExecutable(true);
        }
    }

    public static Path write(String string, String outputPath) {
        return write(string.getBytes(), outputPath);
    }

    public static Path write(byte[] file, String outputPath) {
        try {
            Path path = Paths.get(outputPath);
            Files.createDirectories(path.getParent());
            return Files.write(path, file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void trace(String value) {
        long now = System.currentTimeMillis();
        Long time = times.get(value);
        if (time == null) {
            times.put(value, now);
        } else {
            long passed = now - time;
            log.info(value, passed, "ms");
            times.remove(value);
        }
    }

    public static boolean isPortAvailable(int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }
        return false;
    }

    public static String parseModelName(String json) {
        Matcher matcher = MODEL_PATTERN.matcher(json);
        if (matcher.find()) return matcher.group(1);
        return null;
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

}
