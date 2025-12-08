package model;

public class PlayerStats {

    public final String playerName;        
    public final String avatarId;         

    public final int totalGames;
    public final int wins;
    public final int losses;
    public final int giveUps;
    public final int winsWithNoMistakes;

    public final int bestScore;
    public final String bestScoreOpponent;

    public final int bestTimeSeconds;
    public final String bestTimeOpponent;

    public final int[] easyScores;
    public final int[] mediumScores;
    public final int[] hardScores;

    public PlayerStats(String playerName,
                       String avatarId,
                       int totalGames,
                       int wins,
                       int losses,
                       int giveUps,
                       int winsWithNoMistakes,
                       int bestScore,
                       String bestScoreOpponent,
                       int bestTimeSeconds,
                       String bestTimeOpponent,
                       int[] easyScores,
                       int[] mediumScores,
                       int[] hardScores) {

        this.playerName = (playerName == null || playerName.isBlank()) ? "-" : playerName;
        this.avatarId   = avatarId;

        this.totalGames = Math.max(0, totalGames);
        this.wins       = Math.max(0, wins);
        this.losses     = Math.max(0, losses);
        this.giveUps    = Math.max(0, giveUps);
        this.winsWithNoMistakes = Math.max(0, winsWithNoMistakes);

        this.bestScore = Math.max(0, bestScore);
        this.bestScoreOpponent = (bestScoreOpponent == null || bestScoreOpponent.isBlank())
                ? "-" : bestScoreOpponent;

        this.bestTimeSeconds = Math.max(0, bestTimeSeconds);
        this.bestTimeOpponent = (bestTimeOpponent == null || bestTimeOpponent.isBlank())
                ? "-" : bestTimeOpponent;

        this.easyScores   = easyScores   != null ? easyScores   : new int[0];
        this.mediumScores = mediumScores != null ? mediumScores : new int[0];
        this.hardScores   = hardScores   != null ? hardScores   : new int[0];
    }
}
