import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.util.Duration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Lightweight autosave timer with 1s ticks and a background IO thread. */
public class AutosaveService {
    private final BooleanProperty enabled = new SimpleBooleanProperty(false);
    private final IntegerProperty intervalSeconds = new SimpleIntegerProperty(120);
    private final IntegerProperty remainingSeconds = new SimpleIntegerProperty(120);
    private final BooleanProperty showCountdown = new SimpleBooleanProperty(false);

    private final Timeline timeline;
    private final ExecutorService ioPool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Autosave-IO");
        t.setDaemon(true);
        return t;
    });
    private final Runnable onAutosave;
    private final AtomicBoolean inProgress = new AtomicBoolean(false);

    public AutosaveService(Runnable onAutosave) {
        this.onAutosave = onAutosave;
        this.timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        this.timeline.setCycleCount(Timeline.INDEFINITE);

        enabled.addListener((obs, oldVal, en) -> {
            if (en) {
                resetCountdown();
                timeline.play();
            } else {
                timeline.stop();
            }
        });
    }

    private void tick() {
        if (!enabled.get()) return;
        int r = remainingSeconds.get() - 1;
        if (r > 0) {
            remainingSeconds.set(r);
            return;
        }
        remainingSeconds.set(intervalSeconds.get());
        if (onAutosave != null && inProgress.compareAndSet(false, true)) {
            ioPool.submit(() -> {
                try { onAutosave.run(); }
                finally { inProgress.set(false); }
            });
        }
    }

    public void resetCountdown() {
        remainingSeconds.set(Math.max(1, intervalSeconds.get()));
    }

    public void setEnabled(boolean v) { enabled.set(v); }
    public boolean isEnabled() { return enabled.get(); }
    public BooleanProperty enabledProperty() { return enabled; }

    public void setIntervalSeconds(int s) {
        int clamped = Math.max(10, Math.min(24*60*60, s));
        intervalSeconds.set(clamped);
        if (enabled.get()) resetCountdown();
    }
    public int getIntervalSeconds() { return intervalSeconds.get(); }
    public IntegerProperty intervalSecondsProperty() { return intervalSeconds; }

    public IntegerProperty remainingSecondsProperty() { return remainingSeconds; }
    public int getRemainingSeconds() { return remainingSeconds.get(); }

    public BooleanProperty showCountdownProperty() { return showCountdown; }
    public boolean isShowCountdown() { return showCountdown.get(); }
    public void setShowCountdown(boolean v) { showCountdown.set(v); }

    public void shutdown() {
        timeline.stop();
        ioPool.shutdownNow();
    }
}
