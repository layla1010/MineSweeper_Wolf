package model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents a single finished game that will appear in the History screen.
 * A Game stores:
 *  - player1 nickname
 *  - player2 nickname
 *  - difficulty level
 *  - final score
 *  - date of the game
 *  - time of the game (time of day)
 */
public class Game {

    private final String player1Nickname;
    private final String player2Nickname;
    private final Difficulty difficulty;
    private final int finalScore;
    private final LocalDate date;   // e.g. 2025-11-26
    private final LocalTime time;   // e.g. 21:15

    // We will reuse these both for CSV and for the History screen
    public static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    public Game(String player1Nickname,
                String player2Nickname,
                Difficulty difficulty,
                int finalScore,
                LocalDate date,
                LocalTime time) {

        if (player1Nickname == null || player1Nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("player1Nickname cannot be null or empty");
        }
        if (player2Nickname == null || player2Nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("player2Nickname cannot be null or empty");
        }
        this.player1Nickname = player1Nickname.trim();
        this.player2Nickname = player2Nickname.trim();
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty cannot be null");
        this.finalScore = finalScore;
        this.date = Objects.requireNonNull(date, "date cannot be null");
        this.time = Objects.requireNonNull(time, "time cannot be null");
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

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTime() {
        return time;
    }

    /**
     * Helper for showing the date as text (e.g. "2025-11-26").
     */
    public String getDateAsString() {
        return date.toString(); // ISO_LOCAL_DATE by default
    }

    /**
     * Helper for showing the time as text (e.g. "21:15").
     */
    public String getTimeAsString() {
        return time.format(TIME_FORMATTER);
    }

    @Override
    public String toString() {
        return "Game{" +
                "player1='" + player1Nickname + '\'' +
                ", player2='" + player2Nickname + '\'' +
                ", difficulty=" + difficulty +
                ", finalScore=" + finalScore +
                ", date=" + date +
                ", time=" + time +
                '}';
    }
}
