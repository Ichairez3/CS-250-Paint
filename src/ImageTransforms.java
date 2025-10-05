import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/** Utility image transforms using JavaFX Canvas transforms */
public final class ImageTransforms {

    private ImageTransforms() {}

    public static WritableImage rotate90(Image src) {
        int w = (int) Math.round(src.getWidth());
        int h = (int) Math.round(src.getHeight());
        Canvas c = new Canvas(h, w);
        GraphicsContext g = c.getGraphicsContext2D();
        g.translate(h, 0);
        g.rotate(90);
        g.drawImage(src, 0, 0);
        return snap(c);
    }
    public static WritableImage rotate180(Image src) {
        int w = (int) Math.round(src.getWidth());
        int h = (int) Math.round(src.getHeight());
        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();
        g.translate(w, h);
        g.rotate(180);
        g.drawImage(src, 0, 0);
        return snap(c);
    }
    public static WritableImage rotate270(Image src) {
        int w = (int) Math.round(src.getWidth());
        int h = (int) Math.round(src.getHeight());
        Canvas c = new Canvas(h, w);
        GraphicsContext g = c.getGraphicsContext2D();
        g.translate(0, w);
        g.rotate(270);
        g.drawImage(src, 0, 0);
        return snap(c);
    }
    public static WritableImage flipHorizontal(Image src) {
        int w = (int) Math.round(src.getWidth());
        int h = (int) Math.round(src.getHeight());
        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();
        g.translate(w, 0);
        g.scale(-1, 1);
        g.drawImage(src, 0, 0);
        return snap(c);
    }
    public static WritableImage flipVertical(Image src) {
        int w = (int) Math.round(src.getWidth());
        int h = (int) Math.round(src.getHeight());
        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();
        g.translate(0, h);
        g.scale(1, -1);
        g.drawImage(src, 0, 0);
        return snap(c);
    }

    private static WritableImage snap(Canvas c){
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage out = new WritableImage((int)c.getWidth(), (int)c.getHeight());
        c.snapshot(sp, out);
        return out;
    }
}
