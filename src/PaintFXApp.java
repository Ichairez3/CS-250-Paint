import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PaintFXApp extends Application {
    // UI
    private TabPane tabs;
    private ColorPicker colorPicker;
    private Spinner<Integer> widthSpinner;
    private ToggleGroup toolGroup;
    private CheckBox dashedBox;
    private Label widthLabel, zoomLabel, colorLabel, autosaveLabel;
    private ToggleButton pencilBtn, lineBtn;

    // App state
    private String currentToolName = "PENCIL";
    private int polygonSides = 5;
    private int textSize = 24;
    private boolean suppressToolbarToolListener = false;

    // Services
    private AutosaveService autosave;
    private LocalWebServer webServer = new LocalWebServer();
    private LoggingService logger = new LoggingService();

    //notification system set up
    private NotificationService notifications = new NotificationService();
    public NotificationService getNotificationService() {
        return notifications;
    }
    //star tool
    private int starPoints = 5;
    private double starInnerRatio = 0.5;


    //lifecycle
    @Override
    public void start(Stage stage) {
        tabs = new TabPane();

        // autosave service
        autosave = new AutosaveService(this::performAutosave);
        autosave.setIntervalSeconds(120);
        autosave.setShowCountdown(false);
        autosave.setEnabled(false);

        MenuBar menuBar = MenuBuilder.build(this, stage, tabs);
        ToolBar toolBar = buildToolBar();

        BorderPane root = new BorderPane();
        root.setTop(new VBox(menuBar, new Separator(Orientation.HORIZONTAL), toolBar));
        root.setCenter(tabs);
        root.setBottom(buildStatusBar());

        addNewDocument(800, 600);

        tabs.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> {
            bindZoomLabel();
            bindColorLabel();
        });


        Scene scene = new Scene(root, 1100, 750);
        stage.setTitle("PaintFX");
        stage.setScene(scene);
        stage.setOnCloseRequest(ev -> {
            if (!MenuBuilder.tryCloseAll(this, stage, tabs)) { ev.consume(); return; }
            autosave.shutdown();
            stopWebServer();
            logger.shutdown();
            notifications.shutdown();
        });

        notifications.init(stage);
        stage.show();
    }

    ToolBar buildToolBar() {
        colorPicker = new ColorPicker(javafx.scene.paint.Color.BLACK);
        widthSpinner = new Spinner<>(1, 80, 4);
        widthSpinner.setEditable(true);

        //tool toggles
        pencilBtn = new ToggleButton("Pencil");
        lineBtn   = new ToggleButton("Line");
        pencilBtn.setUserData("PENCIL");
        lineBtn.setUserData("LINE");
        toolGroup = new ToggleGroup();
        pencilBtn.setToggleGroup(toolGroup);
        lineBtn.setToggleGroup(toolGroup);
        pencilBtn.setSelected(true);

        // ICONS + TOOLTIPS
        int sz = 18;
        pencilBtn.setGraphic(new javafx.scene.image.ImageView(IconFactory.pencil(sz)));
        lineBtn.setGraphic(new javafx.scene.image.ImageView(IconFactory.line(sz)));

        pencilBtn.setTooltip(new Tooltip("Pencil (B) — freehand draw"));
        lineBtn.setTooltip(new Tooltip("Line (L) — straight line tool"));

        dashedBox = new CheckBox("Dashed");
        dashedBox.setTooltip(new Tooltip("Toggle dashed stroke for shapes/lines"));

        Button newBtn = new Button("New");
        newBtn.setOnAction(e -> MenuBuilder.handleNew(this));
        newBtn.setTooltip(new Tooltip("New Canvas (Ctrl+N)"));

        Button openBtn = new Button("Open");
        openBtn.setOnAction(e -> MenuBuilder.handleOpen(this));
        openBtn.setTooltip(new Tooltip("Open Image… (Ctrl+O)"));

        HBox box = new HBox(
                10,
                newBtn, openBtn,
                new Separator(),
                pencilBtn, lineBtn,
                new Separator(),
                new Label("Color:"), colorPicker,
                new Label("Width:"), widthSpinner,
                dashedBox
        );
        box.setPadding(new Insets(6));

        toolGroup.selectedToggleProperty().addListener((o, a, b) -> {
            if (suppressToolbarToolListener) return;
            if (b == null) return;
            String type = b.getUserData().toString();
            setToolByName(type);
        });

        return new ToolBar(box);
    }


    HBox buildStatusBar() {
        widthLabel = new Label();
        zoomLabel  = new Label();
        colorLabel = new Label();
        autosaveLabel = new Label();

        widthLabel.setPadding(new Insets(4,10,4,10));
        zoomLabel.setPadding(new Insets(4,10,4,10));
        colorLabel.setPadding(new Insets(4,10,4,10));
        autosaveLabel.setPadding(new Insets(4,10,4,10));

        updateWidthLabel();
        zoomLabel.setText("Zoom: 100%");
        colorLabel.setText("Color: #000000 rgb(0,0,0) Black");

        widthSpinner.valueProperty().addListener((obs, o, n) -> updateWidthLabel());

        autosaveLabel.textProperty().bind(Bindings.createStringBinding(
                () -> autosave.isEnabled()
                        ? "Autosave in: " + formatSeconds(autosave.getRemainingSeconds())
                        : "Autosave: off",
                autosave.enabledProperty(), autosave.remainingSecondsProperty()));
        autosaveLabel.visibleProperty().bind(autosave.showCountdownProperty());
        autosaveLabel.managedProperty().bind(autosave.showCountdownProperty());

        HBox bar = new HBox(
                new Label("  PaintFX ready"),
                new Separator(), widthLabel,
                new Separator(), zoomLabel,
                new Separator(), colorLabel,
                new Separator(), autosaveLabel
        );
        bar.setPadding(new Insets(4));
        return bar;
    }

    private String formatSeconds(int s) {
        int h = s / 3600; s %= 3600;
        int m = s / 60; int sec = s % 60;
        return (h > 0 ? (h + ":" + String.format("%02d", m) + ":" + String.format("%02d", sec))
                : (m + ":" + String.format("%02d", sec)));
    }

    private void updateWidthLabel() {
        widthLabel.setText("Width: " + widthSpinner.getValue() + " px");
    }

    private void bindZoomLabel(){
        zoomLabel.textProperty().unbind();
        DocumentView v = currentView();
        if (v != null) {
            zoomLabel.textProperty().bind(Bindings.createStringBinding(
                    () -> "Zoom: " + Math.round(v.getZoom() * 100) + "%",
                    v.zoomProperty()
            ));
        } else {
            zoomLabel.setText("Zoom: —");
        }
    }

    private void bindColorLabel(){
        colorLabel.textProperty().unbind();
        ImageDocument d = currentDoc();
        if (d != null) {
            colorLabel.textProperty().bind(Bindings.createStringBinding(
                    () -> formatColorInfo(d.strokeColorProperty().get()),
                    d.strokeColorProperty()
            ));
        } else {
            colorLabel.setText("Color: —");
        }
    }

    private static String formatColorInfo(javafx.scene.paint.Color c){
        int r = (int)Math.round(c.getRed()*255);
        int g = (int)Math.round(c.getGreen()*255);
        int b = (int)Math.round(c.getBlue()*255);
        String hex = String.format("#%02X%02X%02X", r,g,b);
        String name = basicColorName(c);
        return "Color: " + hex + " rgb(" + r + "," + g + "," + b + ")" + (name.isEmpty() ? "" : " " + name);
    }
    private static String basicColorName(javafx.scene.paint.Color c){
        if (c.equals(javafx.scene.paint.Color.BLACK)) return "Black";
        if (c.equals(javafx.scene.paint.Color.WHITE)) return "White";
        if (c.equals(javafx.scene.paint.Color.RED)) return "Red";
        if (c.equals(javafx.scene.paint.Color.GREEN)) return "Green";
        if (c.equals(javafx.scene.paint.Color.BLUE)) return "Blue";
        if (c.equals(javafx.scene.paint.Color.CYAN)) return "Cyan";
        if (c.equals(javafx.scene.paint.Color.MAGENTA)) return "Magenta";
        if (c.equals(javafx.scene.paint.Color.YELLOW)) return "Yellow";
        if (c.equals(javafx.scene.paint.Color.GRAY)) return "Gray";
        return "";
    }

    //helpers
    public ImageDocument currentDoc() {
        Tab t = tabs.getSelectionModel().getSelectedItem();
        return (t == null) ? null : (ImageDocument) t.getUserData();
    }
    public DocumentView currentView() {
        Tab t = tabs.getSelectionModel().getSelectedItem();
        return (t == null) ? null : (DocumentView) t.getProperties().get("view");
    }

    public void addDocument(ImageDocument doc) {
        DocumentView view = new DocumentView(doc);
        bindUI(view, doc);

        Tab tab = new Tab(doc.getDisplayName(), view.getRoot());
        tab.setUserData(doc);
        tab.getProperties().put("view", view);
        tab.setOnCloseRequest(ev -> {
            if (!MenuBuilder.tryCloseDoc(this, stage(), tabs, tab)) ev.consume();
            else logger.log(doc, "Close Tab");
        });
        tabs.getTabs().add(tab);
        tabs.getSelectionModel().select(tab);

        setToolByName(currentToolName);
        bindZoomLabel();
        bindColorLabel();

        logger.log(doc, "Opened");
    }

    public void addNewDocument(int w, int h) {
        ImageDocument doc = ImageDocument.newBlank(w, h);
        addDocument(doc);
        logger.log(doc, "New canvas " + w + "x" + h);
    }

    private void bindUI(DocumentView view, ImageDocument doc) {
        colorPicker.valueProperty().bindBidirectional(doc.strokeColorProperty());
        widthSpinner.getValueFactory().valueProperty().bindBidirectional(doc.strokeWidthProperty().asObject());
        dashedBox.selectedProperty().bindBidirectional(doc.dashedProperty());
        setToolByName(currentToolName);
    }

    /** Centralized tool switch used by both toolbar and menus. */
    public void setToolByName(String name) {
        currentToolName = name;
        DocumentView view = currentView();
        if (view == null) return;

        switch (name) {
            case "SELECT":     view.setTool(new SelectionTool()); break;
            case "EYEDROPPER": view.setTool(new EyedropperTool()); break;
            case "TEXT":       view.setTool(new TextTool(() -> getTextSize())); break;
            case "ERASER":     view.setTool(new EraserTool()); break;
            case "LINE":       view.setTool(new LineTool()); break;
            case "RECT":       view.setTool(new RectTool()); break;
            case "ELLIPSE":    view.setTool(new EllipseTool()); break;
            case "TRIANGLE":   view.setTool(new TriangleTool()); break;
            case "POLYGON":    view.setTool(new PolygonTool(() -> getPolygonSides())); break;
            case "STAR":       view.setTool(new StarTool(() -> getStarPoints())); break;
            default:           view.setTool(new PencilTool()); currentToolName = "PENCIL";
        }

        suppressToolbarToolListener = true;
        try {
            if ("PENCIL".equals(currentToolName)) {
                toolGroup.selectToggle(pencilBtn);
            } else if ("LINE".equals(currentToolName)) {
                toolGroup.selectToggle(lineBtn);
            } else {
                toolGroup.selectToggle(null);
            }
        } finally { suppressToolbarToolListener = false; }

        logger.log(currentDoc(), "Tool selected: " + currentToolName);
    }

    public String getCurrentToolName() { return currentToolName; }
    public int getPolygonSides() { return polygonSides; }
    public void setPolygonSides(int n) { polygonSides = Math.max(3, Math.min(64, n)); }
    public int getTextSize() { return textSize; }
    public void setTextSize(int s) { textSize = Math.max(6, Math.min(200, s)); }

    public AutosaveService getAutosaveService() { return autosave; }
    public LoggingService getLogger() { return logger; }

    /** Called by AutosaveService on background thread to write autosaves + log. */
    private void performAutosave() {
        int count = 0;
        for (Tab t : tabs.getTabs()) {
            ImageDocument d = (ImageDocument) t.getUserData();
            if (d == null) continue;
            try { FileService.autosave(d); logger.log(d, "Autosave"); count++; }
            catch (Exception ignored) {}
        }
        if (count == 0) logger.log("Autosave (no documents)");
        notifications.notifyAutosaveFired(count);
    }

    // Web server helpers
    public boolean isWebServerRunning() { return webServer != null && webServer.isRunning(); }
    public LocalWebServer getWebServer() { return webServer; }
    public boolean startWebServer(int port, java.util.LinkedHashMap<String, ImageDocument> docsById,
                                  java.util.LinkedHashMap<String, String> titlesById) {
        try {
            if (webServer == null) webServer = new LocalWebServer();
            webServer.start(port, docsById, titlesById);
            return true;
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Failed to start web server: " + ex.getMessage()).showAndWait();
            return false;
        }
    }
    public void stopWebServer() { if (webServer != null) webServer.stop(); }

    /** Open a URL in the default browser. */
    public void openURL(String url) { getHostServices().showDocument(url); }

    public int getStarPoints() {
        return starPoints;
    }
    public void setStarPoints(int n) {
        this.starPoints = Math.max(4, Math.min(64, n));
    }

    public Stage stage() { return (Stage) tabs.getScene().getWindow(); }
    public static void main(String[] args) { launch(args); }
}
