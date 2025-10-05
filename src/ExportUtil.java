import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

/** Small helpers for export preflight checks. No packages. */
public class ExportUtil {

    /** Quick scan: does the image contain any pixel with alpha < 1.0 ? */
    public static boolean hasTransparency(WritableImage img) {
        if (img == null) return false;
        PixelReader pr = img.getPixelReader();
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double a = pr.getColor(x, y).getOpacity();
                if (a < 1.0) return true;
            }
        }
        return false;
    }

    /** True if the target format will drop alpha (e.g., jpg, bmp). */
    public static boolean formatDropsAlpha(String fmt) {
        if (fmt == null) return false;
        String f = fmt.toLowerCase();
        return f.equals("jpg") || f.equals("jpeg") || f.equals("bmp");
    }
}
