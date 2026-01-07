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

	public GameConfig getConfig() {
		return config;
	}

	public void setConfig(GameConfig config) {
		this.config = config;
	}

	public Difficulty getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(Difficulty difficulty) {
		this.difficulty = difficulty;
	}

	public Board getBoard1() {
		return board1;
	}

	public void setBoard1(Board board1) {
		this.board1 = board1;
	}

	public Board getBoard2() {
		return board2;
	}

	public void setBoard2(Board board2) {
		this.board2 = board2;
	}

	public StackPane[][] getP1Buttons() {
		return p1Buttons;
	}

	public void setP1Buttons(StackPane[][] p1Buttons) {
		this.p1Buttons = p1Buttons;
	}

	public StackPane[][] getP2Buttons() {
		return p2Buttons;
	}

	public void setP2Buttons(StackPane[][] p2Buttons) {
		this.p2Buttons = p2Buttons;
	}

	public int getSharedHearts() {
		return sharedHearts;
	}

	public void setSharedHearts(int sharedHearts) {
		this.sharedHearts = sharedHearts;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public int getMinesLeft1() {
		return minesLeft1;
	}

	public void setMinesLeft1(int minesLeft1) {
		this.minesLeft1 = minesLeft1;
	}

	public int getMinesLeft2() {
		return minesLeft2;
	}

	public void setMinesLeft2(int minesLeft2) {
		this.minesLeft2 = minesLeft2;
	}

	public int getSurprisesLeft1() {
		return surprisesLeft1;
	}

	public void setSurprisesLeft1(int surprisesLeft1) {
		this.surprisesLeft1 = surprisesLeft1;
	}

	public int getSurprisesLeft2() {
		return surprisesLeft2;
	}

	public void setSurprisesLeft2(int surprisesLeft2) {
		this.surprisesLeft2 = surprisesLeft2;
	}

	public int getQuestionsLeft1() {
		return questionsLeft1;
	}

	public void setQuestionsLeft1(int questionsLeft1) {
		this.questionsLeft1 = questionsLeft1;
	}

	public int getQuestionsLeft2() {
		return questionsLeft2;
	}

	public void setQuestionsLeft2(int questionsLeft2) {
		this.questionsLeft2 = questionsLeft2;
	}

	public int getFlagsLeft1() {
		return flagsLeft1;
	}

	public void setFlagsLeft1(int flagsLeft1) {
		this.flagsLeft1 = flagsLeft1;
	}

	public int getFlagsLeft2() {
		return flagsLeft2;
	}

	public void setFlagsLeft2(int flagsLeft2) {
		this.flagsLeft2 = flagsLeft2;
	}

	public int getFlagsPlaced1() {
		return flagsPlaced1;
	}

	public void setFlagsPlaced1(int flagsPlaced1) {
		this.flagsPlaced1 = flagsPlaced1;
	}

	public int getFlagsPlaced2() {
		return flagsPlaced2;
	}

	public void setFlagsPlaced2(int flagsPlaced2) {
		this.flagsPlaced2 = flagsPlaced2;
	}

	public int getRevealedCountP1() {
		return revealedCountP1;
	}

	public void setRevealedCountP1(int revealedCountP1) {
		this.revealedCountP1 = revealedCountP1;
	}

	public int getRevealedCountP2() {
		return revealedCountP2;
	}

	public void setRevealedCountP2(int revealedCountP2) {
		this.revealedCountP2 = revealedCountP2;
	}

	public int getTotalCellsP1() {
		return totalCellsP1;
	}

	public void setTotalCellsP1(int totalCellsP1) {
		this.totalCellsP1 = totalCellsP1;
	}

	public int getTotalCellsP2() {
		return totalCellsP2;
	}

	public void setTotalCellsP2(int totalCellsP2) {
		this.totalCellsP2 = totalCellsP2;
	}

	public int getSafeCellsRemaining1() {
		return safeCellsRemaining1;
	}

	public void setSafeCellsRemaining1(int safeCellsRemaining1) {
		this.safeCellsRemaining1 = safeCellsRemaining1;
	}

	public int getSafeCellsRemaining2() {
		return safeCellsRemaining2;
	}

	public void setSafeCellsRemaining2(int safeCellsRemaining2) {
		this.safeCellsRemaining2 = safeCellsRemaining2;
	}

	public int getEndScoreBefore() {
		return endScoreBefore;
	}

	public void setEndScoreBefore(int endScoreBefore) {
		this.endScoreBefore = endScoreBefore;
	}

	public String getPlayer1OfficialName() {
		return player1OfficialName;
	}

	public void setPlayer1OfficialName(String player1OfficialName) {
		this.player1OfficialName = player1OfficialName;
	}

	public String getPlayer2OfficialName() {
		return player2OfficialName;
	}

	public void setPlayer2OfficialName(String player2OfficialName) {
		this.player2OfficialName = player2OfficialName;
	}

	public boolean isPlayer1Turn() {
		return isPlayer1Turn;
	}

	public void setPlayer1Turn(boolean isPlayer1Turn) {
		this.isPlayer1Turn = isPlayer1Turn;
	}

	public boolean isPaused() {
		return isPaused;
	}

	public void setPaused(boolean isPaused) {
		this.isPaused = isPaused;
	}

	public boolean isGameOver() {
		return gameOver;
	}

	public void setGameOver(boolean gameOver) {
		this.gameOver = gameOver;
	}

	public boolean isGameWon() {
		return gameWon;
	}

	public void setGameWon(boolean gameWon) {
		this.gameWon = gameWon;
	}

	public boolean isMistakeMade() {
		return mistakeMade;
	}

	public void setMistakeMade(boolean mistakeMade) {
		this.mistakeMade = mistakeMade;
	}

	public ImageCursor getForbiddenCursor() {
		return forbiddenCursor;
	}

	public void setForbiddenCursor(ImageCursor forbiddenCursor) {
		this.forbiddenCursor = forbiddenCursor;
	}

	public Timeline getTimer() {
		return timer;
	}

	public void setTimer(Timeline timer) {
		this.timer = timer;
	}

	public int getElapsedSeconds() {
		return elapsedSeconds;
	}

	public void setElapsedSeconds(int elapsedSeconds) {
		this.elapsedSeconds = elapsedSeconds;
	}

	public int getEndHeartsRemaining() {
		return endHeartsRemaining;
	}

	public void setEndHeartsRemaining(int endHeartsRemaining) {
		this.endHeartsRemaining = endHeartsRemaining;
	}

	public int getEndHeartsBonusPoints() {
		return endHeartsBonusPoints;
	}

	public void setEndHeartsBonusPoints(int endHeartsBonusPoints) {
		this.endHeartsBonusPoints = endHeartsBonusPoints;
	}

	public boolean[][] getRevealedCellsP1() {
		return revealedCellsP1;
	}

	public void setRevealedCellsP1(boolean[][] revealedCellsP1) {
		this.revealedCellsP1 = revealedCellsP1;
	}

	public boolean[][] getRevealedCellsP2() {
		return revealedCellsP2;
	}

	public void setRevealedCellsP2(boolean[][] revealedCellsP2) {
		this.revealedCellsP2 = revealedCellsP2;
	}

	public static int getTotalHeartSlots() {
		return TOTAL_HEART_SLOTS;
	}
}
