package model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
    private final boolean winWithoutMistakes;
    private final String player1AvatarPath;
    private final String player2AvatarPath;



    //Can be used elsewhere if needed (for time fields, not used directly here)
    public static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    public Game(String player1OfficialName,
            String player2OfficialName,
            String player1Nickname,
            String player2Nickname,
            Difficulty difficulty,
            int score,
            GameResult result,
            LocalDate date,
            int durationSeconds,
            boolean winWithoutMistakes,
            String player1AvatarPath,
            String player2AvatarPath) {

    this.player1OfficialName = player1OfficialName;
    this.player2OfficialName = player2OfficialName;
    this.player1Nickname = player1Nickname;
    this.player2Nickname = player2Nickname;
    this.difficulty = difficulty;
    this.finalScore = score;
    this.result = result;
    this.date = date;
    this.durationSeconds = durationSeconds;
    this.winWithoutMistakes = winWithoutMistakes;
    this.player1AvatarPath = player1AvatarPath;
    this.player2AvatarPath = player2AvatarPath;
}
    
    public boolean isWinWithoutMistakes() {
        return winWithoutMistakes;
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

	public String getPlayer1AvatarPath() {
		return player1AvatarPath;
	}

	public String getPlayer2AvatarPath() {
		return player2AvatarPath;
	}

	public static DateTimeFormatter getTimeFormatter() {
		return TIME_FORMATTER;
	}

	@Override
	public String toString() {
		return "Game [player1OfficialName=" + player1OfficialName + ", player2OfficialName=" + player2OfficialName
				+ ", player1Nickname=" + player1Nickname + ", player2Nickname=" + player2Nickname + ", difficulty="
				+ difficulty + ", finalScore=" + finalScore + ", result=" + result + ", date=" + date
				+ ", durationSeconds=" + durationSeconds + ", winWithoutMistakes=" + winWithoutMistakes
				+ ", player1AvatarPath=" + player1AvatarPath + ", player2AvatarPath=" + player2AvatarPath + "]";
	}

   
}
