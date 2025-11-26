package test;

import model.SysData;
import model.Game;
import model.Difficulty;

import java.time.LocalDate;
import java.time.LocalTime;

public class HistoryTest {

    public static void main(String[] args) {

        // 1) Get SysData instance
        SysData db = SysData.getInstance();

        // 2) Create one sample Game record
        Game g = new Game(
                "Adan",
                "Layla",
                Difficulty.EASY,
                55,
                LocalDate.now(),
                136
        );

        // 3) Add the game into memory
        db.addGameToHistory(g);

        // 4) Save to CSV file
        db.saveHistoryToCsv();

        // 5) Clear memory and reload from CSV
        db.getHistory().clear();
        db.loadHistoryFromCsv();

        // 6) Print results to Eclipse console
        System.out.println("=== Loaded History: ===");
        db.getHistory().getGames().forEach(System.out::println);
    }
}
