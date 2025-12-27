package control;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import model.Board;
import model.SysData;
import util.SoundManager;

public class GameUIServiceController {

    private final GameStateController s;

    private GamePlayServiceController playService;
    private GameBonusServiceController bonusService;

    private final GridPane player1Grid;
    private final GridPane player2Grid;

    // ✅ Replaced player1BombsLeftLabel/player2BombsLeftLabel with HBoxes
    private final HBox player1StatsBox;
    private final HBox player2StatsBox;

    private final Label difficultyLabel;
    private final Label timeLabel;
    private final Label scoreLabel;

    private final HBox heartsBox;

    private final Button pauseBtn;
    private final Button soundButton;
    private final Button musicButton;

    private final Parent root;

    private final ImageView player1AvatarImage;
    private final ImageView player2AvatarImage;

    public GameUIServiceController(GameStateController s,
                                   GridPane player1Grid,
                                   GridPane player2Grid,
                                   HBox player1StatsBox,
                                   HBox player2StatsBox,
                                   Label difficultyLabel,
                                   Label timeLabel,
                                   Label scoreLabel,
                                   HBox heartsBox,
                                   Button pauseBtn,
                                   Button soundButton,
                                   Button musicButton,
                                   Parent root,
                                   ImageView player1AvatarImage,
                                   ImageView player2AvatarImage) {
        this.s = s;
        this.player1Grid = player1Grid;
        this.player2Grid = player2Grid;
        this.player1StatsBox = player1StatsBox;
        this.player2StatsBox = player2StatsBox;
        this.difficultyLabel = difficultyLabel;
        this.timeLabel = timeLabel;
        this.scoreLabel = scoreLabel;
        this.heartsBox = heartsBox;
        this.pauseBtn = pauseBtn;
        this.soundButton = soundButton;
        this.musicButton = musicButton;
        this.root = root;
        this.player1AvatarImage = player1AvatarImage;
        this.player2AvatarImage = player2AvatarImage;
    }

    public void setPlayService(GamePlayServiceController playService) {
        this.playService = playService;
    }

    public void setBonusService(GameBonusServiceController bonusService) {
        this.bonusService = bonusService;
    }

    // =======================
    // Icons-based stats bar
    // =======================
    private ImageView icon(String path, double size) {
        var stream = getClass().getResourceAsStream(path);
        if (stream == null) {
            System.err.println("Missing resource: " + path);
            return new ImageView();
        }
        ImageView iv = new ImageView(new Image(stream));
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        return iv;
    }

    private HBox statItem(String iconPath, String value) {
        ImageView iv = icon(iconPath, 18);

        Label valueLbl = new Label(value);
        valueLbl.getStyleClass().add("stats-value"); // optional css

        HBox item = new HBox(6, iv, valueLbl);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPickOnBounds(false);
        return item;
    }

    private Label sep() {
        Label l = new Label("|");
        l.getStyleClass().add("stats-sep");
        return l;
    }

    private void buildPlayerStats(HBox box, String nickname,
                                  int mines, int flags, int surprises, int questions) {
        if (box == null) return;

        box.getChildren().clear();

        Label name = new Label(nickname + ",");
        name.getStyleClass().add("player-name"); 
        box.getChildren().add(name);
        box.getChildren().add(sep());

        box.getChildren().add(statItem("/Images/bomb.png", String.valueOf(mines)));
        box.getChildren().add(sep());

        box.getChildren().add(statItem("/Images/red-flag.png", String.valueOf(flags)));
        box.getChildren().add(sep());

        box.getChildren().add(statItem("/Images/giftbox.png", String.valueOf(surprises)));
        box.getChildren().add(sep());

        box.getChildren().add(statItem("/Images/question-mark.png", String.valueOf(questions)));
    }

    // =======================
    // UI Build
    // =======================
    public void loadAvatars() {
        setBoardAvatar(player1AvatarImage, s.config.getPlayer1AvatarPath());
        setBoardAvatar(player2AvatarImage, s.config.getPlayer2AvatarPath());
    }

