import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class ExportUtilTransparencyTest {
    @BeforeAll
    static void setup() { FxTestSupport.init(); }

    @Test
    void detectsTransparencyAndAlphaDroppingFormats() {
        WritableImage img = new WritableImage(2, 2);
        img.getPixelWriter().setColor(0, 0, new Color(1, 0, 0, 0.5)); // half-transparent red

        Assertions.assertTrue(ExportUtil.hasTransparency(img), "Should detect transparency");
        Assertions.assertTrue(ExportUtil.formatDropsAlpha("jpg"), "JPG drops alpha");
        Assertions.assertTrue(ExportUtil.formatDropsAlpha("bmp"), "BMP drops alpha");
        Assertions.assertFalse(ExportUtil.formatDropsAlpha("png"), "PNG keeps alpha");
    }

    @Test
    void opaqueImageHasNoTransparency() {
        WritableImage img = new WritableImage(2, 2);
        img.getPixelWriter().setColor(0, 0, Color.RED); // fully opaque
        Assertions.assertFalse(ExportUtil.hasTransparency(img));
    }
}
