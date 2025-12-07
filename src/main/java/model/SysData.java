package model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

//This is a singleton class "SysData" that Stores the global game history, loads and saves history to a CSV file and holds global settings (music, sound, timer, smart hints, auto-remove flag).
public class SysData {

    //Singleton instance
    private static final SysData INSTANCE = new SysData();


    public static SysData getInstance() {
        return INSTANCE;
    }

    //History storage
    private final History history = new History();
    
    private final Map<String, Player> playersByEmail = new HashMap<>();
    private final Map<String, Player> playersByName = new HashMap<>();
    


    //CSV file name (you can change location if needed)
    private static final String HISTORY_FILE_NAME = "/data/history.csv";


    //Controls whether background music is enabled.
    private static boolean musicEnabled = true;

    //Controls whether sound effects are enabled. 
    private static boolean soundEnabled = true;

    //Controls whether a game timer should be shown and used.
    private static boolean timerEnabled = true;

    //Controls whether "smart hints" are enabled in the game.
    private static boolean smartHintsEnabled = false;

    //Controls whether flags are automatically removed in some situations.
    private static boolean autoRemoveFlagEnabled = true;

    // Private constructor (singleton)
    private SysData() {
    }

    //Returns the History object that holds all Game records.
    public History getHistory() {
        return history;
    }
    
    //Resolves the full path to the history CSV file.
    private static String getHistoryCsvPath() {
        try {
            String path = SysData.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath();

            String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8.name());

            if (decoded.length() > 2
                    && decoded.charAt(0) == '/'
                    && Character.isLetter(decoded.charAt(1))
                    && decoded.charAt(2) == ':') {
                decoded = decoded.substring(1);  
            }

            if (decoded.contains(".jar")) {
                decoded = decoded.substring(0, decoded.lastIndexOf('/'));
                return decoded + "/Data/history.csv";
            } 
            else {
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
    
 // Resolves the full path to the players CSV file.
    private static String getPlayersCsvPath() {
        try {
            String path = SysData.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath();

            String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8.name());

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


    //Adds a single Game record to the history.
    public void addGameToHistory(Game game) {
        history.addGame(game);
    }
    
    
    //Loads all game history from the history CSV file into memory.
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

            // skip header if present
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
    
    
    //Loads all Players data from the Player CSV file into memory.
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
    
    //Parses a single CSV line into a Player object.
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

        Role role;
        
        try {
            role = Role.valueOf(roleStr.toUpperCase());  
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown role '" + roleStr + "', defaulting to PLAYER");
            role = Role.PLAYER;
        }
        try {
            return new Player(officialName, email, password, role);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    //Parses a single CSV line into a Game object.
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
            GameResult result = GameResult.valueOf(parts[4]); // WIN / LOSE
            String nick_player1 = parts[5];
            String nick_player2 = parts[6];
            String off_player1 = toNullIfBlank(parts[7]);
            String off_player2 = toNullIfBlank(parts[8]);

            return new Game(off_player1, off_player2,nick_player1, nick_player2, difficulty, score, result, date, durationSeconds);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String toNullIfBlank(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }
    
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
    //Saves the current in-memory history list to the CSV file.
    public void saveHistoryToCsv() {
        String csvPath = getHistoryCsvPath();
        System.out.println("Saving history to: " + csvPath);

        Path path = Paths.get(csvPath);
        List<Game> games = history.getGames();

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
    
    
    //Saves the current in-memory players list to the CSV file.
    public void savePlayersToCsv() {
        String csvPath = getPlayersCsvPath();
        System.out.println("Saving players to: " + csvPath);

        Path path = Paths.get(csvPath);

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            // header
            writer.write("officialName,email,password,Role");
            writer.newLine();

            for (Player p : playersByEmail.values()) {
                writer.write(formatPlayerAsCsvLine(p));
                writer.newLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //Converts a Game object into a CSV line string.
    private String formatGameAsCsvLine(Game game) {
        String dateStr = game.getDate().toString();               // "2025-11-26"
        String durationStr = game.getDurationFormatted();         // "21:15"
        String difficultyStr = game.getDifficulty().name();
        String scoreStr = Integer.toString(game.getFinalScore());
        String resultStr = game.getResult().name();               // WIN / LOSE
        String p1Nick = sanitizeForCsv(game.getPlayer1Nickname());
        String p2Nick = sanitizeForCsv(game.getPlayer2Nickname());
        String p1Off  = sanitizeForCsvOrEmpty(game.getPlayer1OfficialName());
        String p2Off  = sanitizeForCsvOrEmpty(game.getPlayer2OfficialName());

        return String.join(",", dateStr, durationStr, difficultyStr, scoreStr, resultStr, p1Nick, p2Nick, p1Off, p2Off
        );
    }
    
    
    //Converts a Player object into a CSV line string.
    private String formatPlayerAsCsvLine(Player p) {
        String officialName = sanitizeForCsv(p.getOfficialName());
        String email        = sanitizeForCsv(p.getEmail());
        String password     = sanitizeForCsv(p.getPassword()); 
        String role         = p.getRole().name();
        
        return String.join(",", officialName, email, password, role);
    }

    
    
    //Sanitizes a text field for safe CSV storage.
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
    
    public Player createPlayer(String officialName, String email, String password, Role role) {
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }

        String emailKey = email.trim().toLowerCase();
        if (playersByEmail.containsKey(emailKey)) {
            throw new IllegalArgumentException("Email already exists");
        }

        Player p = new Player(officialName, email, password, role);

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

}
