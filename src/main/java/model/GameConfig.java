package model;

public class GameConfig {
	
	    private final String player1Name;
	    private final String player2Name;
	    private final Difficulty difficulty;

	    private static GameConfig current;   //current simple singleton instead of player set up

	    public GameConfig(String player1Name, String player2Name, Difficulty difficulty) {
	        this.player1Name = player1Name;
	        this.player2Name = player2Name;
	        this.difficulty = difficulty;
	    }

	    public String getPlayer1Name() {
	        return player1Name;
	    }

	    public String getPlayer2Name() {
	        return player2Name;
	    }

	    public Difficulty getDifficulty() {
	        return difficulty;
	    }

	    //static access used by the controller

	    public static void setCurrent(GameConfig config) {
	        current = config;
	    }

	    public static GameConfig getCurrentOrDefault() {
	        if (current == null) {
	            //default values because i do not have setup screen yet
	            return new GameConfig("Player 1", "Player 2", Difficulty.MEDIUM);
	        }
	        return current;
	    }
}
