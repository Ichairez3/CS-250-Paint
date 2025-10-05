import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class AutosaveServiceTest {
    @BeforeAll
    static void setup() { FxTestSupport.init(); }

    @Test
    void manualSaveResetsCountdownToInterval() {
        AutosaveService svc = new AutosaveService(() -> {});
        svc.setIntervalSeconds(60);
        svc.setEnabled(true);

        svc.remainingSecondsProperty().set(10);

        svc.resetCountdown();

        Assertions.assertEquals(60, svc.getRemainingSeconds(),
                "Reset should restore remainingSeconds to the full interval");

        svc.shutdown();
    }
}
