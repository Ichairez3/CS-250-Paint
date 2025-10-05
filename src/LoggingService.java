import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.LinkedBlockingQueue;

/** Background, single-writer logging service (one event per line). */
public class LoggingService {

    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;
    private Thread writerThread;

    private final File logDir = new File(System.getProperty("user.home"), "PaintFXLogs");
    private BufferedWriter out = null;
    private LocalDate currentDay = null;

    private final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    public LoggingService() {
        logDir.mkdirs();
        writerThread = new Thread(this::runLoop, "PaintFX-Logger");
        writerThread.setDaemon(true);
        writerThread.start();
        // first line lets us verify the logger is alive
        log("Logger started");
    }

    /** App-level log (no specific document). */
    public void log(String message) {
        enqueue(formatTag("[app]") + " " + message);
    }

    /** Document-scoped log (shows [filename] or [new tab]). */
    public void log(ImageDocument doc, String message) {
        String tag = "[new tab]";
        if (doc != null) {
            String name = (doc.getFile() != null ? doc.getFile().getName() : doc.getDisplayName());
            if (name == null || name.isEmpty()) name = "new tab";
            tag = "[" + name.replace("*", "") + "]";
        }
        enqueue(formatTag(tag) + " " + message);
    }

    /** Stop the writer thread and flush file. Call at app shutdown. */
    public void shutdown() {
        running = false;
        if (writerThread != null) {
            writerThread.interrupt();
            try { writerThread.join(1500); } catch (InterruptedException ignored) {}
        }
        closeWriter();
    }

    //internals

    private String formatTag(String bracketedDoc) {
        LocalDateTime now = LocalDateTime.now();
        return DATE.format(now) + " " + bracketedDoc + " " + TIME.format(now);
    }

    private void enqueue(String line) {
        // ensure one line per event
        queue.offer(line.replace("\n", " ") + System.lineSeparator());
    }

    private void runLoop() {
        while (running || !queue.isEmpty()) {
            try {
                String line = queue.poll();
                if (line == null) {
                    Thread.sleep(50);
                    continue;
                }
                writeLine(line);
            } catch (InterruptedException ie) {
                // loop will exit if running=false
            } catch (IOException io) {
                // if writing somehow fails, we just drop until next line
            }
        }
    }

    private void writeLine(String line) throws IOException {
        LocalDate today = LocalDate.now();
        if (out == null || !today.equals(currentDay)) {
            rotate(today);
        }
        out.write(line);
        out.flush();
    }

    private void rotate(LocalDate today) throws IOException {
        closeWriter();
        currentDay = today;
        File f = new File(logDir, today.toString() + ".log"); // yyyy-MM-dd.log
        out = new BufferedWriter(new FileWriter(f, /*append=*/true));
    }

    private void closeWriter() {
        if (out != null) {
            try { out.flush(); out.close(); } catch (IOException ignored) {}
            out = null;
        }
    }
    public File getLogDir() {
        return logDir;
    }

    public File currentLogFile() {
        return new File(logDir, LocalDate.now().toString() + ".log");
    }

}
