package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the list of all past games.
 * Acts as an in-memory "history" that the History screen will display.
 */
public class History {

    private final List<Game> games = new ArrayList<>();

    /**
     * Add a finished game to the history.
     */
    public void addGame(Game game) {
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null");
        }
        games.add(game);
    }

    /**
     * @return an unmodifiable list of all games (to avoid external modification).
     */
    public List<Game> getGames() {
        return Collections.unmodifiableList(games);
    }

    /**
     * Remove all existing history entries (used when reloading from CSV).
     */
    public void clear() {
        games.clear();
    }

    public int size() {
        return games.size();
    }

    public boolean isEmpty() {
        return games.isEmpty();
    }
}
