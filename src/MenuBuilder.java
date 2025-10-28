import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Locale;

public class MenuBuilder {

    public static MenuBar build(PaintFXApp app, Stage stage, TabPane tabs) {
        //File
        Menu file = new Menu("File");
        MenuItem New = new MenuItem("New...");
        MenuItem Open = new MenuItem("Open...");
        MenuItem Save = new MenuItem("Save");
        MenuItem SaveAs = new MenuItem("Save As...");
        MenuItem ExportAs = new MenuItem("Export As...");
        MenuItem CloseTab = new MenuItem("Close Tab");
        MenuItem Exit = new MenuItem("Exit");

        New.setOnAction(e -> handleNew(app));
        Open.setOnAction(e -> handleOpen(app));
        Save.setOnAction(e -> handleSave(app, stage, tabs));
        SaveAs.setOnAction(e -> handleSaveAs(app, stage, tabs));
        ExportAs.setOnAction(e -> handleExportAs(app, stage, tabs));
        CloseTab.setOnAction(e -> handleCloseTab(app, stage, tabs));
        Exit.setOnAction(e -> { if (tryCloseAll(app, stage, tabs)) stage.close(); });

        // Accelerators
        New.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
        Open.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        Save.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        SaveAs.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        ExportAs.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN));
        CloseTab.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));

        // Autosave submenu
        Menu autosaveMenu = new Menu("Autosave");
        CheckMenuItem enableAutosave = new CheckMenuItem("Enable Autosave");
        CheckMenuItem showCountdown = new CheckMenuItem("Show Countdown");
        MenuItem intervalItem = new MenuItem("Set Interval…");
        CheckMenuItem notifyItem = new CheckMenuItem("Notifications");

        // handlers
        enableAutosave.setOnAction(e -> {
            boolean on = enableAutosave.isSelected();
            app.getAutosaveService().setEnabled(on);
            app.getLogger().log("Autosave " + (on ? "Enabled" : "Disabled"));
            if (on) app.getNotificationService().notifyAutosaveEnabled();
            else    app.getNotificationService().notifyAutosaveDisabled();
        });

        showCountdown.setOnAction(e -> {
            boolean show = showCountdown.isSelected();
            app.getAutosaveService().setShowCountdown(show);
            app.getLogger().log("Autosave countdown " + (show ? "Shown" : "Hidden"));
        });

        intervalItem.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog(String.valueOf(app.getAutosaveService().getIntervalSeconds()/60));
            dlg.setHeaderText("Autosave interval (minutes)");
            dlg.setContentText("Minutes:");
            dlg.showAndWait().ifPresent(s -> {
                try {
                    int mins = Integer.parseInt(s.trim());
                    app.getAutosaveService().setIntervalSeconds(Math.max(1, mins) * 60);
                    app.getLogger().log("Autosave interval set to " + Math.max(1, mins) + " min");
                } catch (Exception ignored) {}
            });
        });

