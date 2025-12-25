package control;

import javafx.animation.Timeline;
import javafx.scene.ImageCursor;
import javafx.scene.layout.StackPane;
import model.Board;
import model.Difficulty;
import model.GameConfig;

public class GameStateController {

    GameConfig config;
    Difficulty difficulty;

    Board board1;
    Board board2;

    StackPane[][] p1Buttons;
    StackPane[][] p2Buttons;

    int sharedHearts;
    int score;

    int minesLeft1;
    int minesLeft2;

    int surprisesLeft1;
    int surprisesLeft2;

    int questionsLeft1;
    int questionsLeft2;

    int flagsLeft1;
    int flagsLeft2;

    int flagsPlaced1;
    int flagsPlaced2;

    int revealedCountP1;
    int revealedCountP2;

    int totalCellsP1;
    int totalCellsP2;

    int safeCellsRemaining1;
    int safeCellsRemaining2;

    int endScoreBefore;

    String player1OfficialName;
    String player2OfficialName;

    boolean isPlayer1Turn = true;
    boolean isPaused = false;
    boolean gameOver = false;
    boolean gameWon = false;

    boolean mistakeMade = false;

    ImageCursor forbiddenCursor;

    Timeline timer;
    int elapsedSeconds = 0;

    int endHeartsRemaining = 0;
    int endHeartsBonusPoints = 0;

    boolean[][] revealedCellsP1;
    boolean[][] revealedCellsP2;

    static final int TOTAL_HEART_SLOTS = 10;
}
