package model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class SysData {


    /** Singleton instance of SysData. */
    private static final SysData INSTANCE = new SysData();

    public static SysData getInstance() {
        return INSTANCE;
    }

    private static final Logger LOG = Logger.getLogger(SysData.class.getName());

    private final AtomicBoolean historyLoaded = new AtomicBoolean(false);
    private final AtomicBoolean playersLoaded = new AtomicBoolean(false);

    
    /** Stores all game history records. */
    private final History history = new History();

    /** Player lookup by email (lowercased). */
    private final Map<String, Player> playersByEmail = new HashMap<>();

    /** Player lookup by official name (lowercased). */
    private final Map<String, Player> playersByName = new HashMap<>();

    /** CSV file name (kept for reference – actual path is resolved dynamically). */
    private static final String HISTORY_FILE_NAME = "/data/history.csv";


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
    
    
    private final List<Question> questions = new ArrayList<>();

    /** Private constructor (singleton). */
    private SysData() {
    }


    /** Returns the History object that holds all Game records. */
    public History getHistory() {
        return history;
    }
    
    public List<Question> getAllQuestions() {
        return new ArrayList<>(questions);
    }

    public void deleteQuestionById(int id) {
        questions.removeIf(q -> q.getId() == id);
        renumberQuestionIds();
        saveQuestionsToCsv();
    }
    
    private void renumberQuestionIds() {
        int id = 1;
        for (Question q : questions) {
            q.setId(id++);
        }
    }

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


    /** Adds a single Game record to the history. */
    public void addGameToHistory(Game game) {
        history.addGame(game);
    }

    /** Loads all game history from the history CSV file into memory. */
    private void loadHistoryFromCsvInternal() {
        history.clear();

        String csvPath = getHistoryCsvPath();
        LOG.info("Loading history from: " + csvPath);

        Path path = Paths.get(csvPath);
        if (!Files.exists(path)) {
        	LOG.warning("History file not found, skipping load.");
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
        if (parts.length < 10) {
            return null;
        }

        try {
            LocalDate date          = LocalDate.parse(parts[0]);       // "2025-11-26"
            int durationSeconds     = parseDuration(parts[1]);         // "21:15" or "1275"
            Difficulty difficulty   = Difficulty.valueOf(parts[2]);
            int score               = Integer.parseInt(parts[3]);
            GameResult result       = GameResult.valueOf(parts[4]);    // WIN / LOSE / GIVE_UP
            String nick_player1     = parts[5];
            String nick_player2     = parts[6];
            String off_player1      = toNullIfBlank(parts[7]);
            String off_player2      = toNullIfBlank(parts[8]);
            boolean winWithoutMistakes = Boolean.parseBoolean(parts[9].trim());

            // NEW: optional avatar columns (for older CSVs these may not exist)
            String avatar1 = (parts.length > 10) ? toNullIfBlank(parts[10]) : null;
            String avatar2 = (parts.length > 11) ? toNullIfBlank(parts[11]) : null;

            return new Game(
                    off_player1,
                    off_player2,
                    nick_player1,
                    nick_player2,
                    difficulty,
                    score,
                    result,
                    date,
                    durationSeconds,
                    winWithoutMistakes,
                    avatar1,
                    avatar2
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
            writer.write("date,duration,difficulty,score,result,player1Nickname,player2Nickname,player1Official,player2Official,winWithoutMistakes,player1Avatar,player2Avatar");
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
        String noMistakesStr = Boolean.toString(game.isWinWithoutMistakes());
        String p1Avatar = sanitizeForCsvOrEmpty(game.getPlayer1AvatarPath());
        String p2Avatar = sanitizeForCsvOrEmpty(game.getPlayer2AvatarPath());

        
        return String.join(",",
                dateStr,
                durationStr,
                difficultyStr,
                scoreStr,
                resultStr,
                p1Nick,
                p2Nick,
                p1Off,
                p2Off,
                noMistakesStr,
                p1Avatar,
                p2Avatar
        );
    }


    /** Loads all Players data from the Player CSV file into memory. */
    private void loadPlayersFromCsvInternal() {
        playersByEmail.clear();
        playersByName.clear();

        String csvPath = getPlayersCsvPath();
        LOG.info("Loading players from: " + csvPath);


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
        int winsWithNoMistakes = 0; // will update later once we track mistakes per game

        int bestScore = Integer.MIN_VALUE;
        String bestScoreOpponent = "-";

        // best time: SHORTEST WINNING TIME ONLY
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

                    if (game.isWinWithoutMistakes()) {
                        winsWithNoMistakes++;
                    }

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

            // best score (max over ALL results)
            if (score > bestScore) {
                bestScore = score;
                bestScoreOpponent = opponent;
            }

            // best time (MIN duration, but ONLY among WINS)
            if (res == GameResult.WIN) {
            	int durationSeconds = game.getDurationSeconds();
                if (durationSeconds > 0 && durationSeconds < bestTimeSeconds) {
                    bestTimeSeconds = durationSeconds;
                    bestTimeOpponent = opponent;
                }
            }
        }

        // Normalize for "no games"
        if (totalGames == 0) {
            bestScore = 0;
            bestTimeSeconds = 0;
            bestScoreOpponent = "-";
            bestTimeOpponent = "-";
        } else {
            // Player has some games but ZERO wins → no “best winning time”
            if (wins == 0) {
                bestTimeSeconds = 0;
                bestTimeOpponent = "-";
            }
            // If player has games but all scores <= 0 we keep bestScore=0, opponent "-"
            if (bestScore == Integer.MIN_VALUE) {
                bestScore = 0;
                bestScoreOpponent = "-";
            }
            if (bestTimeSeconds == Integer.MAX_VALUE) {
                bestTimeSeconds = 0;
                bestTimeOpponent = "-";
            }
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
    
    /**
     * Loads history from CSV only once per application run.
     * Safe to call from any controller.
     */
    public void ensureHistoryLoaded() {
        if (historyLoaded.compareAndSet(false, true)) {
            loadHistoryFromCsvInternal();
        }
    }

    /**
     * Loads players from CSV only once per application run.
     * Safe to call from any controller.
     */
    public void ensurePlayersLoaded() {
        if (playersLoaded.compareAndSet(false, true)) {
            loadPlayersFromCsvInternal();
        }
    }

    public void reloadHistoryFromCsv() {
        loadHistoryFromCsvInternal();
        historyLoaded.set(true);
    }

    public void reloadPlayersFromCsv() {
        loadPlayersFromCsvInternal();
        playersLoaded.set(true);
    }

    //================= Questions Manager =================//

    /**
     * Loads all questions from the CSV file into the internal questions list.
     * Invalid rows are skipped with an error message.
     */
    public void loadQuestionsFromCsv() {
        questions.clear();

        String csvPath = getQuestionsCsvPath();
        System.out.println("Loading questions from: " + csvPath);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(csvPath), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                return;
            }

            String delimiter = headerLine.contains(";") ? ";" : ",";

            String[] headers = headerLine.split(delimiter, -1);
            Map<String, Integer> col = new HashMap<>();

            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim().replace("\uFEFF", ""); // remove BOM if present
                col.put(h, i);
            }

            String line;
            int rowNumber = 1;

            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty()) continue;

                String[] cells = line.split(delimiter, -1);

                Integer iA = col.get("A");
                Integer iB = col.get("B");
                Integer iC = col.get("C");
                Integer iD = col.get("D");
                Integer iDifficulty = col.get("Difficulty");
                Integer iId = col.get("ID");
                Integer iQuestion = col.get("Question");
                Integer iCorrect = col.get("Correct Answer");

                // Basic header validation
                if (iA == null || iB == null || iC == null || iD == null ||
                        iDifficulty == null || iId == null || iQuestion == null || iCorrect == null) {
                    System.err.println("Questions CSV header is missing required columns.");
                    return;
                }

                int maxIndex = Math.max(
                        Math.max(iA, iB),
                        Math.max(iC, Math.max(iD,
                        Math.max(iDifficulty, Math.max(iId,
                        Math.max(iQuestion, iCorrect)))))
                );

                if (cells.length <= maxIndex) {
                    System.err.println("Skipping row " + rowNumber + " – not enough columns");
                    continue;
                }

                String optA = cells[iA].trim();
                String optB = cells[iB].trim();
                String optC = cells[iC].trim();
                String optD = cells[iD].trim();
                String difficultyRaw = cells[iDifficulty].trim();
                String idStr = cells[iId].trim();
                String questionText = cells[iQuestion].trim();
                String correctLetter = cells[iCorrect].trim();

                if (!idStr.matches("\\d+")) {
                    System.err.println("Skipping row " + rowNumber + " – invalid ID");
                    continue;
                }

                String difficultyNum = difficultyRaw.matches("[1-4]")
                        ? difficultyRaw
                        : mapDifficultyToNumber(difficultyRaw);

                if (difficultyNum == null) {
                    System.err.println("Skipping row " + rowNumber + " – invalid difficulty");
                    continue;
                }

                if (!correctLetter.matches("[A-Da-d]")) {
                    System.err.println("Skipping row " + rowNumber + " – invalid correct answer");
                    continue;
                }

                if (questionText.isEmpty()) {
                    System.err.println("Skipping row " + rowNumber + " – empty question");
                    continue;
                }

                Question q = new Question(
                        Integer.parseInt(idStr),
                        mapDifficulty(difficultyNum),   // "1..4" -> "Easy..Expert"
                        questionText,
                        optA,
                        optB,
                        optC,
                        optD,
                        mapCorrectLetter(correctLetter) // "A..D" -> 1..4
                );

                questions.add(q);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the internal questions list back to the CSV file.
     * Question IDs are renumbered sequentially before saving.
     */
    private void saveQuestionsToCsv() {
        String filePath = getQuestionsCsvPath();
        System.out.println("Saving questions to: " + filePath);

        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {

            pw.println("ID,Question,Difficulty,A,B,C,D,Correct Answer");

            int newId = 1;
            for (Question q : questions) {
                q.setId(newId++);

                String line = String.join(",",
                        String.valueOf(q.getId()),
                        escapeCsv(q.getText()),
                        mapDifficultyToNumber(q.getDifficulty()),
                        escapeCsv(q.getOptA()),
                        escapeCsv(q.getOptB()),
                        escapeCsv(q.getOptC()),
                        escapeCsv(q.getOptD()),
                        mapCorrectNumberToLetter(q.getCorrectOption())
                );

                pw.println(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Escapes values that contain commas/quotes/newlines for CSV.
     */
    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            s = s.replace("\"", "\"\"");
            return "\"" + s + "\"";
        }
        return s;
    }

    /**
     * Resolves the actual CSV file path for Data/Questionsss.csv.
     * Works both from IDE (classes folder) and from the JAR.
     */
    private static String getQuestionsCsvPath() {
        try {
            String path = SysData.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath();

            String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8.name());

            // Windows fix (same logic as history/users)
            if (decoded.length() > 2
                    && decoded.charAt(0) == '/'
                    && Character.isLetter(decoded.charAt(1))
                    && decoded.charAt(2) == ':') {
                decoded = decoded.substring(1);
            }

            if (decoded.contains(".jar")) {
                decoded = decoded.substring(0, decoded.lastIndexOf('/'));
                return decoded + "/Data/Questionsss.csv";
            } else {
                if (decoded.contains("target/classes/")) {
                    decoded = decoded.substring(0, decoded.lastIndexOf("target/classes/"));
                } else if (decoded.contains("bin/")) {
                    decoded = decoded.substring(0, decoded.lastIndexOf("bin/"));
                } else {
                    return "Data/Questionsss.csv";
                }

                return decoded + "src/main/resources/Data/Questionsss.csv";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Data/Questionsss.csv";
        }
    }

    /**
     * Converts difficulty number to text.
     */
    private String mapDifficulty(String num) {
        return switch (num) {
            case "1" -> "Easy";
            case "2" -> "Medium";
            case "3" -> "Hard";
            case "4" -> "Expert";
            default -> "Unknown";
        };
    }

    /**
     * Converts correct answer letter A/B/C/D to index 1..4.
     */
    private int mapCorrectLetter(String letter) {
        return switch (letter.toUpperCase()) {
            case "A" -> 1;
            case "B" -> 2;
            case "C" -> 3;
            case "D" -> 4;
            default -> 1;
        };
    }

    /**
     * Difficulty text (Easy/Medium/Hard/Expert) to number (1..4).
     */
    static String mapDifficultyToNumber(String difficultyText) {
        if (difficultyText == null) return null;
        return switch (difficultyText.trim().toLowerCase()) {
            case "easy" -> "1";
            case "medium" -> "2";
            case "hard" -> "3";
            case "expert" -> "4";
            default -> null; // IMPORTANT: return null so caller can treat as invalid
        };
    }

    /**
     * correctOption 1..4 → "A..D".
     */
    private String mapCorrectNumberToLetter(int correctOption) {
        return switch (correctOption) {
            case 1 -> "A";
            case 2 -> "B";
            case 3 -> "C";
            case 4 -> "D";
            default -> "A";
        };
    }

    public void addQuestion(
            String difficultyText,
            String questionText,
            String optA,
            String optB,
            String optC,
            String optD,
            String correctLetter
    ) {
        loadQuestionsFromCsv(); // ensure up-to-date

        int nextId = questions.size() + 1;

        Question q = new Question(
                nextId,
                difficultyText,
                questionText,
                optA,
                optB,
                optC,
                optD,
                mapCorrectLetter(correctLetter)
        );

        questions.add(q);
        saveQuestionsToCsv();
    }

    public void updateQuestion(
            int id,
            String difficultyText,
            String questionText,
            String optA,
            String optB,
            String optC,
            String optD,
            String correctLetter
    ) {
        for (Question q : questions) {
            if (q.getId() == id) {
                q.setDifficulty(difficultyText);
                q.setText(questionText);
                q.setOptA(optA);
                q.setOptB(optB);
                q.setOptC(optC);
                q.setOptD(optD);
                q.setCorrectOption(mapCorrectLetter(correctLetter));
                break;
            }
        }

        saveQuestionsToCsv();
    }

    public int getNextQuestionId() {
        return questions.size() + 1;
    }
}