// NEW: notifications toggle handler
        notifyItem.setOnAction(e -> {
            boolean on = notifyItem.isSelected();
            app.getNotificationService().setEnabled(on);
            app.getLogger().log("Notifications " + (on ? "Enabled" : "Disabled"));
            if (on) app.getNotificationService().show("Notifications", "Enabled");
        });

        // reflect current state when menu opens
        autosaveMenu.setOnShowing(e -> {
            enableAutosave.setSelected(app.getAutosaveService().isEnabled());
            showCountdown.setSelected(app.getAutosaveService().isShowCountdown());
            notifyItem.setSelected(app.getNotificationService().isEnabled());
        });
        // add items to the submenu
        autosaveMenu.getItems().addAll(enableAutosave, showCountdown, intervalItem, new SeparatorMenuItem(), notifyItem);

        file.getItems().addAll(
                New, Open, Save, SaveAs, ExportAs,
                new SeparatorMenuItem(),
                CloseTab,
                new SeparatorMenuItem(),
                autosaveMenu,
                new SeparatorMenuItem(),
                Exit
        );

        //Edit
        Menu edit = new Menu("Edit");
        MenuItem Undo = new MenuItem("Undo");
        MenuItem Redo = new MenuItem("Redo");
        MenuItem Copy = new MenuItem("Copy");
        MenuItem Cut  = new MenuItem("Cut");
        MenuItem Paste= new MenuItem("Paste");
        MenuItem MoveSel = new MenuItem("Move Selection");
        MenuItem DeleteSel = new MenuItem("Delete Selection");
        MenuItem SelectAll = new MenuItem("Select All");
        MenuItem Deselect  = new MenuItem("Deselect");
        MenuItem Clear = new MenuItem("Clear Canvas…");

        Undo.setOnAction(e -> handleUndo(tabs));
        Redo.setOnAction(e -> handleRedo(tabs));
        Copy.setOnAction(e -> handleCopy(app, tabs));
        Cut.setOnAction(e -> handleCut(app, tabs));
        Paste.setOnAction(e -> handlePaste(app, tabs));
        MoveSel.setOnAction(e -> handleMoveSelection(app, tabs));
        DeleteSel.setOnAction(e -> handleDeleteSelection(app, tabs));
        SelectAll.setOnAction(e -> handleSelectAll(app, tabs));
        Deselect.setOnAction(e -> handleDeselect(app, tabs));
        Clear.setOnAction(e -> handleClearCanvas(app, tabs));

        Undo.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN));
        Redo.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN));
        Copy.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));
        Cut.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN));
        Paste.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN));
        MoveSel.setAccelerator(new KeyCodeCombination(KeyCode.M, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        DeleteSel.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        SelectAll.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN));
        Deselect.setAccelerator(new KeyCodeCombination(KeyCode.ESCAPE));

        //Transform Menu
        Menu transform = new Menu("Transform");
        MenuItem Rot90  = new MenuItem("Rotate 90° (CW)");
        MenuItem Rot180 = new MenuItem("Rotate 180°");
        MenuItem Rot270 = new MenuItem("Rotate 270° (CCW)");
        MenuItem FlipH  = new MenuItem("Flip Horizontal");
        MenuItem FlipV  = new MenuItem("Flip Vertical");

        Rot90.setOnAction(e  -> handleTransform(app, tabs, TransformOp.ROT_90));
        Rot180.setOnAction(e -> handleTransform(app, tabs, TransformOp.ROT_180));
        Rot270.setOnAction(e -> handleTransform(app, tabs, TransformOp.ROT_270));
        FlipH.setOnAction(e  -> handleTransform(app, tabs, TransformOp.FLIP_H));
        FlipV.setOnAction(e  -> handleTransform(app, tabs, TransformOp.FLIP_V));

        Rot90.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN));
        Rot180.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        Rot270.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN));
        FlipH.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN));
        FlipV.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));

        transform.getItems().addAll(Rot90, Rot180, Rot270, new SeparatorMenuItem(), FlipH, FlipV);

        edit.getItems().addAll(
                Undo, Redo,
                new SeparatorMenuItem(),
                Copy, Cut, Paste, MoveSel, DeleteSel,
                new SeparatorMenuItem(),
                SelectAll, Deselect,
                new SeparatorMenuItem(),
                transform,
                new SeparatorMenuItem(),
                Clear
        );

        //Tools
        Menu tools = buildToolsMenu(app, tabs);

        //View
        Menu view = buildViewMenu(app);

        //Help
        Menu help = new Menu("Help");
        MenuItem Help = new MenuItem("Help...");
        MenuItem About = new MenuItem("About...");
        Help.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION,
                "Transform:\n" +
                        "  Edit → Transform → Rotate 90/180/270, Flip H/V.\n" +
                        "  With a selection: transforms the selection as a floating piece.\n" +
                        "Shortcuts:\n" +
                        "  File: Ctrl+N, Ctrl+O, Ctrl+S, Ctrl+Shift+S, Ctrl+E, Ctrl+W\n" +
                        "  Edit: Ctrl+Z, Ctrl+Y, Ctrl+C, Ctrl+X, Ctrl+V, Delete, Ctrl+A, Esc, Ctrl+Shift+M\n" +
                        "  Transform: Ctrl+R (90°), Ctrl+Shift+R (180°), Ctrl+Alt+R (270°), Ctrl+H / Ctrl+Shift+H (Flip)\n" +
                        "  View: Ctrl+=, Ctrl-, Ctrl+0\n").showAndWait());
        About.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION,
                "PaintFX \nAuthor: Isaiah C\nJavaFX + ImageIO").showAndWait());
        help.getItems().addAll(Help, About);

        return new MenuBar(file, edit, tools, view, help);
    }

    private static Menu buildViewMenu(PaintFXApp app){
        Menu view = new Menu("View");
        MenuItem ZoomIn = new MenuItem("Zoom In");
        MenuItem ZoomOut = new MenuItem("Zoom Out");
        MenuItem ResetZoom = new MenuItem("Reset Zoom");
        ZoomIn.setOnAction(e -> { DocumentView v = app.currentView(); if (v != null) v.zoomIn(); });
        ZoomOut.setOnAction(e -> { DocumentView v = app.currentView(); if (v != null) v.zoomOut(); });
        ResetZoom.setOnAction(e -> { DocumentView v = app.currentView(); if (v != null) v.resetZoom(); });
        ZoomIn.setAccelerator(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN));
        ZoomOut.setAccelerator(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN));
        ResetZoom.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN));
        view.getItems().addAll(ZoomIn, ZoomOut, ResetZoom);
        return view;
    }

    private static Menu buildToolsMenu(PaintFXApp app, TabPane tabs){
        Menu tools = new Menu("Tools");

        ToggleGroup toolMenuGroup = new ToggleGroup();
        RadioMenuItem mSelect  = new RadioMenuItem("Selection");
        RadioMenuItem mPencil  = new RadioMenuItem("Pencil");
        RadioMenuItem mEraser  = new RadioMenuItem("Eraser");
        RadioMenuItem mLine    = new RadioMenuItem("Line");
        RadioMenuItem mRect    = new RadioMenuItem("Rectangle");
        RadioMenuItem mEllipse = new RadioMenuItem("Ellipse");
        RadioMenuItem mTri     = new RadioMenuItem("Triangle");
        RadioMenuItem mPoly    = new RadioMenuItem("Polygon");
        RadioMenuItem mText    = new RadioMenuItem("Text");
        RadioMenuItem mDropper = new RadioMenuItem("Eyedropper");
        RadioMenuItem mStar = new RadioMenuItem("Star");

        mSelect.setToggleGroup(toolMenuGroup);
        mPencil.setToggleGroup(toolMenuGroup);
        mEraser.setToggleGroup(toolMenuGroup);
        mLine.setToggleGroup(toolMenuGroup);
        mRect.setToggleGroup(toolMenuGroup);
        mEllipse.setToggleGroup(toolMenuGroup);
        mTri.setToggleGroup(toolMenuGroup);
        mPoly.setToggleGroup(toolMenuGroup);
        mText.setToggleGroup(toolMenuGroup);
        mDropper.setToggleGroup(toolMenuGroup);
        mStar.setToggleGroup(toolMenuGroup);

        mSelect.setOnAction(e -> app.setToolByName("SELECT"));
        mPencil.setOnAction(e -> app.setToolByName("PENCIL"));
        mEraser.setOnAction(e -> app.setToolByName("ERASER"));
        mLine.setOnAction(e -> app.setToolByName("LINE"));
        mRect.setOnAction(e -> app.setToolByName("RECT"));
        mEllipse.setOnAction(e -> app.setToolByName("ELLIPSE"));
        mTri.setOnAction(e -> app.setToolByName("TRIANGLE"));
        mPoly.setOnAction(e -> app.setToolByName("POLYGON"));
        mText.setOnAction(e -> app.setToolByName("TEXT"));
        mDropper.setOnAction(e -> app.setToolByName("EYEDROPPER"));
        mStar.setOnAction(e -> app.setToolByName("STAR"));


        Menu shapes = new Menu("Shapes");
        shapes.getItems().addAll(mRect, mEllipse, mTri, mPoly, mStar);

        CheckMenuItem dashedItem = new CheckMenuItem("Dashed Outline");
        dashedItem.setOnAction(e -> {
            ImageDocument doc = currentDoc(tabs);
            if (doc != null) doc.dashedProperty().set(dashedItem.isSelected());
        });

        MenuItem textSizeItem = new MenuItem("Text Size...");
        textSizeItem.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog(String.valueOf(app.getTextSize()));
            dlg.setHeaderText("Set text size (6–200 px)");
            dlg.setContentText("Size:");
            dlg.showAndWait().ifPresent(s -> {
                try { app.setTextSize(Integer.parseInt(s.trim())); } catch (Exception ignored) {}
            });
        });

        MenuItem polySides = new MenuItem("Polygon Sides...");
        polySides.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog(String.valueOf(app.getPolygonSides()));
            dlg.setHeaderText("Set number of sides (3–64)");
            dlg.setContentText("Sides:");
            dlg.showAndWait().ifPresent(s -> {
                try { app.setPolygonSides(Integer.parseInt(s.trim())); } catch (Exception ignored) {}
            });
        });

        //star points
        MenuItem starPointsItem = new MenuItem("Star Points...");
        starPointsItem.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog(String.valueOf(app.getStarPoints()));
            dlg.setHeaderText("Set star points (4–64)");
            dlg.setContentText("Points:");
            dlg.showAndWait().ifPresent(s -> {
                try { app.setStarPoints(Integer.parseInt(s.trim())); } catch (Exception ignored) {}
            });
        });

        // Web Share…
        MenuItem webShare = new MenuItem("Web Share…");
        webShare.setOnAction(e -> handleWebShareDialog(app, tabs));

        tools.setOnShowing(e -> {
            String tool = app.getCurrentToolName();
            mSelect.setSelected("SELECT".equals(tool));
            mPencil.setSelected("PENCIL".equals(tool));
            mEraser.setSelected("ERASER".equals(tool));
            mLine.setSelected("LINE".equals(tool));
            mRect.setSelected("RECT".equals(tool));
            mEllipse.setSelected("ELLIPSE".equals(tool));
            mTri.setSelected("TRIANGLE".equals(tool));
            mPoly.setSelected("POLYGON".equals(tool));
            mText.setSelected("TEXT".equals(tool));
            mDropper.setSelected("EYEDROPPER".equals(tool));
            mStar.setSelected("STAR".equals(tool));

            ImageDocument doc = currentDoc(tabs);
            dashedItem.setSelected(doc != null && doc.dashedProperty().get());
        });

        tools.getItems().addAll(
                mSelect, mPencil, mEraser, mLine,
                new SeparatorMenuItem(),
                shapes,
                new SeparatorMenuItem(),
                mText, mDropper,
                new SeparatorMenuItem(),
                dashedItem, textSizeItem, polySides,
                new SeparatorMenuItem(),
                starPointsItem,
                new SeparatorMenuItem(),
                webShare
        );
        int sz = 16;
        mSelect.setGraphic(new javafx.scene.image.ImageView(IconFactory.marquee(sz)));
        mPencil.setGraphic(new javafx.scene.image.ImageView(IconFactory.pencil(sz)));
        mEraser.setGraphic(new javafx.scene.image.ImageView(IconFactory.eraser(sz)));
        mLine.setGraphic(new javafx.scene.image.ImageView(IconFactory.line(sz)));
        mRect.setGraphic(new javafx.scene.image.ImageView(IconFactory.rect(sz)));
        mEllipse.setGraphic(new javafx.scene.image.ImageView(IconFactory.ellipse(sz)));
        mTri.setGraphic(new javafx.scene.image.ImageView(IconFactory.triangle(sz)));
        mPoly.setGraphic(new javafx.scene.image.ImageView(IconFactory.polygon(sz)));
        mText.setGraphic(new javafx.scene.image.ImageView(IconFactory.text(sz)));
        mDropper.setGraphic(new javafx.scene.image.ImageView(IconFactory.eyedropper(sz)));
        mStar.setGraphic(new javafx.scene.image.ImageView(IconFactory.star(sz)));

        return tools;
    }

    //File/Edit plumbing
    public static void handleNew(PaintFXApp app) {
        TextInputDialog w = new TextInputDialog("800"); w.setHeaderText("New Image Width (px)");
        TextInputDialog h = new TextInputDialog("600"); h.setHeaderText("New Image Height (px)");
        w.showAndWait(); h.showAndWait();
        int wi = parseOr(w.getResult(), 800), hi = parseOr(h.getResult(), 600);
        app.addNewDocument(wi, hi);
    }

    public static void handleOpen(PaintFXApp app) {
        try {
            ImageDocument doc = FileService.open(app.stage());
            if (doc != null) { app.addDocument(doc); app.getLogger().log(doc, "Open"); }
        } catch (Exception ex) { showErr("Open failed: " + ex.getMessage()); }
    }

    public static void handleSave(PaintFXApp app, Stage owner, TabPane tabs) {
        Tab t = tabs.getSelectionModel().getSelectedItem(); if (t == null) return;
        ImageDocument doc = (ImageDocument) t.getUserData();
        try {
            if (!FileService.save(owner, doc)) showInfo("Save canceled.");
            else {
                t.setText(doc.getDisplayName());
                if (app.getAutosaveService().isEnabled()) app.getAutosaveService().resetCountdown();
                app.getLogger().log(doc, "Save");
            }
        } catch (Exception ex) { showErr("Save failed: " + ex.getMessage()); }
    }

    public static void handleSaveAs(PaintFXApp app, Stage owner, TabPane tabs) {
        Tab t = tabs.getSelectionModel().getSelectedItem(); if (t == null) return;
        ImageDocument doc = (ImageDocument) t.getUserData();
        try {
            if (!FileService.saveAs(owner, doc)) showInfo("Save canceled.");
            else {
                t.setText(doc.getDisplayName());
                if (app.getAutosaveService().isEnabled()) app.getAutosaveService().resetCountdown();
                app.getLogger().log(doc, "Save As " + (doc.getFile()!=null ? doc.getFile().getName() : ""));
            }
        } catch (Exception ex) { showErr("Save As failed: " + ex.getMessage()); }
    }

    public static void handleCloseTab(PaintFXApp app, Stage owner, TabPane tabs) {
        Tab t = tabs.getSelectionModel().getSelectedItem();
        if (t == null) return;
        if (tryCloseDoc(app, owner, tabs, t)) tabs.getTabs().remove(t);
    }

    public static boolean tryCloseDoc(PaintFXApp app, Stage owner, TabPane tabs, Tab t) {
        ImageDocument doc = (ImageDocument) t.getUserData();
        if (doc == null || !doc.isDirty()) return true;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Close without saving changes to " + doc.getDisplayName() + "?",
                ButtonType.CANCEL, ButtonType.NO, ButtonType.YES);
        a.setHeaderText("Unsaved changes");
        a.showAndWait();
        if (a.getResult() == ButtonType.YES) return true;
        if (a.getResult() == ButtonType.NO) { handleSave(app, owner, tabs); return !doc.isDirty(); }
        return false;
    }

    public static boolean tryCloseAll(PaintFXApp app, Stage owner, TabPane tabs) {
        for (Tab t : tabs.getTabs()) if (!tryCloseDoc(app, owner, tabs, t)) return false;
        return true;
    }

    //Export As
    private static void handleExportAs(PaintFXApp app, Stage owner, TabPane tabs) {
        Tab t = tabs.getSelectionModel().getSelectedItem();
        if (t == null) return;
        ImageDocument doc = (ImageDocument) t.getUserData();
        if (doc == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Export Image As");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG (*.png)", "*.png"),
                new FileChooser.ExtensionFilter("JPEG (*.jpg;*.jpeg)", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("BMP (*.bmp)", "*.bmp")
        );
        fc.setSelectedExtensionFilter(fc.getExtensionFilters().get(0));
        fc.setInitialFileName(suggestName(doc));

        File chosen = fc.showSaveDialog(owner);
        if (chosen == null) return;

        FileChooser.ExtensionFilter sel = fc.getSelectedExtensionFilter();
        String ext = extensionOf(chosen);
        if (ext.isEmpty() && sel != null) {
            String def = defaultExtFor(sel);
            chosen = new File(chosen.getParentFile(), chosen.getName() + "." + def);
            ext = def;
        }
        String fmt = ext.toLowerCase(Locale.ROOT);

        if (ExportUtil.formatDropsAlpha(fmt) && ExportUtil.hasTransparency(doc.getBacking())) {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                    "Exporting to " + fmt.toUpperCase() + " will remove transparency.\nProceed?",
                    ButtonType.CANCEL, ButtonType.OK);
            a.setHeaderText("Possible Data Loss");
            a.showAndWait();
            if (a.getResult() != ButtonType.OK) return;
        }

        try {
            FileService.writeToFile(doc, chosen);
            showInfo("Exported to:\n" + chosen.getAbsolutePath());
            app.getLogger().log(doc, "Export " + chosen.getName());
        } catch (Exception ex) {
            showErr("Export failed: " + ex.getMessage());
        }
    }

    //Transform handling
    private enum TransformOp { ROT_90, ROT_180, ROT_270, FLIP_H, FLIP_V }

    private static void handleTransform(PaintFXApp app, TabPane tabs, TransformOp op) {
        ImageDocument doc = currentDoc(tabs); if (doc == null) return;
        DocumentView view = app.currentView(); if (view == null) return;

        Rectangle2D sel = doc.getSelection();

        if (sel != null && sel.getWidth() >= 1 && sel.getHeight() >= 1) {
            //Selection-only: crop -> transform -> floating piece
            WritableImage crop = new WritableImage(
                    doc.getBacking().getPixelReader(),
                    (int) sel.getMinX(), (int) sel.getMinY(),
                    (int) sel.getWidth(), (int) sel.getHeight());

            WritableImage transformed = applyTransform(crop, op);
            double x = sel.getMinX(), y = sel.getMinY();

            doc.clearOverlay();
            view.beginFloating(transformed, x, y);

            app.getLogger().log(doc, labelOf(op) + " (selection)");
        } else {
            int w = (int) Math.round(doc.getBacking().getWidth());
            int h = (int) Math.round(doc.getBacking().getHeight());

            switch (op) {
                case ROT_90:
                case ROT_270: {
                    // Rotate in SAME TAB by replacing the document under the hood
                    Tab oldTab = tabs.getSelectionModel().getSelectedItem();
                    if (oldTab == null) return;

                    WritableImage transformed = applyTransform(doc.getBacking(), op);

                    // New document from rotated image (inherit file if you prefer)
                    ImageDocument nd = ImageDocument.fromImage(transformed, /* keep file? */ doc.getFile());

                    // Add the new one (becomes selected)
                    app.addDocument(nd);

                    // Keep title continuity
                    Tab newTab = tabs.getSelectionModel().getSelectedItem();
                    if (newTab != null) newTab.setText(oldTab.getText());

                    // Remove the old one so user still sees one tab
                    tabs.getTabs().remove(oldTab);

                    app.getLogger().log(doc, labelOf(op) + " (canvas)");
                    break;
                }
                case ROT_180:
                case FLIP_H:
                case FLIP_V: {
                    WritableImage transformed = applyTransform(doc.getBacking(), op);
                    Rectangle2D dirty = new Rectangle2D(0, 0, w, h);
                    doc.commit(gc -> {
                        gc.setFill(Color.WHITE);
                        gc.fillRect(0, 0, w, h);
                        gc.drawImage(transformed, 0, 0);
                    }, dirty);
                    doc.clearOverlay();
                    doc.redrawBase();
                    app.getLogger().log(doc, labelOf(op) + " (canvas)");
                    break;
                }
            }
        }
    }

    private static WritableImage applyTransform(Image src, TransformOp op) {
        switch (op) {
            case ROT_90:  return ImageTransforms.rotate90(src);
            case ROT_180: return ImageTransforms.rotate180(src);
            case ROT_270: return ImageTransforms.rotate270(src);
            case FLIP_H:  return ImageTransforms.flipHorizontal(src);
            case FLIP_V:  return ImageTransforms.flipVertical(src);
            default:      return ImageTransforms.rotate180(src);
        }
    }

    private static String labelOf(TransformOp op){
        switch (op){
            case ROT_90:  return "Rotate 90°";
            case ROT_180: return "Rotate 180°";
            case ROT_270: return "Rotate 270°";
            case FLIP_H:  return "Flip Horizontal";
            case FLIP_V:  return "Flip Vertical";
            default:      return "Transform";
        }
    }

    //Selection ops (with logging)
    public static void handleUndo(TabPane tabs) {
        ImageDocument doc = currentDoc(tabs);
        if (doc != null) doc.undo();
    }
    public static void handleRedo(TabPane tabs) {
        ImageDocument doc = currentDoc(tabs);
        if (doc != null) doc.redo();
    }

    private static void handleCopy(PaintFXApp app, TabPane tabs){
        ImageDocument doc = currentDoc(tabs); if (doc == null) return;
        Rectangle2D r = doc.getSelection();   if (r == null || r.getWidth()<1 || r.getHeight()<1) return;
        WritableImage clip = new WritableImage(doc.getBacking().getPixelReader(),
                (int) r.getMinX(), (int) r.getMinY(), (int) r.getWidth(), (int) r.getHeight());
        doc.setClipboard(clip);
        app.getLogger().log(doc, "Copy");
    }
    private static void handleCut(PaintFXApp app, TabPane tabs){
        ImageDocument doc = currentDoc(tabs); if (doc == null) return;
        Rectangle2D r = doc.getSelection();   if (r == null || r.getWidth()<1 || r.getHeight()<1) return;
        WritableImage clip = new WritableImage(doc.getBacking().getPixelReader(),
                (int) r.getMinX(), (int) r.getMinY(), (int) r.getWidth(), (int) r.getHeight());
        doc.setClipboard(clip);
        Rectangle2D dirty = new Rectangle2D(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight());
        doc.commit(gc -> { gc.setFill(Color.WHITE); gc.fillRect(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight()); }, dirty);
        doc.clearSelection(); doc.clearOverlay(); doc.redrawBase();
        app.getLogger().log(doc, "Cut");
    }
    private static void handlePaste(PaintFXApp app, TabPane tabs){
        ImageDocument doc = currentDoc(tabs); if (doc == null) return;
        WritableImage clip = doc.getClipboard(); if (clip == null) return;
        DocumentView v = app.currentView(); if (v == null) return;
        Rectangle2D r = doc.getSelection();
        double x = (r != null) ? r.getMinX() : 50, y = (r != null) ? r.getMinY() : 50;
        v.beginFloating(clip, x, y);
        app.getLogger().log(doc, "Paste");
    }
    private static void handleMoveSelection(PaintFXApp app, TabPane tabs){
        ImageDocument doc = currentDoc(tabs); if (doc == null) return;
        Rectangle2D r = doc.getSelection();   if (r == null || r.getWidth()<1 || r.getHeight()<1) return;
        DocumentView v = app.currentView();   if (v == null) return;
        WritableImage clip = new WritableImage(doc.getBacking().getPixelReader(),
                (int) r.getMinX(), (int) r.getMinY(), (int) r.getWidth(), (int) r.getHeight());
        doc.setClipboard(clip);
        Rectangle2D dirty = new Rectangle2D(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight());
        doc.commit(gc -> { gc.setFill(Color.WHITE); gc.fillRect(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight()); }, dirty);
        v.beginFloating(clip, r.getMinX(), r.getMinY());
        app.getLogger().log(doc, "Move Selection");
    }
    private static void handleDeleteSelection(PaintFXApp app, TabPane tabs){
        ImageDocument doc = currentDoc(tabs); if (doc == null) return;
        Rectangle2D r = doc.getSelection();   if (r == null || r.getWidth()<1 || r.getHeight()<1) return;
        Rectangle2D dirty = new Rectangle2D(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight());
        doc.commit(gc -> { gc.setFill(Color.WHITE); gc.fillRect(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight()); }, dirty);
        doc.clearSelection(); doc.clearOverlay(); doc.redrawBase();
        app.getLogger().log(doc, "Delete Selection");
    }
    private static void handleSelectAll(PaintFXApp app, TabPane tabs){
        ImageDocument doc = currentDoc(tabs); if (doc == null) return;
        double w = doc.getBacking().getWidth(), h = doc.getBacking().getHeight();
        Rectangle2D r = new Rectangle2D(0, 0, w, h);
        doc.setSelection(r);
        GraphicsContext g = doc.getOverlay().getGraphicsContext2D();
        doc.clearOverlay();
        g.setLineWidth(1.0); g.setStroke(Color.BLACK); g.setLineDashes(6, 4);
        g.strokeRect(0, 0, w, h); g.setLineDashes(0);
        app.getLogger().log(doc, "Select All");
    }
    private static void handleDeselect(PaintFXApp app, TabPane tabs){
        ImageDocument doc = currentDoc(tabs); if (doc == null) return;
        doc.clearSelection(); doc.clearOverlay();
        app.getLogger().log(doc, "Deselect");
    }

    private static void handleClearCanvas(PaintFXApp app, TabPane tabs){
        ImageDocument doc = currentDoc(tabs);
        if (doc == null) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Clear the entire canvas to white?\nThis can be undone.",
                ButtonType.CANCEL, ButtonType.OK);
        a.setHeaderText("Clear Canvas");
        a.showAndWait();
        if (a.getResult() != ButtonType.OK) return;
        double w = doc.getBacking().getWidth(), h = doc.getBacking().getHeight();
        Rectangle2D dirty = new Rectangle2D(0, 0, w, h);
        doc.commit(gc -> { gc.setFill(Color.WHITE); gc.fillRect(0, 0, w, h); }, dirty);
        doc.clearSelection(); doc.clearOverlay();
        app.getLogger().log(doc, "Clear Canvas");
    }

    //Web Share dialog
    private static void handleWebShareDialog(PaintFXApp app, TabPane tabs) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Web Share");
        ButtonType startBtn = new ButtonType("Start", ButtonBar.ButtonData.OK_DONE);
        ButtonType stopBtn  = new ButtonType("Stop",  ButtonBar.ButtonData.NO);
        ButtonType closeBtn = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dlg.getDialogPane().getButtonTypes().addAll(startBtn, stopBtn, closeBtn);

        VBox box = new VBox(8);
        TextField portField = new TextField(app.isWebServerRunning() ? String.valueOf(app.getWebServer().getPort()) : "8080");
        portField.setPrefColumnCount(6);
        box.getChildren().add(new HBox(6, new Label("Port:"), portField));
        box.getChildren().add(new Separator());

        java.util.ArrayList<CheckBox> checks = new java.util.ArrayList<>();
        for (int i = 0; i < tabs.getTabs().size(); i++) {
            Tab t = tabs.getTabs().get(i);
            String title = t.getText();
            CheckBox cb = new CheckBox(title);
            if (app.isWebServerRunning()) {
                ImageDocument d = (ImageDocument) t.getUserData();
                cb.setSelected(app.getWebServer().getSharedDocs().containsValue(d));
            }
            checks.add(cb);
            box.getChildren().add(cb);
        }
        dlg.getDialogPane().setContent(box);

        java.util.Optional<ButtonType> res = dlg.showAndWait();
        if (!res.isPresent()) return;

        if (res.get() == startBtn) {
            int port = 8080;
            try { port = Integer.parseInt(portField.getText().trim()); } catch (Exception ignored) {}
            java.util.LinkedHashMap<String, ImageDocument> docsById = new java.util.LinkedHashMap<>();
            java.util.LinkedHashMap<String, String> titlesById = new java.util.LinkedHashMap<>();
            for (int i = 0; i < checks.size(); i++) {
                if (checks.get(i).isSelected()) {
                    Tab t = tabs.getTabs().get(i);
                    ImageDocument d = (ImageDocument) t.getUserData();
                    String id = "tab" + i;
                    docsById.put(id, d);
                    titlesById.put(id, t.getText());
                }
            }
            if (docsById.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "No tabs selected to share.").showAndWait();
                return;
            }
            if (app.startWebServer(port, docsById, titlesById)) {
                app.getLogger().log("WebShare start port=" + port + " tabs=" + docsById.size());
                String url = (docsById.size() == 1)
                        ? "http://localhost:" + port + "/img/" + docsById.keySet().iterator().next() + ".png"
                        : "http://localhost:" + port + "/";
                try { app.openURL(url); }
                catch (Exception ex) { new Alert(Alert.AlertType.INFORMATION, "Serving at: " + url).showAndWait(); }
            }
        } else if (res.get() == stopBtn) {
            app.stopWebServer();
            app.getLogger().log("WebShare stop");
            new Alert(Alert.AlertType.INFORMATION, "Web server stopped.").showAndWait();
        }
    }

    //tiny helpers
    private static ImageDocument currentDoc(TabPane tabs) {
        Tab t = tabs.getSelectionModel().getSelectedItem();
        return (t == null) ? null : (ImageDocument) t.getUserData();
    }
    private static String suggestName(ImageDocument doc) {
        File f = doc.getFile();
        if (f != null && f.getName() != null) {
            String n = f.getName();
            int dot = n.lastIndexOf('.');
            return (dot > 0 ? n.substring(0, dot) : n);
        }
        return "Untitled";
    }
    private static String extensionOf(File f) {
        String n = f.getName(); int dot = n.lastIndexOf('.');
        if (dot < 0) return ""; return n.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
    private static String defaultExtFor(FileChooser.ExtensionFilter sel) {
        String desc = sel.getDescription().toLowerCase(Locale.ROOT);
        if (desc.contains("png")) return "png";
        if (desc.contains("jpeg") || desc.contains("jpg")) return "jpg";
        if (desc.contains("bmp")) return "bmp";
        return "png";
    }
    private static int parseOr(String s, int d){ try { return Integer.parseInt(s); } catch(Exception e){ return d; } }
    private static void showErr(String m){ new Alert(Alert.AlertType.ERROR, m).showAndWait(); }
    private static void showInfo(String m){ new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); }
}
