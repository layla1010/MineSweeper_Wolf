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


public class SysData {

    private static final SysData INSTANCE = new SysData();

    public static SysData getInstance() {
        return INSTANCE;
    }

    private final History history = new History();

    // You can change this path later if you want to put the CSV elsewhere
    private static final String HISTORY_FILE_NAME = "history.csv";

    private SysData() {
        // private constructor, no one else can create instances
    }

    public History getHistory() {
        return history;
    }

    
    public void addGameToHistory(Game game) {
        history.addGame(game);
    }

    
    public void loadHistoryFromCsv() {
        history.clear();

        Path path = Paths.get(HISTORY_FILE_NAME);
        if (!Files.exists(path)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line = reader.readLine();

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

    
    private Game parseGameFromCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        String[] parts = line.split(",", -1);  
        if (parts.length != 7) {
            return null;
        }

        try {
            LocalDate date = LocalDate.parse(parts[0]); 
            int durationSeconds =  parseDuration(parts[1]); 
            Difficulty difficulty = Difficulty.valueOf(parts[2]);
            int score = Integer.parseInt(parts[3]);
            GameResult result = GameResult.valueOf(parts[4]);   
            String player1 = parts[5];
            String player2 = parts[6];

            return new Game(player1, player2, difficulty, score ,result , date, durationSeconds);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
            return Integer.parseInt(text); // assume raw seconds
        }
    }
    
    

    
    public void saveHistoryToCsv() {
        Path path = Paths.get(HISTORY_FILE_NAME);
        List<Game> games = history.getGames();

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write("date,duration,difficulty,score,result,player1,player2");
            writer.newLine();

            for (Game game : games) {
                writer.write(formatGameAsCsvLine(game));
                writer.newLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

  
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

   
    private String sanitizeForCsv(String text) {
        if (text == null) {
            return "";
        }
        return text.replace(",", " ");
/*
 * Aya Ala Deen – Settings Screen Implementation Documentation
 */


public class SysData {

   
    private static boolean musicEnabled = true;

    /** Controls whether sound effects are enabled. */
    private static boolean soundEnabled = true;

    /** Controls whether a game timer should be shown and used. */
    private static boolean timerEnabled = true;

    /** Controls whether "smart hints" are enabled in the game. */
    private static boolean smartHintsEnabled = false;

    /** Controls whether flags are automatically removed in some situations. */
    private static boolean autoRemoveFlagEnabled = true;


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

    
    public static void resetToDefaults() {
        musicEnabled = true;
        soundEnabled = true;
        timerEnabled = true;
        smartHintsEnabled = false;
        autoRemoveFlagEnabled = true;
    }
}
