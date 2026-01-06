package util;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

final class OnboardingOverlay extends Pane {

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
        setPickOnBounds(true);

        // Dim layer: blocks interaction with underlying UI
        dim.setFill(Color.rgb(0, 0, 0, 0.35)); // adjust if you want darker
        dim.setMouseTransparent(false);

        // Bind dim to overlay size
        dim.widthProperty().bind(widthProperty());
        dim.heightProperty().bind(heightProperty());

        // Highlight rectangle
        highlight.setFill(Color.TRANSPARENT);
        highlight.setStroke(Color.rgb(255, 255, 255, 0.95));
        highlight.setStrokeWidth(3);
        highlight.setArcWidth(18);
        highlight.setArcHeight(18);
        highlight.setEffect(new DropShadow(25, Color.rgb(255, 255, 255, 0.55)));
        highlight.setMouseTransparent(true);

        // Card UI
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
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(10, backBtn, spacer, skipBtn, nextBtn);
        actions.setStyle("-fx-alignment: center-right;");

        backBtn.setStyle(buttonStyle());
       // nextBtn.setStyle(buttonStylePrimary());
     // keep default colorful theme look via CSS (theme.css), allow wolf override
        nextBtn.getStyleClass().add("onboarding-next");

        skipBtn.setStyle(buttonStyle());

        backBtn.setOnAction(e -> onBack.run());
        nextBtn.setOnAction(e -> onNext.run());
        skipBtn.setOnAction(e -> onSkip.run());

        card.getChildren().addAll(titleLabel, textLabel, actions);

        getChildren().addAll(dim, highlight, card);
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

    void renderStep(List<OnboardingStep> steps, int index, Node target) {
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

    private void layoutHighlightAndCard(Node target) {
        if (target == null || target.getScene() == null) {
            highlight.setVisible(false);
            centerCard();
            return;
        }

        highlight.setVisible(true);

        // Convert target bounds to this overlay's local coordinates
        Bounds bScene = target.localToScene(target.getBoundsInLocal());
        Point2D pMin = this.sceneToLocal(bScene.getMinX(), bScene.getMinY());

        double pad = 10;
        double x = clamp(pMin.getX() - pad, 0, getWidth());
        double y = clamp(pMin.getY() - pad, 0, getHeight());
        double w = clamp(bScene.getWidth() + 2 * pad, 0, getWidth());
        double h = clamp(bScene.getHeight() + 2 * pad, 0, getHeight());

        highlight.setX(x);
        highlight.setY(y);
        highlight.setWidth(Math.min(w, getWidth() - x));
        highlight.setHeight(Math.min(h, getHeight() - y));

        card.applyCss();
        card.layout();

        double cardW = Math.min(card.prefWidth(-1), 360);
        double cardH = card.prefHeight(cardW);

        double gap = 12;
        double belowY = highlight.getY() + highlight.getHeight() + gap;
        double aboveY = highlight.getY() - cardH - gap;

        double chosenY = (belowY + cardH <= getHeight() - 12) ? belowY
                : (aboveY >= 12 ? aboveY : clamp(getHeight() - cardH - 12, 12, getHeight()));

        boolean narrowTarget = highlight.getWidth() < 120;

        double chosenX = narrowTarget
                ? (getWidth() - cardW) / 2
                : clamp(
                      highlight.getX() + (highlight.getWidth() - cardW) / 2,
                      12,
                      getWidth() - cardW - 12
                  );

        card.resizeRelocate(chosenX, chosenY, cardW, cardH);
    }

    private void centerCard() {
        card.applyCss();
        card.layout();

        double cardW = Math.min(card.prefWidth(-1), 360);
        double cardH = card.prefHeight(cardW);

        double x = (getWidth() - cardW) / 2;
        double y = (getHeight() - cardH) / 2;

        card.resizeRelocate(x, y, cardW, cardH);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    // Attach/remove helpers (called from OnboardingManager)
    static OnboardingOverlay attachToScene(Scene scene, OnboardingOverlay overlay) {
        if (scene == null || scene.getRoot() == null) return overlay;

        Parent oldRoot = scene.getRoot();
        StackPane stack;

        if (oldRoot instanceof StackPane sp) {
            stack = sp;
        } else {
            // Wrap old root
            stack = new StackPane(oldRoot);

            // CRITICAL: preserve identity that CSS selectors depend on
            stack.setId(oldRoot.getId());
            stack.getStyleClass().addAll(oldRoot.getStyleClass());

            // Optional but recommended: preserve inline style too
            stack.setStyle(oldRoot.getStyle());

            // Avoid duplicated style classes (not mandatory, but clean)
            oldRoot.setId(null);
            oldRoot.getStyleClass().clear();
            oldRoot.setStyle("");

            scene.setRoot(stack);
        }

        // Ensure overlay stretches
        StackPane.setAlignment(overlay, javafx.geometry.Pos.TOP_LEFT);
        overlay.prefWidthProperty().bind(stack.widthProperty());
        overlay.prefHeightProperty().bind(stack.heightProperty());

        if (!stack.getChildren().contains(overlay)) {
            stack.getChildren().add(overlay);
        } else {
            overlay.toFront();
        }

        return overlay;
    }


    static void detachFromScene(Scene scene, OnboardingOverlay overlay) {
        if (scene == null || overlay == null) return;
        if (scene.getRoot() instanceof StackPane sp) {
            sp.getChildren().remove(overlay);
        }
    }
}
