import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import java.util.function.Supplier;

public class TextTool implements Tool {
    private final Supplier<Integer> sizeSupplier;

    public TextTool(Supplier<Integer> sizeSupplier) {
        this.sizeSupplier = sizeSupplier;
    }

    @Override
    public void onPressed(MouseEvent e, ImageDocument doc) {
        // Open a simple dialog with a single-line TextArea (users can paste multi-line, we draw first line)
        Dialog<String> dlg = new Dialog<>();
        dlg.setTitle("Insert Text");
        TextArea ta = new TextArea();
        ta.setPromptText("Type your text...");
        ta.setPrefRowCount(1);
        dlg.getDialogPane().setContent(ta);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.setResultConverter(btn -> btn == ButtonType.OK ? ta.getText() : null);

        String text = dlg.showAndWait().orElse(null);
        if (text == null || text.isEmpty()) return;

        // Position (Canvas fillText uses baseline at y)
        double x = e.getX();
        double y = e.getY();

        int size = Math.max(6, Math.min(200, sizeSupplier.get()));
        Font font = Font.font(size);

        // Measure bounds with a Text node (not rendered)
        Text meas = new Text(text);
        meas.setFont(font);
        Bounds b = meas.getLayoutBounds();
        double w = Math.max(1, b.getWidth());
        double h = Math.max(1, b.getHeight());

        // Dirty rectangle (pad a bit)
        int pad = doc.strokeWidthProperty().get() + 6;
        Rectangle2D dirty = new Rectangle2D(
                Math.max(0, x - pad),
                Math.max(0, (y - h) - pad),  // baseline -> top
                w + 2 * pad,
                h + 2 * pad
        );

        // Commit draw onto backing image
        double fx = x, fy = y;
        doc.commit(gc -> {
            // Use stroke color as fill for text
            gc.setFill(doc.strokeColorProperty().get());
            gc.setFont(font);
            gc.fillText(text, fx, fy);
        }, dirty);
    }

    @Override public void onDragged(MouseEvent e, ImageDocument doc) { /* no-op */ }
    @Override public void onReleased(MouseEvent e, ImageDocument doc) { /* no-op */ }
}
