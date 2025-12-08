package model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SysData {

    // ===== Singleton =====

    /** Singleton instance of SysData. */
    private static final SysData INSTANCE = new SysData();

    public static SysData getInstance() {
        return INSTANCE;
    }

    // ===== History & Players =====

    /** Stores all game history records. */
    private final History history = new History();

    /** Player lookup by email (lowercased). */
    private final Map<String, Player> playersByEmail = new HashMap<>();

    /** Player lookup by official name (lowercased). */
    private final Map<String, Player> playersByName = new HashMap<>();

    /** CSV file name (kept for reference – actual path is resolved dynamically). */
    private static final String HISTORY_FILE_NAME = "/data/history.csv";

    // ===== Global Settings (Filters) =====

    /** Controls whether background music is enabled. */
    private static boolean musicEnabled = true;

    /** Controls whether sound effects are enabled. */
    private static boolean soundEnabled = true;

    /** Controls whether a game timer should be shown and used. */
    private static boolean timerEnabled = true;

    /** Controls whether "smart hints" are enabled in the game. */
    private static boolean smartHintsEnabled = false;

    /** Controls whether flags are automatically removed in some situations. */
    private static boolean autoRemoveFlagEnabled = true;

    /** Private constructor (singleton). */
    private SysData() {
    }

    // ================== Accessors ==================

    /** Returns the History object that holds all Game records. */
    public History getHistory() {
        return history;
    }

    // ================== File Path Helpers ==================

    /** Resolves the full path to the history CSV file. */
    private static String getHistoryCsvPath() {
        try {
            String path = SysData.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath();

            String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8.name());

            // Windows fix: remove leading '/' from "C:/..."
            if (decoded.length() > 2
                    && decoded.charAt(0) == '/'
                    && Character.isLetter(decoded.charAt(1))
                    && decoded.charAt(2) == ':') {
                decoded = decoded.substring(1);
            }

            // Running from a JAR
            if (decoded.contains(".jar")) {
                decoded = decoded.substring(0, decoded.lastIndexOf('/'));
                return decoded + "/Data/history.csv";
            } else {
                // Running from IDE
                if (decoded.contains("target/classes/")) {
                    decoded = decoded.substring(0, decoded.lastIndexOf("target/classes/"));
                } else if (decoded.contains("bin/")) {
                    decoded = decoded.substring(0, decoded.lastIndexOf("bin/"));
                } else {
                    return "Data/history.csv";
                }

                return decoded + "src/main/resources/Data/history.csv";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Data/history.csv";
        }
    }

    /** Resolves the full path to the players CSV file. */
    private static String getPlayersCsvPath() {
        try {
            String path = SysData.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath();

            String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8.name());

            // Windows fix
            if (decoded.length() > 2
                    && decoded.charAt(0) == '/'
                    && Character.isLetter(decoded.charAt(1))
                    && decoded.charAt(2) == ':') {
                decoded = decoded.substring(1);
            }

            if (decoded.contains(".jar")) {
                decoded = decoded.substring(0, decoded.lastIndexOf('/'));
                return decoded + "/Data/Users.csv";
            } else {
                if (decoded.contains("target/classes/")) {
                    decoded = decoded.substring(0, decoded.lastIndexOf("target/classes/"));
                } else if (decoded.contains("bin/")) {
                    decoded = decoded.substring(0, decoded.lastIndexOf("bin/"));
                } else {
                    return "Data/Users.csv";
                }

                return decoded + "src/main/resources/Data/Users.csv";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Data/Users.csv";
        }
    }

    // ================== History: Load / Save ==================

    /** Adds a single Game record to the history. */
    public void addGameToHistory(Game game) {
        history.addGame(game);
    }

    /** Loads all game history from the history CSV file into memory. */
    public void loadHistoryFromCsv() {
        history.clear();

        String csvPath = getHistoryCsvPath();
        System.out.println("Loading history from: " + csvPath);

        Path path = Paths.get(csvPath);
        if (!Files.exists(path)) {
            System.out.println("History file not found, skipping load.");
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line = reader.readLine();

            // Skip header if present
            if (line != null && line.startsWith("date,")) {
                line = reader.readLine();
            }

            while (line != null) {
                Game game = parseGameFromCsvLine(line);
                if (game != null) {
                    history.addGame(game);
                }
                line = reader.readLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Parses a single CSV line into a Game object. */
    private Game parseGameFromCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        String[] parts = line.split(",", -1);  // keep empty columns
        if (parts.length != 9) {
            return null;
        }

        try {
            LocalDate date = LocalDate.parse(parts[0]);       // "2025-11-26"
            int durationSeconds = parseDuration(parts[1]);    // "21:15" or "1275"
            Difficulty difficulty = Difficulty.valueOf(parts[2]);
            int score = Integer.parseInt(parts[3]);
            GameResult result = GameResult.valueOf(parts[4]); // WIN / LOSE / GIVE_UP
            String nick_player1 = parts[5];
            String nick_player2 = parts[6];
            String off_player1 = toNullIfBlank(parts[7]);
            String off_player2 = toNullIfBlank(parts[8]);

            return new Game(
                    off_player1,
                    off_player2,
                    nick_player1,
                    nick_player2,
                    difficulty,
                    score,
                    result,
                    date,
                    durationSeconds
            );

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Converts empty strings to null. */
    private String toNullIfBlank(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    /** Parses "mm:ss" or raw seconds into an int duration in seconds. */
    private int parseDuration(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        text = text.trim();

        if (text.contains(":")) {
            String[] parts = text.split(":");
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            return minutes * 60 + seconds;
        } else {
            // assume raw seconds
            return Integer.parseInt(text);
        }
    }

    /** Saves the current in-memory history list to the CSV file. */
    public void saveHistoryToCsv() {
        String csvPath = getHistoryCsvPath();
        System.out.println("Saving history to: " + csvPath);

        Path path = Paths.get(csvPath);
        var games = history.getGames();

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            // header
            writer.write("date,duration,difficulty,score,result,player1Nickname,player2Nickname,player1Official,player2Official");
            writer.newLine();

            for (Game game : games) {
                writer.write(formatGameAsCsvLine(game));
                writer.newLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Converts a Game object into a CSV line string. */
    private String formatGameAsCsvLine(Game game) {
        String dateStr       = game.getDate().toString();
        String durationStr   = game.getDurationFormatted();
        String difficultyStr = game.getDifficulty().name();
        String scoreStr      = Integer.toString(game.getFinalScore());
        String resultStr     = game.getResult().name();
        String p1Nick        = sanitizeForCsv(game.getPlayer1Nickname());
        String p2Nick        = sanitizeForCsv(game.getPlayer2Nickname());
        String p1Off         = sanitizeForCsvOrEmpty(game.getPlayer1OfficialName());
        String p2Off         = sanitizeForCsvOrEmpty(game.getPlayer2OfficialName());

        return String.join(",",
                dateStr,
                durationStr,
                difficultyStr,
                scoreStr,
                resultStr,
                p1Nick,
                p2Nick,
                p1Off,
                p2Off
        );
    }

    // ================== Players: Load / Save ==================

    /** Loads all Players data from the Player CSV file into memory. */
    public void loadPlayersFromCsv() {
        playersByEmail.clear();
        playersByName.clear();

        String csvPath = getPlayersCsvPath();
        System.out.println("Loading players from: " + csvPath);

        Path path = Paths.get(csvPath);
        if (!Files.exists(path)) {
            System.out.println("Players file not found, skipping load.");
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            // header
            String header = reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                Player p = parsePlayerFromCsvLine(line);
                if (p != null) {
                    String keyEmail = p.getEmail().toLowerCase();
                    String keyName  = p.getOfficialName().toLowerCase();
                    playersByEmail.put(keyEmail, p);
                    playersByName.put(keyName, p);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Parses a single CSV line into a Player object. */
    private Player parsePlayerFromCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        String[] parts = line.split(",", -1);
        if (parts.length < 4) {
            return null;
        }

        String officialName = parts[0].trim();
        String email        = parts[1].trim();
        String password     = parts[2].trim();
        String roleStr      = parts[3].trim();
        String avatarId     = (parts.length >= 5) ? parts[4].trim() : null;

        Role role;
        try {
            role = Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown role '" + roleStr + "', defaulting to PLAYER");
            role = Role.PLAYER;
        }

        try {
            return new Player(officialName, email, password, role, avatarId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Saves the current in-memory players list to the CSV file. */
    public void savePlayersToCsv() {
        String csvPath = getPlayersCsvPath();
        System.out.println("Saving players to: " + csvPath);

        Path path = Paths.get(csvPath);

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            // header
            writer.write("officialName,email,password,Role,Avatar");
            writer.newLine();

            for (Player p : playersByEmail.values()) {
                writer.write(formatPlayerAsCsvLine(p));
                writer.newLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Converts a Player object into a CSV line string. */
    private String formatPlayerAsCsvLine(Player p) {
        String officialName = sanitizeForCsv(p.getOfficialName());
        String email        = sanitizeForCsv(p.getEmail());
        String password     = sanitizeForCsv(p.getPassword());
        String role         = p.getRole().name();
        String avatarId     = p.getAvatarId();

        return String.join(",", officialName, email, password, role, avatarId);
    }

    // ================== CSV Sanitizers ==================

    /** Sanitizes a text field for safe CSV storage. */
    private String sanitizeForCsv(String text) {
        if (text == null) {
            return "";
        }
        // Remove commas so they don't break CSV columns
        return text.replace(",", " ");
    }

    private String sanitizeForCsvOrEmpty(String text) {
        if (text == null) {
            return "";
        }
        return sanitizeForCsv(text);
    }

    // ================== Settings Getters / Setters ==================

    public static boolean isMusicEnabled() {
        return musicEnabled;
    }

    public static void setMusicEnabled(boolean enabled) {
        musicEnabled = enabled;
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }

    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    public static boolean isTimerEnabled() {
        return timerEnabled;
    }

    public static void setTimerEnabled(boolean enabled) {
        timerEnabled = enabled;
    }

    public static boolean isSmartHintsEnabled() {
        return smartHintsEnabled;
    }

    public static void setSmartHintsEnabled(boolean enabled) {
        smartHintsEnabled = enabled;
    }

    public static boolean isAutoRemoveFlagEnabled() {
        return autoRemoveFlagEnabled;
    }

    public static void setAutoRemoveFlagEnabled(boolean enabled) {
        autoRemoveFlagEnabled = enabled;
    }

    /** Restore default values for all settings. */
    public static void resetToDefaults() {
        musicEnabled = true;
        soundEnabled = true;
        timerEnabled = true;
        smartHintsEnabled = false;
        autoRemoveFlagEnabled = true;
    }

    // ================== Player Management API ==================

    public Player findPlayerByEmail(String email) {
        if (email == null) return null;
        return playersByEmail.get(email.trim().toLowerCase());
    }

    public Player findPlayerByOfficialName(String name) {
        if (name == null) return null;
        return playersByName.get(name.trim().toLowerCase());
    }

    public boolean checkPlayerLogin(String email, String attemptedPassword) {
        Player p = findPlayerByEmail(email);
        if (p == null) return false;
        return p.checkPassword(attemptedPassword);
    }

    public Player createPlayer(String officialName,
                               String email,
                               String password,
                               Role role,
                               String avatarId) {

        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }

        String emailKey = email.trim().toLowerCase();
        if (playersByEmail.containsKey(emailKey)) {
            throw new IllegalArgumentException("Email already exists");
        }

        Player p = new Player(officialName, email, password, role, avatarId);

        playersByEmail.put(emailKey, p);
        playersByName.put(officialName.trim().toLowerCase(), p);

        savePlayersToCsv();
        return p;
    }

    public void updatePlayerPassword(String email, String newPassword) {
        Player p = findPlayerByEmail(email);
        if (p == null) {
            throw new IllegalArgumentException("No such player for email: " + email);
        }
        p.setPassword(newPassword);
        savePlayersToCsv();
    }

    // ================== Stats ==================

    public PlayerStats computeStatsForPlayer(Player player) {
        if (player == null) {
            return new PlayerStats(
                    "-", null,
                    0, 0, 0, 0, 0,
                    0, "-",
                    0, "-",
                    new int[0], new int[0], new int[0]
            );
        }
        return computeStatsForOfficialName(player.getOfficialName(), player.getAvatarId());
    }

    public PlayerStats computeStatsForOfficialName(String officialName, String avatarId) {
        if (officialName == null || officialName.isBlank()) {
            // For guest users etc.
            return new PlayerStats(
                    "-", avatarId,
                    0, 0, 0, 0, 0,
                    0, "-",
                    0, "-",
                    new int[0], new int[0], new int[0]
            );
        }

        String targetName = officialName.trim();

        int totalGames = 0;
        int wins = 0;
        int losses = 0;
        int giveUps = 0;
        int winsWithNoMistakes = 0; // TODO: update when storing mistakes per game

        int bestScore = Integer.MIN_VALUE;
        String bestScoreOpponent = "-";

        int bestTimeSeconds = Integer.MAX_VALUE;
        String bestTimeOpponent = "-";

        var easyScoresList = new ArrayList<Integer>();
        var medScoresList  = new ArrayList<Integer>();
        var hardScoresList = new ArrayList<Integer>();

        for (Game game : history.getGames()) {

            if (!isPlayerInGame(game, targetName)) {
                continue;
            }

            totalGames++;

            Difficulty diff = game.getDifficulty();
            int score = game.getFinalScore();

            // progression by difficulty
            if (diff == Difficulty.EASY) {
                easyScoresList.add(score);
            } else if (diff == Difficulty.MEDIUM) {
                medScoresList.add(score);
            } else if (diff == Difficulty.HARD) {
                hardScoresList.add(score);
            }

            // result-based counters
            GameResult res = game.getResult();
            switch (res) {
                case WIN:
                    wins++;
                    // winsWithNoMistakes will be updated when mistakes per game are stored
                    break;
                case LOSE:
                    losses++;
                    break;
                case GIVE_UP:
                    giveUps++;
                    break;
                default:
                    break;
            }

            // Opponent name
            String opponent = getOpponentName(game, targetName);

            // best score (max)
            if (score > bestScore) {
                bestScore = score;
                bestScoreOpponent = opponent;
            }

            // best time (min duration)
            int durationSeconds = parseDuration(game.getDurationFormatted());
            if (durationSeconds > 0 && durationSeconds < bestTimeSeconds) {
                bestTimeSeconds = durationSeconds;
                bestTimeOpponent = opponent;
            }
        }

        // Normalize for "no games"
        if (totalGames == 0) {
            bestScore = 0;
            bestTimeSeconds = 0;
            bestScoreOpponent = "-";
            bestTimeOpponent = "-";
        }

        int[] easyScores = easyScoresList.stream().mapToInt(Integer::intValue).toArray();
        int[] medScores  = medScoresList.stream().mapToInt(Integer::intValue).toArray();
        int[] hardScores = hardScoresList.stream().mapToInt(Integer::intValue).toArray();

        return new PlayerStats(
                targetName,
                avatarId,
                totalGames,
                wins,
                losses,
                giveUps,
                winsWithNoMistakes,
                bestScore,
                bestScoreOpponent,
                bestTimeSeconds,
                bestTimeOpponent,
                easyScores,
                medScores,
                hardScores
        );
    }

    private boolean isPlayerInGame(Game game, String officialName) {
        if (officialName == null || officialName.isBlank()) {
            return false;
        }
        String target = officialName.trim();

        String p1Off = game.getPlayer1OfficialName();
        String p2Off = game.getPlayer2OfficialName();

        if (p1Off != null && p1Off.trim().equalsIgnoreCase(target)) {
            return true;
        }
        if (p2Off != null && p2Off.trim().equalsIgnoreCase(target)) {
            return true;
        }

        // Optional: match by nickname if official name is missing
        String p1Nick = game.getPlayer1Nickname();
        String p2Nick = game.getPlayer2Nickname();

        if (p1Nick != null && p1Nick.trim().equalsIgnoreCase(target)) {
            return true;
        }
        if (p2Nick != null && p2Nick.trim().equalsIgnoreCase(target)) {
            return true;
        }

        return false;
    }

    private String getOpponentName(Game game, String officialName) {
        if (officialName == null) {
            return "-";
        }
        String target = officialName.trim();

        String p1Off = game.getPlayer1OfficialName();
        String p2Off = game.getPlayer2OfficialName();
        String p1Nick = game.getPlayer1Nickname();
        String p2Nick = game.getPlayer2Nickname();

        // If target is P1
        if (p1Off != null && p1Off.trim().equalsIgnoreCase(target)) {
            if (p2Off != null && !p2Off.isBlank()) return p2Off;
            if (p2Nick != null && !p2Nick.isBlank()) return p2Nick;
            return "-";
        }

        // If target is P2
        if (p2Off != null && p2Off.trim().equalsIgnoreCase(target)) {
            if (p1Off != null && !p1Off.isBlank()) return p1Off;
            if (p1Nick != null && !p1Nick.isBlank()) return p1Nick;
            return "-";
        }

        // Maybe we matched by nickname:
        if (p1Nick != null && p1Nick.trim().equalsIgnoreCase(target)) {
            if (p2Off != null && !p2Off.isBlank()) return p2Off;
            if (p2Nick != null && !p2Nick.isBlank()) return p2Nick;
        }

        if (p2Nick != null && p2Nick.trim().equalsIgnoreCase(target)) {
            if (p1Off != null && !p1Off.isBlank()) return p1Off;
            if (p1Nick != null && !p1Nick.isBlank()) return p1Nick;
        }

        return "-";
    }
}