    private void setBoardAvatar(ImageView target, String avatarId) {
        if (target == null) return;
        if (avatarId == null || avatarId.isBlank()) return;

        try {
            Image img;
            if (avatarId.startsWith("file:")) {
                img = new Image(avatarId);
            } else {
                var stream = getClass().getResourceAsStream("/Images/" + avatarId);
                if (stream == null) {
                    System.err.println("GameUIService: cannot find /Images/" + avatarId);
                    return;
                }
                img = new Image(stream);
            }
            target.setImage(img);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buildHeartsBar() {
        if (heartsBox == null) return;

        heartsBox.getChildren().clear();

        for (int i = 0; i < GameStateController.TOTAL_HEART_SLOTS; i++) {
            boolean isFull = i < s.sharedHearts;
            String imgPath = isFull ? "/Images/heart.png" : "/Images/favorite.png";

            var stream = getClass().getResourceAsStream(imgPath);
            if (stream == null) {
                System.err.println("Missing resource: " + imgPath);
                return;
            }

            Image img = new Image(stream);
            ImageView iv = new ImageView(img);
            iv.setFitHeight(50);
            iv.setFitWidth(50);
            iv.setPreserveRatio(true);

            StackPane slot = new StackPane(iv);
            slot.setPrefSize(50, 50);
            slot.setMinSize(50, 50);
            slot.setMaxSize(50, 50);
            StackPane.setMargin(iv, new Insets(0));

            heartsBox.getChildren().add(slot);
        }
    }

    public void initLabels() {
        difficultyLabel.setText(
                "Difficulty: " +
                        s.difficulty.name().charAt(0) +
                        s.difficulty.name().substring(1).toLowerCase()
        );

        if (SysData.isTimerEnabled()) {
            timeLabel.setText("Time: 00:00");
        } else {
            timeLabel.setText("Timer: OFF");
        }

        updateScoreAndMineLabels();
    }

    public void updateScoreAndMineLabels() {
        scoreLabel.setText("Score: " + s.score);

        buildPlayerStats(player1StatsBox,
                s.config.getPlayer1Nickname(),
                s.minesLeft1, s.flagsLeft1, s.surprisesLeft1, s.questionsLeft1);

        buildPlayerStats(player2StatsBox,
                s.config.getPlayer2Nickname(),
                s.minesLeft2, s.flagsLeft2, s.surprisesLeft2, s.questionsLeft2);
    }

    // =======================
    // Cursor + Boards State
    // =======================
    public void initForbiddenCursor() {
        try {
            var stream = getClass().getResourceAsStream("/Images/cursor_forbidden.png");
            if (stream == null) {
                s.forbiddenCursor = null;
                return;
            }
            Image img = new Image(stream);
            s.forbiddenCursor = new ImageCursor(img, img.getWidth() / 2, img.getHeight() / 2);
        } catch (Exception e) {
            s.forbiddenCursor = null;
        }
    }

    public void applyTurnStateToBoards() {
        if (player1Grid == null || player2Grid == null) return;

        if (s.isPlayer1Turn) {
            setBoardActive(player1Grid, player1StatsBox);
            setBoardInactive(player2Grid, player2StatsBox);
        } else {
            setBoardInactive(player1Grid, player1StatsBox);
            setBoardActive(player2Grid, player2StatsBox);
        }
    }

    private void setBoardActive(GridPane grid, HBox box) {
        grid.setDisable(false);

        grid.getStyleClass().remove("inactive-board");
        if (!grid.getStyleClass().contains("active-board")) {
            grid.getStyleClass().add("active-board");
        }

        grid.setCursor(Cursor.HAND);

        box.getStyleClass().remove("inactive-player-label");
        if (!box.getStyleClass().contains("active-player-label")) {
            box.getStyleClass().add("active-player-label");
        }
    }

    private void setBoardInactive(GridPane grid, HBox box) {
        grid.setDisable(true);

        grid.getStyleClass().remove("active-board");
        if (!grid.getStyleClass().contains("inactive-board")) {
            grid.getStyleClass().add("inactive-board");
        }

        grid.setCursor(s.forbiddenCursor != null ? s.forbiddenCursor : Cursor.DEFAULT);

        box.getStyleClass().remove("active-player-label");
        if (!box.getStyleClass().contains("inactive-player-label")) {
            box.getStyleClass().add("inactive-player-label");
        }
    }

    // =======================
    // Grids
    // =======================
    public void buildGrids() {
        buildGridForPlayer(player1Grid, s.board1, true);
        buildGridForPlayer(player2Grid, s.board2, false);
    }

    private void buildGridForPlayer(GridPane grid, model.Board board, boolean isPlayer1) {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        grid.getRowConstraints().clear();

        int rows = board.getRows();
        int cols = board.getCols();

        double colPercent = 100.0 / cols;
        double rowPercent = 100.0 / rows;

        for (int c = 0; c < cols; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(colPercent);
            cc.setHalignment(HPos.CENTER);
            grid.getColumnConstraints().add(cc);
        }

        for (int r = 0; r < rows; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(rowPercent);
            rc.setValignment(VPos.CENTER);
            grid.getRowConstraints().add(rc);
        }

        StackPane[][] buttons = new StackPane[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                StackPane tile = createCellTile(board, r, c, isPlayer1);
                buttons[r][c] = tile;
                grid.add(tile, c, r);
            }
        }

        if (isPlayer1) s.p1Buttons = buttons;
        else s.p2Buttons = buttons;
    }

    private StackPane createCellTile(model.Board board, int row, int col, boolean isPlayer1) {
        Button button = new Button();
        button.setMinSize(0, 0);
        button.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        button.getStyleClass().addAll("cell-tile", "cell-hidden");

        if (isPlayer1) button.getStyleClass().add("p1-cell");
        else button.getStyleClass().add("p2-cell");

        StackPane tile = new StackPane(button);
        tile.setMinSize(0, 0);
        tile.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        tile.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        GridPane.setHgrow(tile, Priority.ALWAYS);
        GridPane.setVgrow(tile, Priority.ALWAYS);

        final int r = row;
        final int c = col;
        final boolean tileIsPlayer1 = isPlayer1;

        button.setOnMouseClicked(e -> {
            if (s.gameOver) return;
            if (s.isPaused) return;

            if ((tileIsPlayer1 && !s.isPlayer1Turn) || (!tileIsPlayer1 && s.isPlayer1Turn)) {
                return;
            }

            if (button.isDisable()) return;

            if (bonusService != null) bonusService.resetIdleHintTimer();

            if (e.getButton() == MouseButton.SECONDARY) {
                if (!button.getStyleClass().contains("cell-hidden")) return;
                playService.toggleFlag(board, r, c, button, tileIsPlayer1);
                return;
            }

            if (e.getButton() == MouseButton.PRIMARY) {
                boolean activated = bonusService.tryHandleSecondClickActivation(board, r, c, button, tile, tileIsPlayer1);
                if (activated) {
                    playService.switchTurn();
                    return;
                }

                boolean consumedAction = playService.revealAndMaybeActivate(board, r, c, button, tile, tileIsPlayer1);
                if (consumedAction) playService.switchTurn();
                return;
            }
        });

        return tile;
    }

    // =======================
    // Timer
    // =======================
    public void startTimer() {
        if (s.timer != null) s.timer.stop();

        s.timer = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    if (!s.isPaused && !s.gameOver) {
                        s.elapsedSeconds++;
                        updateTimeLabel();
                    }
                })
        );
        s.timer.setCycleCount(Timeline.INDEFINITE);
        s.timer.play();
    }

    public void pauseTimer() {
        if (s.timer != null) s.timer.pause();
    }

    public void resumeTimer() {
        if (s.timer != null) s.timer.play();
    }

    public void stopTimer() {
        if (s.timer != null) s.timer.stop();
    }

    public void updateTimeLabel() {
        int minutes = s.elapsedSeconds / 60;
        int seconds = s.elapsedSeconds % 60;
        timeLabel.setText(String.format("Time: %02d:%02d", minutes, seconds));
    }

    // =======================
    // Sound/Music/Pause Icons
    // =======================
    public void refreshMusicIconFromSettings() {
        if (musicButton == null) return;
        if (!(musicButton.getGraphic() instanceof ImageView iv)) return;

        boolean enabled = SysData.isMusicEnabled();

        if (enabled && !SoundManager.isMusicOn()) {
            SoundManager.startMusic();
        } else if (!enabled && SoundManager.isMusicOn()) {
            SoundManager.stopMusic();
        }

        String iconPath = enabled ? "/Images/music.png" : "/Images/music_mute.png";
        double size = 40;

        Image img = new Image(getClass().getResourceAsStream(iconPath));
        iv.setImage(img);
        iv.setFitWidth(size);
        iv.setFitHeight(size);
    }

    public void refreshSoundIconFromSettings() {
        if (soundButton == null) return;
        if (!(soundButton.getGraphic() instanceof ImageView iv)) return;

        boolean enabled = SysData.isSoundEnabled();
        String iconPath = enabled ? "/Images/volume.png" : "/Images/mute.png";

        Image img = new Image(getClass().getResourceAsStream(iconPath));
        iv.setImage(img);
    }

    public void updatePauseIcon() {
        if (pauseBtn == null) return;
        if (!(pauseBtn.getGraphic() instanceof ImageView iv)) return;

        String iconPath = s.isPaused ? "/Images/play-button.png" : "/Images/pause.png";
        var stream = getClass().getResourceAsStream(iconPath);
        if (stream == null) {
            System.err.println("Missing resource: " + iconPath);
            return;
        }
        Image img = new Image(stream);
        iv.setImage(img);
    }

    // =======================
    // Misc
    // =======================
    public void setBoardsOpacity(double opacity) {
        if (player1Grid != null) player1Grid.setOpacity(opacity);
        if (player2Grid != null) player2Grid.setOpacity(opacity);
    }
}
