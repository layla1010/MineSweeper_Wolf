package model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents a single finished game that will appear in the History screen.
 * Stores:
 *  - player1 nickname
 *  - player2 nickname
 *  - difficulty level
 *  - final score
 *  - result (WIN / LOSE)
 *  - date of the game
 *  - duration in seconds (stopwatch from start to end)
 */


public class Game {
	
	private final GameConfig config;
    private final Board board1;
    private final Board board2;

    private int sharedHearts;
    private int score;
    private int minesLeft1;
    private int minesLeft2;
    private boolean isPlayer1Turn;
    private boolean paused;
    private long elapsedSeconds;
    
    public Game(GameConfig config) {
        this.config = config;
        this.board1 = new Board(config.getDifficulty());
        this.board2 = new Board(config.getDifficulty());

        this.sharedHearts   = config.getDifficulty().getInitialLives();
        this.score          = 0;
        this.minesLeft1     = board1.getMineCount();
        this.minesLeft2     = board2.getMineCount();
        this.isPlayer1Turn  = true;
        this.paused         = false;
        this.elapsedSeconds = 0;
    }
    
    
    public GameConfig getConfig() {
        return config;
    }

    public Difficulty getDifficulty() {
        return config.getDifficulty();
    }

    public Board getBoard1() {
        return board1;
    }

    public Board getBoard2() {
        return board2;
    }

    public int getSharedHearts() {
        return sharedHearts;
    }

    public int getScore() {
        return score;
    }

    public int getMinesLeft1() {
        return minesLeft1;
    }

    public int getMinesLeft2() {
        return minesLeft2;
    }

    public boolean isPlayer1Turn() {
        return isPlayer1Turn;
    }

    public boolean isPaused() {
        return paused;
    }

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    
    public void loseHeart() {
        if (sharedHearts > 0) {
            sharedHearts--;
        }
    }

    public void addScore(int delta) {
        score += delta;
        if (score < 0) score = 0;
    }

    public void decrementMinesLeftForPlayer1() {
        if (minesLeft1 > 0) {
            minesLeft1--;
        }
    }

    public void decrementMinesLeftForPlayer2() {
        if (minesLeft2 > 0) {
            minesLeft2--;
        }
    }

    public void switchTurn() {
        isPlayer1Turn = !isPlayer1Turn;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void incrementElapsedSeconds() {
        elapsedSeconds++;
    }

    private final String player1Nickname;
    private final String player2Nickname;
    private final Difficulty difficulty;
    private final int finalScore;
    private final GameResult result;
    private final LocalDate date;   // e.g. 2025-11-26
    final int durationSeconds;;   // total duration in seconds

    // We will reuse these both for CSV and for the History screen
    public static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    public Game(String player1Nickname,
                String player2Nickname,
                Difficulty difficulty,
                int finalScore,
                GameResult result,
                LocalDate date,
                int durationSeconds) {

        if (player1Nickname == null || player1Nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("player1Nickname cannot be null or empty");
        }
        if (player2Nickname == null || player2Nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("player2Nickname cannot be null or empty");
        }
        if (durationSeconds < 0) {
            throw new IllegalArgumentException("durationSeconds cannot be negative");
        }
        this.player1Nickname = player1Nickname.trim();
        this.player2Nickname = player2Nickname.trim();
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty cannot be null");
        this.finalScore = finalScore;
        this.result = Objects.requireNonNull(result, "result cannot be null");
        this.date = Objects.requireNonNull(date, "date cannot be null");
        this.durationSeconds = durationSeconds;
    }

    public String getPlayer1Nickname() {
        return player1Nickname;
    }

    public String getPlayer2Nickname() {
        return player2Nickname;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public int getFinalScore() {
    	
        return finalScore;
    }
    
    public GameResult getResult() {
        return result;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }
    
    /**
     * Formats the duration as M:SS (e.g. "10:21").
     */
    public String getDurationFormatted() {
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Helper for showing the date as text (e.g. "2025-11-26").
     */
    public String getDateAsString() {
        return date.toString(); // ISO_LOCAL_DATE by default
    }
    
    /**
     * Nice text for UI ("Win" / "Lose")
     */
    public String getResultAsText() {
        return (result == GameResult.WIN) ? "Win" : "Lose";
    }


    @Override
    public String toString() {
        return "Game{" +
                "player1='" + player1Nickname + '\'' +
                ", player2='" + player2Nickname + '\'' +
                ", difficulty=" + difficulty +
                ", finalScore=" + finalScore +
                ", result=" + result +
                ", date=" + date +
                ", duration=" + getDurationFormatted() +
                '}';
    }
}
