package ru.pechat55.constructor.render.rest;

import com.sun.net.httpserver.HttpExchange;
import org.apache.commons.io.input.ReversedLinesFileReader;
import ru.pechat55.constructor.render.App;
import ru.pechat55.constructor.render.Constructor;
import ru.pechat55.constructor.render.Utils;
import ru.pechat55.constructor.render.Version;

import java.io.File;

public class StatusEndpoint extends Endpoint {

    public StatusEndpoint(App app) {
        super(app);
    }

    private static final String html = Utils.readResource("status.html");

    @Override
    public HttpResponse processRequest(HttpExchange exchange) {
        return new HttpResponse(200, getStatus());
    }

    public String getStatus() {
        StringBuilder rows = new StringBuilder();
        int i = -1;
        for (Constructor constructor : app.constructorPool.constructors) {
            i++;
            if (constructor != null) {
                rows.append(constructor.isBusy() ? "<tr class=\"busy\">" : "<tr>")
                        .append("<td>").append(constructor.version).append("</td>")
                        .append("<td>").append(constructor.modelName).append("</td>")
                        .append("<td>").append(constructor.width).append("</td>")
                        .append("<td>").append(constructor.height).append("</td>")
                        .append("<td>").append(constructor.isBusy()).append("</td>")
                        .append("<td>").append(toSeconds(constructor.maxTime)).append("</td>")
                        .append("<td>").append(toSeconds(constructor.lastTime)).append("</td>")
                        .append("<td>").append(constructor.runs).append("</td>")
                        .append("<td class=\"error\">")
                        .append(constructor.errors.get() > 0 ? constructor.errors : "")
                        .append("</td>")
                        .append("<td>")
                        .append("<a href=\"reload?index=")
                        .append(i)
                        .append("\" target=\"_blank\"><span class=reload>&#x21bb;</span></a>")
                        .append("</td>")
                        .append("</tr>");
            } else {
                rows.append("<tr><td colspan=9>initializing...</td></tr>");
            }
        }

        return html
                .replaceFirst(Constructor.$, Version.getVersion())
                .replaceFirst(Constructor.$, getUptime())
                .replaceFirst(Constructor.$, rows.toString());
    }

    private String toSeconds(long millis) {
        double seconds = Math.round((float) millis / 100);
        return seconds / 10 + " s";
    }

    private String getUptime() {
        long seconds = (System.currentTimeMillis() - app.started) / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        StringBuilder uptime = new StringBuilder();
        if (days > 0) uptime.append(days).append(" d ");
        if (hours > 0) uptime.append(hours % 24).append(" h ");
        uptime.append(minutes % 60).append(" m");
        return uptime.toString();
    }

}
