package control;

import java.time.LocalDate;

import model.Game;
import model.GameResult;
import model.Player;
import model.SysData;
import util.SessionManager;

public class GameHistoryServiceController {

    public void saveCurrentGameToHistory(GameStateController s) {
        if (s.config == null) return;

        GameResult result = s.gameWon ? GameResult.WIN : GameResult.LOSE;

        Player p1 = SessionManager.getPlayer1();
        Player p2 = SessionManager.getPlayer2();

        String player1Official = (p1 != null) ? p1.getOfficialName() : null;
        String player2Official = (p2 != null) ? p2.getOfficialName() : null;

        String player1Nick = s.config.getPlayer1Nickname();
        String player2Nick = s.config.getPlayer2Nickname();

        boolean winWithoutMistakes = (s.gameWon && !s.mistakeMade);

        Game gameRecord = new Game(
                player1Official,
                player2Official,
                player1Nick,
                player2Nick,
                s.difficulty,
                s.score,
                result,
                LocalDate.now(),
                s.elapsedSeconds,
                winWithoutMistakes,
                s.config.getPlayer1AvatarPath(),
                s.config.getPlayer2AvatarPath()
        );

        SysData sysData = SysData.getInstance();
        sysData.addGameToHistory(gameRecord);
        sysData.saveHistoryToCsv();

        System.out.println("Saved game: " + gameRecord);
    }

    public void saveGiveUpGame(GameStateController s) {
        if (s.config == null) return;
        if (s.gameOver) return;

        GameResult result = GameResult.GIVE_UP;

        Player p1 = SessionManager.getPlayer1();
        Player p2 = SessionManager.getPlayer2();

        String player1Official = (p1 != null) ? p1.getOfficialName() : null;
        String player2Official = (p2 != null) ? p2.getOfficialName() : null;

        String player1Nick = s.config.getPlayer1Nickname();
        String player2Nick = s.config.getPlayer2Nickname();

        Game giveUpGame = new Game(
                player1Official,
                player2Official,
                player1Nick,
                player2Nick,
                s.difficulty,
                s.score,
                result,
                LocalDate.now(),
                s.elapsedSeconds,
                false,
                s.config.getPlayer1AvatarPath(),
                s.config.getPlayer2AvatarPath()
        );

        SysData sysData = SysData.getInstance();
        sysData.addGameToHistory(giveUpGame);
        sysData.saveHistoryToCsv();

        System.out.println("Saved GIVE_UP game: " + giveUpGame);
    }
}
