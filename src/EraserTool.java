import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class EraserTool implements Tool {
    private final List<Double> xs = new ArrayList<>();
    private final List<Double> ys = new ArrayList<>();
    private boolean drawing = false;

    @Override
    public void onPressed(MouseEvent e, ImageDocument doc) {
        xs.clear(); ys.clear();
        xs.add(e.getX()); ys.add(e.getY());
        drawing = true;
        doc.clearOverlay();
    }

    @Override
    public void onDragged(MouseEvent e, ImageDocument doc) {
        if (!drawing) return;
        double x = e.getX(), y = e.getY();
        double lx = xs.get(xs.size()-1), ly = ys.get(ys.size()-1);

        // Live preview on overlay as a white stroke
        GraphicsContext g = doc.getOverlay().getGraphicsContext2D();
        g.setLineDashes((double[]) null);
        g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        g.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        g.setLineWidth(doc.strokeWidthProperty().get());
        g.setStroke(Color.WHITE);
        g.strokeLine(lx, ly, x, y);

        xs.add(x); ys.add(y);
    }

    @Override
    public void onReleased(MouseEvent e, ImageDocument doc) {
        if (!drawing) return;
        drawing = false;

        // Compute dirty bounds for this path
        double minX = xs.get(0), maxX = xs.get(0), minY = ys.get(0), maxY = ys.get(0);
        for (int i=1;i<xs.size();i++){
            double px = xs.get(i), py = ys.get(i);
            if (px<minX) minX=px; if (px>maxX) maxX=px;
            if (py<minY) minY=py; if (py>maxY) maxY=py;
        }
        int pad = doc.strokeWidthProperty().get() + 4;
        Rectangle2D dirty = new Rectangle2D(Math.max(0, minX-pad), Math.max(0, minY-pad),
                (maxX-minX)+2*pad, (maxY-minY)+2*pad);

        final ArrayList<Double> fx = new ArrayList<>(xs);
        final ArrayList<Double> fy = new ArrayList<>(ys);
        final int w = doc.strokeWidthProperty().get();

        // Commit one undoable step
        doc.commit(gc -> {
            gc.setLineDashes((double[]) null);
            gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            gc.setLineWidth(w);
            gc.setStroke(Color.WHITE); // erase to white
            for (int i=1;i<fx.size();i++){
                gc.strokeLine(fx.get(i-1), fy.get(i-1), fx.get(i), fy.get(i));
            }
        }, dirty);

        doc.clearOverlay();
    }
}
