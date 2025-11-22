package model;

public class GameConfig {

    private final String player1Nickname;
    private final String player2Nickname;
    private final Difficulty difficulty;

    public GameConfig(String player1Nickname, String player2Nickname, Difficulty difficulty) {
        if (player1Nickname == null || player1Nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("player1Nickname cannot be null or empty");
        }
        if (player2Nickname == null || player2Nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("player2Nickname cannot be null or empty");
        }
        if (difficulty == null) {
            throw new IllegalArgumentException("difficulty cannot be null");
        }

        this.player1Nickname = player1Nickname.trim();
        this.player2Nickname = player2Nickname.trim();
        this.difficulty = difficulty;
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

    // Convenience delegates to Difficulty

    public int getRows() {
        return difficulty.getRows();
    }

    public int getCols() {
        return difficulty.getCols();
    }

    public int getInitialLives() {
        return difficulty.getInitialLives();
    }

    public int getMines() {
        return difficulty.getMines();
    }

    public int getQuestions() {
        return difficulty.getQuestions();
    }

    public int getSurprises() {
        return difficulty.getSurprises();
    }

    @Override
    public String toString() {
        return "GameConfig{" +
                "player1='" + player1Nickname + '\'' +
                ", player2='" + player2Nickname + '\'' +
                ", difficulty=" + difficulty +
                ", rows=" + getRows() +
                ", cols=" + getCols() +
                ", lives=" + getInitialLives() +
                ", mines=" + getMines() +
                ", questions=" + getQuestions() +
                ", surprises=" + getSurprises() +
                '}';
    }
}
