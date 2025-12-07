package model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

//Represents a single finished game that will appear in the History screen.

public class Game {
	private final String player1OfficialName;
	private final String player2OfficialName;
    private final String player1Nickname;
    private final String player2Nickname;
    private final Difficulty difficulty;
    private final int finalScore;
    private final GameResult result;
    private final LocalDate date;       
    private final int durationSeconds;  //total duration in seconds

    //Can be used elsewhere if needed (for time fields, not used directly here)
    public static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    public Game(String player1OfficialName, String player2OfficialName, String player1Nickname,String player2Nickname, Difficulty difficulty, int finalScore, GameResult result, LocalDate date, int durationSeconds) {

        if (player1Nickname == null || player1Nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("player1Nickname cannot be null or empty");
        }
        if (player2Nickname == null || player2Nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("player2Nickname cannot be null or empty");
        }
        if (durationSeconds < 0) {
            throw new IllegalArgumentException("durationSeconds cannot be negative");
        }

        this.player1OfficialName = (player1OfficialName == null || player1OfficialName.isBlank()) ? null : player1OfficialName.trim();
        this.player2OfficialName = (player2OfficialName == null || player2OfficialName.isBlank()) ? null : player2OfficialName.trim();
        this.player1Nickname = player1Nickname.trim();
        this.player2Nickname = player2Nickname.trim();
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty cannot be null");
        this.finalScore = finalScore;
        this.result = Objects.requireNonNull(result, "result cannot be null");
        this.date = Objects.requireNonNull(date, "date cannot be null");
        this.durationSeconds = durationSeconds;
    }

    public String getPlayer1OfficialName() {
        return player1OfficialName;
    }

    public String getPlayer2OfficialName() {
        return player2OfficialName;
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

    
    //Formats the duration as M:SS (for example "10:21").
   
    public String getDurationFormatted() {
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

   //Helper for showing the date as text (for example "2025-11-26").
 
    public String getDateAsString() {
        return date.toString(); // ISO_LOCAL_DATE by default
    }


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
