package model;

public class GameConfig {

    public GameConfig(String player1Nickname, String player2Nickname, Difficulty difficulty, String player1AvatarPath,
			String player2AvatarPath) {
		super();
		this.player1Nickname = player1Nickname;
		this.player2Nickname = player2Nickname;
		this.difficulty = difficulty;
		this.player1AvatarPath = player1AvatarPath;
		this.player2AvatarPath = player2AvatarPath;
	}
	private final String player1Nickname;
    private final String player2Nickname;
    private final Difficulty difficulty;
    
 // avatar paths (classpath or file: URL)
    private final String player1AvatarPath;
    private final String player2AvatarPath;
	public String getPlayer1Nickname() {
		return player1Nickname;
	}
	public String getPlayer2Nickname() {
		return player2Nickname;
	}
	public Difficulty getDifficulty() {
		return difficulty;
	}
	public String getPlayer1AvatarPath() {
		return player1AvatarPath;
	}
	public String getPlayer2AvatarPath() {
		return player2AvatarPath;
	}
	@Override
	public String toString() {
		return "GameConfig [player1Nickname=" + player1Nickname + ", player2Nickname=" + player2Nickname
				+ ", difficulty=" + difficulty + ", player1AvatarPath=" + player1AvatarPath + ", player2AvatarPath="
				+ player2AvatarPath + "]";
	}

    
}
