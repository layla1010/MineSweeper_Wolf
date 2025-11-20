package model;

public class GameConfig {

    private final String player1Nickname;
    private final String player2Nickname;
    private final Difficulty difficulty;

    public GameConfig(String player1Nickname, String player2Nickname, Difficulty difficulty) {
        this.player1Nickname = player1Nickname;
        this.player2Nickname = player2Nickname;
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

    // convenience getters for acceptance test / board logic
    public int getRows() { return difficulty.getRows(); }
    public int getCols() { return difficulty.getCols(); }
    public int getMines() { return difficulty.getMines(); }
    public int getInitialLives() { return difficulty.getInitialLives(); }
}
