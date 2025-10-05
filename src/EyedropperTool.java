import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class EyedropperTool implements Tool {
    private void pick(MouseEvent e, ImageDocument doc){
        int x = (int) Math.round(e.getX());
        int y = (int) Math.round(e.getY());
        if (x < 0 || y < 0 || x >= doc.getBacking().getWidth() || y >= doc.getBacking().getHeight()) return;
        Color c = doc.getBacking().getPixelReader().getColor(x, y);
        doc.strokeColorProperty().set(c);
    }
    @Override public void onPressed(MouseEvent e, ImageDocument doc) { pick(e, doc); }
    @Override public void onDragged(MouseEvent e, ImageDocument doc) { pick(e, doc); }
    @Override public void onReleased(MouseEvent e, ImageDocument doc) { /* no commit needed */ }
}
