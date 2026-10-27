package io.github.lukmnh.toolbox.pdf.sign.presentation;

import io.github.lukmnh.toolbox.common.i18n.Messages;
import io.github.lukmnh.toolbox.pdf.sign.domain.InkColor;
import io.github.lukmnh.toolbox.pdf.sign.domain.SignatureStroke;
import io.github.lukmnh.toolbox.pdf.sign.domain.StrokePoint;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Window;

/**
 * Kanvas untuk menggambar tanda tangan dengan mouse. Garis dihaluskan dengan kurva quadratic melalui titik
 * tengah, sama seperti {@link io.github.lukmnh.toolbox.pdf.sign.domain.SignatureRasterizer}.
 */
final class SignatureDialog extends Dialog<List<SignatureStroke>> {

    private static final double CANVAS_WIDTH = 640;
    private static final double CANVAS_HEIGHT = 260;
    private static final double MIN_PEN = 1;
    private static final double MAX_PEN = 8;
    private static final double DEFAULT_PEN = 2.5;
    private static final double SPACING = 10;

    private final Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
    private final List<SignatureStroke> strokes = new ArrayList<>();
    private final SimpleBooleanProperty empty = new SimpleBooleanProperty(true);
    private final Slider penWidth = new Slider(MIN_PEN, MAX_PEN, DEFAULT_PEN);
    private InkColor inkColor = InkColor.BLACK;
    private List<StrokePoint> currentStroke;

    SignatureDialog(Window owner) {
        initOwner(owner);
        setTitle(Messages.get("pdf.sign.draw.title"));
        setHeaderText(Messages.get("pdf.sign.draw.header"));

        canvas.getStyleClass().add("signature-canvas");
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onPressed);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onDragged);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::onReleased);
        redraw();

        VBox content = new VBox(SPACING, controls(), canvas);
        content.setPadding(new Insets(SPACING));
        getDialogPane().setContent(content);

        ButtonType save = new ButtonType(Messages.get("pdf.sign.draw.save"), ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        Node saveButton = getDialogPane().lookupButton(save);
        saveButton.disableProperty().bind(empty);
        setResultConverter(button -> button == save ? List.copyOf(strokes) : null);
    }

    private HBox controls() {
        ToggleGroup colors = new ToggleGroup();
        ToggleButton black = colorButton("pdf.sign.draw.black", InkColor.BLACK, colors);
        colorButton("pdf.sign.draw.blue", InkColor.BLUE, colors);
        colors.selectToggle(black);
        colors.selectedToggleProperty().addListener((observable, previous, selected) -> {
            if (selected == null) {
                // Selalu ada satu warna aktif
                colors.selectToggle(previous);
                return;
            }
            inkColor = (InkColor) selected.getUserData();
        });

        Button clear = new Button(Messages.get("pdf.sign.draw.clear"));
        clear.setOnAction(event -> {
            strokes.clear();
            empty.set(true);
            redraw();
        });

        HBox row = new HBox(SPACING);
        row.setAlignment(Pos.CENTER_LEFT);
        colors.getToggles().forEach(toggle -> row.getChildren().add((ToggleButton) toggle));
        row.getChildren().addAll(new Label(Messages.get("pdf.sign.draw.pen")), penWidth, clear);
        return row;
    }

    private static ToggleButton colorButton(String key, InkColor color, ToggleGroup group) {
        ToggleButton button = new ToggleButton(Messages.get(key));
        button.setUserData(color);
        button.setToggleGroup(group);
        return button;
    }

    private void onPressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        currentStroke = new ArrayList<>();
        currentStroke.add(new StrokePoint(event.getX(), event.getY()));
        redraw();
    }

    private void onDragged(MouseEvent event) {
        if (currentStroke == null) {
            return;
        }
        currentStroke.add(new StrokePoint(event.getX(), event.getY()));
        redraw();
    }

    private void onReleased(MouseEvent event) {
        if (currentStroke == null) {
            return;
        }
        strokes.add(new SignatureStroke(currentStroke, inkColor, penWidth.getValue()));
        currentStroke = null;
        empty.set(false);
        redraw();
    }

    private void redraw() {
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.setFill(Color.WHITE);
        graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        graphics.setLineCap(StrokeLineCap.ROUND);
        graphics.setLineJoin(StrokeLineJoin.ROUND);
        strokes.forEach(stroke -> drawStroke(graphics, stroke.points(), stroke.color(), stroke.width()));
        if (currentStroke != null) {
            drawStroke(graphics, currentStroke, inkColor, penWidth.getValue());
        }
    }

    private static void drawStroke(GraphicsContext graphics, List<StrokePoint> points, InkColor ink, double width) {
        Color color = Color.rgb((ink.rgb() >> 16) & 0xFF, (ink.rgb() >> 8) & 0xFF, ink.rgb() & 0xFF);
        StrokePoint first = points.getFirst();
        if (points.size() == 1) {
            graphics.setFill(color);
            graphics.fillOval(first.x() - width / 2, first.y() - width / 2, width, width);
            return;
        }
        graphics.setStroke(color);
        graphics.setLineWidth(width);
        graphics.beginPath();
        graphics.moveTo(first.x(), first.y());
        for (int index = 1; index < points.size() - 1; index++) {
            StrokePoint control = points.get(index);
            StrokePoint end = control.midpointTo(points.get(index + 1));
            graphics.quadraticCurveTo(control.x(), control.y(), end.x(), end.y());
        }
        StrokePoint last = points.getLast();
        graphics.lineTo(last.x(), last.y());
        graphics.stroke();
    }
}
