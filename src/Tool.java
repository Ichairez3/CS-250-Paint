import javafx.scene.input.MouseEvent;


public interface Tool {
    void onPressed(MouseEvent e, ImageDocument doc);
    void onDragged(MouseEvent e, ImageDocument doc);
    void onReleased(MouseEvent e, ImageDocument doc);
}
