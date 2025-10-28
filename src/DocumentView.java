import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class DocumentView {
    private final ImageDocument doc;
    private final ScrollPane scroller;
    private final StackPane root;

    // Canvases and zoom wrapper
    private final Group content;
    private final Group zoomGroup;

    private Tool tool;

    // Zoom
    private final DoubleProperty zoom = new SimpleDoubleProperty(1.0);
    private static final double MIN_ZOOM = 0.10, MAX_ZOOM = 8.0, ZOOM_STEP = 1.10;

    // Disable panning while drawing
    private boolean drawingActive = false;

    //floating overlay for paste/move
    private final ImageView floatingView = new ImageView();
    private WritableImage floatingImage = null;
    private boolean floatingActive = false;
    private double floatX = 0, floatY = 0;            // image-space coords
    private double dragOffsetX = 0, dragOffsetY = 0;  // click offset inside floating

    public DocumentView(ImageDocument doc){
        this.doc = doc;

        content   = new Group(doc.getCanvas(), floatingView, doc.getOverlay()); // floating sits between base & overlay
        zoomGroup = new Group(content);

        scroller = new ScrollPane(zoomGroup);
        scroller.setPannable(true);
        scroller.setFitToWidth(false);
        scroller.setFitToHeight(false);

        root = new StackPane(scroller);
        applyScale();

        // Ctrl + wheel zooming
        scroller.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isControlDown()) {
                if (e.getDeltaY() > 0) zoomIn(); else zoomOut();
                e.consume();
            }
        });

        // Mouse routing on overlay
        doc.getOverlay().addEventHandler(MouseEvent.MOUSE_PRESSED,  e -> {
            if (e.isPrimaryButtonDown()) {
                drawingActive = true;
                scroller.setPannable(false);
            }
            if (floatingActive) {
                // start dragging the floating image
                dragOffsetX = e.getX() - floatX;
                dragOffsetY = e.getY() - floatY;
            } else if (tool != null) tool.onPressed(e, doc);
            e.consume();
        });

        doc.getOverlay().addEventHandler(MouseEvent.MOUSE_DRAGGED,  e -> {
            if (floatingActive) {
                floatX = e.getX() - dragOffsetX;
                floatY = e.getY() - dragOffsetY;
                floatingView.setTranslateX(floatX);
                floatingView.setTranslateY(floatY);
            } else if (tool != null) tool.onDragged(e, doc);
            e.consume();
        });

        doc.getOverlay().addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (floatingActive) {
                // Commit the floating buffer into the backing image
                int w = (int) Math.ceil(floatingImage.getWidth());
                int h = (int) Math.ceil(floatingImage.getHeight());
                Rectangle2D dirty = new Rectangle2D(
                        Math.max(0, floatX), Math.max(0, floatY), w, h);
                WritableImage img = floatingImage; // capture
                double x = floatX, y = floatY;

                doc.commit(gc -> {
                    gc.drawImage(img, x, y);
                }, dirty);

                // remove floating
                floatingView.setImage(null);
                floatingImage = null;
                floatingActive = false;
            } else if (tool != null) tool.onReleased(e, doc);

            if (drawingActive) {
                drawingActive = false;
                scroller.setPannable(true);
            }
            e.consume();
        });

        doc.getOverlay().addEventHandler(MouseEvent.MOUSE_EXITED, e -> e.consume());
    }

    public void setTool(Tool t){ this.tool = t; }
    public StackPane getRoot(){ return root; }

    //Zoom
    public DoubleProperty zoomProperty() { return zoom; }
    public double getZoom() { return zoom.get(); }

    public void setZoom(double z){ zoom.set(Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, z))); applyScale(); }
    public void zoomIn(){ setZoom(getZoom() * ZOOM_STEP); }
    public void zoomOut(){ setZoom(getZoom() / ZOOM_STEP); }
    public void resetZoom(){ setZoom(1.0); }

    private void applyScale(){
        zoomGroup.setScaleX(zoom.get());
        zoomGroup.setScaleY(zoom.get());
    }

    //Floating buffer
    public void beginFloating(WritableImage img, double x, double y){
        if (img == null) return;
        this.floatingImage = img;
        this.floatX = x; this.floatY = y;
        floatingView.setImage(img);
        floatingView.setTranslateX(x);
        floatingView.setTranslateY(y);
        this.floatingActive = true;
    }
    public boolean isFloatingActive(){ return floatingActive; }
    public void cancelFloating(){
        floatingView.setImage(null);
        floatingImage = null;
        floatingActive = false;
    }

    // Utility for tools
    public static void applyStrokeStyle(GraphicsContext g, ImageDocument d){
        g.setStroke(d.strokeColorProperty().get());
        g.setLineWidth(d.strokeWidthProperty().get());
        g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        g.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        if (d.dashedProperty().get()) g.setLineDashes(8,6); else g.setLineDashes(null);
    }
    public static Rectangle2D dirtyFrom(double x1,double y1,double x2,double y2,int pad){
        double minX = Math.min(x1,x2)-pad, minY=Math.min(y1,y2)-pad;
        double w = Math.abs(x1-x2)+2*pad, h=Math.abs(y1-y2)+2*pad;
        return new Rectangle2D(Math.max(0,minX), Math.max(0,minY), w, h);
    }
}
