import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class LocalWebServer {
    private HttpServer server;
    private int port;
    private Map<String, ImageDocument> docs = new LinkedHashMap<>();
    private Map<String, String> titles = new LinkedHashMap<>();

    public void start(int port, LinkedHashMap<String, ImageDocument> docsById,
                      LinkedHashMap<String, String> titlesById) throws Exception {
        stop(); // in case it's running
        this.port = port;
        this.docs = new LinkedHashMap<>(docsById);
        this.titles = new LinkedHashMap<>(titlesById);

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/img", new ImageHandler());
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "PaintFX-Web");
            t.setDaemon(true);
            return t;
        }));
        server.start();
    }

    public void stop() {
        if (server != null) { server.stop(0); server = null; }
    }

    public boolean isRunning() { return server != null; }
    public int getPort() { return port; }
    public Map<String, ImageDocument> getSharedDocs() { return docs; }

    private class RootHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            StringBuilder html = new StringBuilder();
            html.append("<!doctype html><html><head><meta charset='utf-8'>")
                    .append("<title>PaintFX Share</title>")
                    .append("<meta http-equiv='Cache-Control' content='no-store'>")
                    .append("<style>body{font-family:sans-serif;margin:20px}h1{margin:0 0 10px}img{max-width:100%;height:auto;border:1px solid #ddd;padding:4px;margin:10px 0}</style>")
                    .append("</head><body><h1>PaintFX Share</h1>");

            if (docs.isEmpty()) {
                html.append("<p>No images selected for sharing.</p>");
            } else {
                long ts = System.currentTimeMillis();
                for (String id : docs.keySet()) {
                    String title = titles.getOrDefault(id, id);
                    html.append("<h2>").append(escape(title)).append("</h2>")
                            .append("<img src=\"/img/").append(id).append(".png?ts=").append(ts).append("\">");
                }
            }
            html.append("</body></html>");

            byte[] bytes = html.toString().getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            ex.getResponseHeaders().add("Cache-Control", "no-store");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        }
    }

    private class ImageHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            URI uri = ex.getRequestURI(); // /img/{id}.png
            String path = uri.getPath();
            String id = null;
            if (path.startsWith("/img/") && path.endsWith(".png")) {
                id = path.substring("/img/".length(), path.length() - ".png".length());
            }
            ImageDocument doc = (id == null) ? null : docs.get(id);
            if (doc == null) { ex.sendResponseHeaders(404, -1); return; }

            WritableImage fx = doc.getBacking();
            BufferedImage bi = SwingFXUtils.fromFXImage(fx, null);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", bos);
            byte[] bytes = bos.toByteArray();

            ex.getResponseHeaders().add("Content-Type", "image/png");
            ex.getResponseHeaders().add("Cache-Control", "no-store");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        }
    }

    private static String escape(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }
}
