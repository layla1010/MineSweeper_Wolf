package util;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.PopupWindow;
import javafx.stage.Window;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

final class OnboardingOverlay {

    private final Popup popup = new Popup();

    private final Pane rootLayer = new Pane();
    private final Rectangle dim = new Rectangle();
    private final Rectangle highlight = new Rectangle();
    private final VBox card = new VBox(10);

    private final Label titleLabel = new Label();
    private final Label textLabel = new Label();

    private final Button backBtn = new Button("Back");
    private final Button nextBtn = new Button("Next");
    private final Button skipBtn = new Button("Skip");

    private final Consumer<Void> onFinish;
    private final Runnable onBack;
    private final Runnable onNext;
    private final Runnable onSkip;

    private Scene scene;

    OnboardingOverlay(
            Consumer<Void> onFinish,
            Runnable onBack,
            Runnable onNext,
            Runnable onSkip
    ) {
        this.onFinish = Objects.requireNonNull(onFinish);
        this.onBack = Objects.requireNonNull(onBack);
        this.onNext = Objects.requireNonNull(onNext);
        this.onSkip = Objects.requireNonNull(onSkip);

        buildUI();
    }

    private void buildUI() {
        popup.setAutoHide(false);
        popup.getContent().add(rootLayer);
        popup.setAutoFix(false);
        popup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);


        // Dim layer
        dim.setFill(Color.rgb(0, 0, 0, 0.55));
        dim.setMouseTransparent(false);

        // Highlight
        highlight.setFill(Color.TRANSPARENT);
        highlight.setStroke(Color.rgb(255, 255, 255, 0.95));
        highlight.setStrokeWidth(3);
        highlight.setArcWidth(18);
        highlight.setArcHeight(18);
        highlight.setEffect(new DropShadow(25, Color.rgb(255, 255, 255, 0.55)));
        highlight.setMouseTransparent(true);

        // Card
        card.setStyle(
                "-fx-background-color: rgba(15,23,42,0.92);" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: rgba(255,255,255,0.25);" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 1.2;" +
                "-fx-padding: 14;"
        );
        card.setMaxWidth(360);

        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");
        textLabel.setStyle("-fx-text-fill: rgba(229,231,235,0.95); -fx-font-size: 13;");
        textLabel.setWrapText(true);

        Region spacer = new Region();
        spacer.setMinWidth(10);

        HBox actions = new HBox(10, backBtn, spacer, skipBtn, nextBtn);
        actions.setStyle("-fx-alignment: center-right;");

        backBtn.setStyle(buttonStyle());
        nextBtn.setStyle(buttonStylePrimary());
        skipBtn.setStyle(buttonStyle());

        backBtn.setOnAction(e -> onBack.run());
        nextBtn.setOnAction(e -> onNext.run());
        skipBtn.setOnAction(e -> onSkip.run());

        card.getChildren().addAll(titleLabel, textLabel, actions);

