package model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Central system data manager.
 * For now it is responsible for:
 *  - keeping the History object in memory
 *  - loading/saving History from/to a CSV file.
 */
public class SysData {

    // === 1) Singleton pattern ===
    private static final SysData INSTANCE = new SysData();

    public static SysData getInstance() {
        return INSTANCE;
    }

    // === 2) Fields ===
    private final History history = new History();

    // You can change this path later if you want to put the CSV elsewhere
    private static final String HISTORY_FILE_NAME = "history.csv";

    private SysData() {
        // private constructor, no one else can create instances
    }

    public History getHistory() {
        return history;
    }

    /**
     * Convenience method used by the game controller:
     * adds a finished game to the history (in memory).
     * Don't forget later to call saveHistoryToCsv() if you want to persist it.
     */
    public void addGameToHistory(Game game) {
        history.addGame(game);
    }

    // ====================================================
    //  CSV LOADING
    // ====================================================

    /**
     * Loads the history from the CSV file into memory.
     * If the file does not exist, history will just stay empty.
     */
    public void loadHistoryFromCsv() {
        history.clear();

        Path path = Paths.get(HISTORY_FILE_NAME);
        if (!Files.exists(path)) {
            // No history yet, nothing to load
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line = reader.readLine();

            // Optional: skip header if present
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
            // You might later show an Alert instead of just printing
        }
    }

    /**
     * Expected format:
     * date,duration,difficulty,score,result,player1,player2
     */
    
    
    private Game parseGameFromCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        String[] parts = line.split(",", -1);  // -1 -> keep empty strings if any
        if (parts.length != 7) {
            // Not in expected format
            return null;
        }

        try {
            LocalDate date = LocalDate.parse(parts[0]); // "2025-11-26"
            int durationSeconds =  parseDuration(parts[1]); 
            Difficulty difficulty = Difficulty.valueOf(parts[2]);
            int score = Integer.parseInt(parts[3]);
            GameResult result = GameResult.valueOf(parts[4]);   // WIN / LOSE
            String player1 = parts[5];
            String player2 = parts[6];

            return new Game(player1, player2, difficulty, score ,result , date, durationSeconds);

        } catch (Exception e) {
            // If any parsing fails, skip this line
            e.printStackTrace();
            return null;
        }
    }
    
 // Accept "M:SS", "MM:SS", or pure seconds like "123"
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
            return Integer.parseInt(text); // assume raw seconds
        }
    }
    
    

    // ====================================================
    //  CSV SAVING
    // ====================================================

    /**
     * Saves the current history to the CSV file.
     * Will overwrite the existing file.
     */
    public void saveHistoryToCsv() {
        Path path = Paths.get(HISTORY_FILE_NAME);
        List<Game> games = history.getGames();

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            // Header row (optional but nice for readability)
            writer.write("date,duration,difficulty,score,result,player1,player2");
            writer.newLine();

            for (Game game : games) {
                writer.write(formatGameAsCsvLine(game));
                writer.newLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
            // Later you may want to show an alert to the user
        }
    }

    /**
     * Convert a Game object to a CSV line according to our agreed format.
     */
    private String formatGameAsCsvLine(Game game) {
        String dateStr = game.getDate().toString(); // "2025-11-26"
        String durationStr = game.getDurationFormatted();    // "21:15"
        String difficultyStr = game.getDifficulty().name();
        String scoreStr = Integer.toString(game.getFinalScore());
        String resultStr = game.getResult().name();          // WIN / LOSE
        String p1 = sanitizeForCsv(game.getPlayer1Nickname());
        String p2 = sanitizeForCsv(game.getPlayer2Nickname());

        return String.join(",", dateStr, durationStr, difficultyStr, scoreStr, resultStr, p1, p2);
    }

    /**
     * Very simple CSV "sanitizer".
     * For now we just replace commas with spaces to avoid breaking the CSV.
     * (You can improve this later with proper quoting if needed.)
     */
    private String sanitizeForCsv(String text) {
        if (text == null) {
            return "";
        }
        return text.replace(",", " ");
    }
}
