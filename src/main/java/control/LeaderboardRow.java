package control;

import javafx.beans.property.*;

public class LeaderboardRow {

    public enum Type { PLAYER, TEAM }

    private final ObjectProperty<Type> type = new SimpleObjectProperty<>(Type.PLAYER);

    private final IntegerProperty rank = new SimpleIntegerProperty(0);

    // For PLAYER rows: name = official name
    // For TEAM rows: optional display name (kept for backward compatibility)
    private final StringProperty name = new SimpleStringProperty("");

    // TEAM-specific: show two separate names in the table
    private final StringProperty player1Name = new SimpleStringProperty("");
    private final StringProperty player2Name = new SimpleStringProperty("");

    private final IntegerProperty games = new SimpleIntegerProperty(0);
    private final IntegerProperty wins  = new SimpleIntegerProperty(0);

    private final DoubleProperty winRate  = new SimpleDoubleProperty(0.0); // 0..100
    private final DoubleProperty avgScore = new SimpleDoubleProperty(0.0);

    private final DoubleProperty avgWinTimeSeconds = new SimpleDoubleProperty(0.0);

    // Avatars (IDs like "S5.png")
    private final StringProperty avatar1Id = new SimpleStringProperty(null);
    private final StringProperty avatar2Id = new SimpleStringProperty(null);

    public LeaderboardRow() {}

    public LeaderboardRow(Type type, int rank, String name) {
        this.type.set(type);
        this.rank.set(rank);
        this.name.set(name);
    }

    // --- Type
    public Type getType() { return type.get(); }
    public ObjectProperty<Type> typeProperty() { return type; }
    public void setType(Type type) { this.type.set(type); }

    // --- Rank
    public int getRank() { return rank.get(); }
    public IntegerProperty rankProperty() { return rank; }
    public void setRank(int rank) { this.rank.set(rank); }

    // --- Name (player name for PLAYER rows; optional for TEAM rows)
    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }
    public void setName(String name) { this.name.set(name); }

    // --- Team names
    public String getPlayer1Name() { return player1Name.get(); }
    public StringProperty player1NameProperty() { return player1Name; }
    public void setPlayer1Name(String player1Name) { this.player1Name.set(player1Name); }

    public String getPlayer2Name() { return player2Name.get(); }
    public StringProperty player2NameProperty() { return player2Name; }
    public void setPlayer2Name(String player2Name) { this.player2Name.set(player2Name); }

    // --- Games
    public int getGames() { return games.get(); }
    public IntegerProperty gamesProperty() { return games; }
    public void setGames(int games) { this.games.set(games); }

    // --- Wins
    public int getWins() { return wins.get(); }
    public IntegerProperty winsProperty() { return wins; }
    public void setWins(int wins) { this.wins.set(wins); }

    // --- WinRate (0..100)
    public double getWinRate() { return winRate.get(); }
    public DoubleProperty winRateProperty() { return winRate; }
    public void setWinRate(double winRate) { this.winRate.set(winRate); }

    // --- AvgScore
    public double getAvgScore() { return avgScore.get(); }
    public DoubleProperty avgScoreProperty() { return avgScore; }
    public void setAvgScore(double avgScore) { this.avgScore.set(avgScore); }

    // --- Avg win time (seconds)
    public double getAvgWinTimeSeconds() { return avgWinTimeSeconds.get(); }
    public DoubleProperty avgWinTimeSecondsProperty() { return avgWinTimeSeconds; }
    public void setAvgWinTimeSeconds(double avgWinTimeSeconds) { this.avgWinTimeSeconds.set(avgWinTimeSeconds); }

    // --- Avatars
    public String getAvatar1Id() { return avatar1Id.get(); }
    public StringProperty avatar1IdProperty() { return avatar1Id; }
    public void setAvatar1Id(String id) { this.avatar1Id.set(id); }

    public String getAvatar2Id() { return avatar2Id.get(); }
    public StringProperty avatar2IdProperty() { return avatar2Id; }
    public void setAvatar2Id(String id) { this.avatar2Id.set(id); }

    // --- Convenience
    public String getAvgWinTimeText() {
        if (getWins() <= 0) return "—";
        return formatSeconds((int) Math.round(getAvgWinTimeSeconds()));
    }

    private static String formatSeconds(int totalSeconds) {
        if (totalSeconds <= 0) return "—";
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}