        rootLayer.getChildren().addAll(dim, highlight, card);
    }

    private String buttonStyle() {
        return "-fx-background-radius: 12;" +
               "-fx-background-color: rgba(255,255,255,0.10);" +
               "-fx-text-fill: white;" +
               "-fx-cursor: hand;";
    }

    private String buttonStylePrimary() {
        return "-fx-background-radius: 12;" +
               "-fx-background-color: linear-gradient(#f973c5, #a855f7);" +
               "-fx-text-fill: white;" +
               "-fx-cursor: hand;";
    }

    void show(Window owner, Scene scene) {
        this.scene = scene;
        if (scene == null || scene.getRoot() == null) return;

        // Ensure scene is laid out once
        scene.getRoot().applyCss();
        scene.getRoot().layout();

        // Force correct overlay sizing BEFORE showing
        resizeToScene();

        // Compute content top-left
        Point2D topLeft = scene.getRoot().localToScreen(0, 0);
        if (topLeft == null) return;

        if (!popup.isShowing()) {
            popup.show(owner, topLeft.getX(), topLeft.getY());
        } else {
            popup.setX(topLeft.getX());
            popup.setY(topLeft.getY());
        }

        // Keep aligned on resize
        scene.widthProperty().addListener((obs, o, n) -> Platform.runLater(() -> {
            resizeToScene();
            anchorToSceneRoot();
        }));
        scene.heightProperty().addListener((obs, o, n) -> Platform.runLater(() -> {
            resizeToScene();
            anchorToSceneRoot();
        }));

        // Keep aligned when window moves
        owner.xProperty().addListener((obs, o, n) -> Platform.runLater(this::anchorToSceneRoot));
        owner.yProperty().addListener((obs, o, n) -> Platform.runLater(this::anchorToSceneRoot));
    }

    private void anchorToSceneRoot() {
        if (scene == null || scene.getRoot() == null) return;

        scene.getRoot().applyCss();
        scene.getRoot().layout();

        Point2D topLeft = scene.getRoot().localToScreen(0, 0);
        if (topLeft == null) return;

        popup.setX(topLeft.getX());
        popup.setY(topLeft.getY());
    }

    void hide() {
        popup.hide();
    }

    void renderStep(List<OnboardingStep> steps, int index, Node target) {
        if (scene == null) return;

        boolean first = index <= 0;
        boolean last = index >= steps.size() - 1;

        backBtn.setDisable(first);
        nextBtn.setText(last ? "Done" : "Next");

        titleLabel.setText(steps.get(index).getTitle());
        textLabel.setText(steps.get(index).getText());

        layoutHighlightAndCard(target);

        if (last) {
            nextBtn.setOnAction(e -> onFinish.accept(null));
        } else {
            nextBtn.setOnAction(e -> onNext.run());
        }
    }

    private void resizeToScene() {
        if (scene == null) return;

        double w = scene.getWidth();
        double h = scene.getHeight();

        rootLayer.setMinSize(w, h);
        rootLayer.setPrefSize(w, h);
        rootLayer.setMaxSize(w, h);

        dim.setWidth(w);
        dim.setHeight(h);

        rootLayer.autosize();
        rootLayer.layout();
    }


    
    private void layoutHighlightAndCard(Node target) {
        if (target == null || target.getScene() == null) {
            highlight.setVisible(false);
            centerCard();
            return;
        }

        highlight.setVisible(true);

        Bounds bScene = target.localToScene(target.getBoundsInLocal());

        double pad = 10;
        double x = clamp(bScene.getMinX() - pad, 0, scene.getWidth());
        double y = clamp(bScene.getMinY() - pad, 0, scene.getHeight());
        double w = clamp(bScene.getWidth() + 2 * pad, 0, scene.getWidth());
        double h = clamp(bScene.getHeight() + 2 * pad, 0, scene.getHeight());

        highlight.setX(x);
        highlight.setY(y);
        highlight.setWidth(Math.min(w, scene.getWidth() - x));
        highlight.setHeight(Math.min(h, scene.getHeight() - y));    


        // place card below the highlight if possible, else above
        card.applyCss();
        card.layout();

        double cardW = Math.min(card.prefWidth(-1), 360);
        double cardH = card.prefHeight(cardW);

        double gap = 12;

        double belowY = highlight.getY() + highlight.getHeight() + gap;
        double aboveY = highlight.getY() - cardH - gap;

        double chosenY = (belowY + cardH <= scene.getHeight() - 12) ? belowY
                : (aboveY >= 12 ? aboveY : clamp(scene.getHeight() - cardH - 12, 12, scene.getHeight()));

        boolean narrowTarget = highlight.getWidth() < 120;

        double chosenX = narrowTarget
                ? (scene.getWidth() - cardW) / 2
                : clamp(
                      highlight.getX() + (highlight.getWidth() - cardW) / 2,
                      12,
                      scene.getWidth() - cardW - 12
                  );


        card.resizeRelocate(chosenX, chosenY, cardW, cardH);
    }


    private void centerCard() {
        card.applyCss();
        card.layout();

        double cardW = Math.min(card.prefWidth(-1), 360);
        double cardH = card.prefHeight(cardW);

        double x = (scene.getWidth() - cardW) / 2;
        double y = (scene.getHeight() - cardH) / 2;

        card.resizeRelocate(x, y, cardW, cardH);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
