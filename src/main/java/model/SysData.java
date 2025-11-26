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
     * Parses a CSV line into a Game object.
     * Expected format:
     * date,time,difficulty,score,player1,player2
     *
     * Example:
     * 2025-11-26,21:15,EASY,42,Alice,Bob
     */
    private Game parseGameFromCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        String[] parts = line.split(",", -1);  // -1 -> keep empty strings if any
        if (parts.length != 6) {
            // Not in expected format
            return null;
        }

        try {
            LocalDate date = LocalDate.parse(parts[0]); // "2025-11-26"
            LocalTime time = LocalTime.parse(parts[1]); // "21:15"
            Difficulty difficulty = Difficulty.valueOf(parts[2]);
            int score = Integer.parseInt(parts[3]);
            String player1 = parts[4];
            String player2 = parts[5];

            return new Game(player1, player2, difficulty, score, date, time);

        } catch (Exception e) {
            // If any parsing fails, skip this line
            e.printStackTrace();
            return null;
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
            writer.write("date,time,difficulty,score,player1,player2");
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
        String timeStr = game.getTimeAsString();    // "21:15"
        String difficultyStr = game.getDifficulty().name();
        String scoreStr = Integer.toString(game.getFinalScore());
        String p1 = sanitizeForCsv(game.getPlayer1Nickname());
        String p2 = sanitizeForCsv(game.getPlayer2Nickname());

        return String.join(",", dateStr, timeStr, difficultyStr, scoreStr, p1, p2);
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
