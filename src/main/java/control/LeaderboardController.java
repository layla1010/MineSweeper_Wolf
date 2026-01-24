package control;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import model.Game;
import model.GameResult;
import model.Player;
import model.SysData;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardController {

    public enum Metric { WINS, WIN_RATE, AVG_SCORE, AVG_WIN_TIME }
    public enum TimeWindow { ALL_TIME, LAST_7_DAYS, LAST_30_DAYS, THIS_MONTH }

    @FXML private StackPane root;

    @FXML private ComboBox<TimeWindow> timeWindowCombo;
    @FXML private ComboBox<Metric> metricCombo;

    @FXML private TabPane tabs;

    @FXML private TableView<LeaderboardRow> playersTable;
    @FXML private TableColumn<LeaderboardRow, Number> pRankCol;
    @FXML private TableColumn<LeaderboardRow, String> pNameCol;
    @FXML private TableColumn<LeaderboardRow, Number> pWinsCol;
    @FXML private TableColumn<LeaderboardRow, Number> pGamesCol;
    @FXML private TableColumn<LeaderboardRow, Number> pWinRateCol;
    @FXML private TableColumn<LeaderboardRow, Number> pAvgScoreCol;
    @FXML private TableColumn<LeaderboardRow, String> pAvgTimeCol;

    @FXML private TableColumn<LeaderboardRow, String> pAvatarCol;

    @FXML private TableView<LeaderboardRow> teamsTable;
    @FXML private TableColumn<LeaderboardRow, Number> tRankCol;

    // NEW: separate names instead of "Team"
    @FXML private TableColumn<LeaderboardRow, String> tP1NameCol;
    @FXML private TableColumn<LeaderboardRow, String> tP2NameCol;

    @FXML private TableColumn<LeaderboardRow, Number> tWinsCol;
    @FXML private TableColumn<LeaderboardRow, Number> tGamesCol;
    @FXML private TableColumn<LeaderboardRow, Number> tWinRateCol;
    @FXML private TableColumn<LeaderboardRow, Number> tAvgScoreCol;
    @FXML private TableColumn<LeaderboardRow, String> tAvgTimeCol;

    @FXML private TableColumn<LeaderboardRow, String> tAvatar1Col;
    @FXML private TableColumn<LeaderboardRow, String> tAvatar2Col;

    private final ObservableList<LeaderboardRow> playerRows = FXCollections.observableArrayList();
    private final ObservableList<LeaderboardRow> teamRows   = FXCollections.observableArrayList();

    // ------------------------------- Stats aggregation -------------------------------

    private static final class Stats {
        int games;
        int wins;

        int totalScore;

        int totalWinTimeSeconds;
        int winTimeCount;

        void addGame(int score) {
            games++;
            int safe = Math.max(0, score);
            totalScore += safe;
        }

        void addWin(int durationSeconds) {
            wins++;
            if (durationSeconds > 0) {
                totalWinTimeSeconds += durationSeconds;
                winTimeCount++;
            }
        }

        double winRate() {
            return (games <= 0) ? 0.0 : (wins * 100.0) / games;
        }

        double avgScore() {
            return (games <= 0) ? 0.0 : totalScore / (double) games;
        }

        double avgWinTimeSeconds() {
            return (winTimeCount <= 0) ? 0.0 : totalWinTimeSeconds / (double) winTimeCount;
        }
    }

    @FXML
    private void initialize() {
        SysData.getInstance().ensureHistoryLoaded();
        SysData.getInstance().ensurePlayersLoaded();

        if (timeWindowCombo != null) {
            timeWindowCombo.setItems(FXCollections.observableArrayList(TimeWindow.values()));
            timeWindowCombo.getSelectionModel().select(TimeWindow.ALL_TIME);
            timeWindowCombo.setOnAction(e -> reload());
        }
        if (metricCombo != null) {
            metricCombo.setItems(FXCollections.observableArrayList(Metric.values()));
            metricCombo.getSelectionModel().select(Metric.WINS);
            metricCombo.setOnAction(e -> reload());
        }

        // Rank columns (with medals)
        configureRankColumn(pRankCol);
        configureRankColumn(tRankCol);

        // Players columns
        if (pNameCol != null) pNameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        if (pWinsCol != null) pWinsCol.setCellValueFactory(c -> c.getValue().winsProperty());
        if (pGamesCol != null) pGamesCol.setCellValueFactory(c -> c.getValue().gamesProperty());
        if (pWinRateCol != null) pWinRateCol.setCellValueFactory(c -> c.getValue().winRateProperty());
        if (pAvgScoreCol != null) pAvgScoreCol.setCellValueFactory(c -> c.getValue().avgScoreProperty());
        if (pAvgTimeCol != null) {
            pAvgTimeCol.setCellValueFactory(c ->
                    new javafx.beans.property.SimpleStringProperty(c.getValue().getAvgWinTimeText()));
        }

        // Teams columns (NEW)
        if (tP1NameCol != null) tP1NameCol.setCellValueFactory(c -> c.getValue().player1NameProperty());
        if (tP2NameCol != null) tP2NameCol.setCellValueFactory(c -> c.getValue().player2NameProperty());

        if (tWinsCol != null) tWinsCol.setCellValueFactory(c -> c.getValue().winsProperty());
        if (tGamesCol != null) tGamesCol.setCellValueFactory(c -> c.getValue().gamesProperty());
        if (tWinRateCol != null) tWinRateCol.setCellValueFactory(c -> c.getValue().winRateProperty());
        if (tAvgScoreCol != null) tAvgScoreCol.setCellValueFactory(c -> c.getValue().avgScoreProperty());
        if (tAvgTimeCol != null) {
            tAvgTimeCol.setCellValueFactory(c ->
                    new javafx.beans.property.SimpleStringProperty(c.getValue().getAvgWinTimeText()));
        }

        // Avatar columns
        configureAvatarColumn(pAvatarCol, LeaderboardRow::getAvatar1Id);
        configureAvatarColumn(tAvatar1Col, LeaderboardRow::getAvatar1Id);
        configureAvatarColumn(tAvatar2Col, LeaderboardRow::getAvatar2Id);

        if (playersTable != null) playersTable.setItems(playerRows);
        if (teamsTable != null) teamsTable.setItems(teamRows);

        reload();
    }

    private interface AvatarIdGetter {
        String get(LeaderboardRow row);
    }

    private void configureRankColumn(TableColumn<LeaderboardRow, Number> col) {
        if (col == null) return;

        col.setCellValueFactory(c -> c.getValue().rankProperty());

        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Number rank, boolean empty) {
                super.updateItem(rank, empty);

                if (empty || rank == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                int r = rank.intValue();
                switch (r) {
                    case 1 -> { setText(null); setGraphic(new Label("🥇")); }
                    case 2 -> { setText(null); setGraphic(new Label("🥈")); }
                    case 3 -> { setText(null); setGraphic(new Label("🥉")); }
                    default -> { setGraphic(null); setText(String.valueOf(r)); }
                }
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });
    }

    private void configureAvatarColumn(TableColumn<LeaderboardRow, String> col, AvatarIdGetter getter) {
        if (col == null) return;

        col.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(getter.get(cd.getValue())));
        col.setCellFactory(tc -> new TableCell<>() {
            private final ImageView iv = new ImageView();

            {
                // change these numbers to resize avatars
                iv.setFitWidth(40);
                iv.setFitHeight(40);
                iv.setPreserveRatio(true);
                setAlignment(javafx.geometry.Pos.CENTER);
            }

            @Override
            protected void updateItem(String avatarId, boolean empty) {
                super.updateItem(avatarId, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                Image img = util.AvatarManager.resolveAvatar(avatarId);
                if (img == null) {
                    var stream = getClass().getResourceAsStream("/Images/S5.png");
                    img = (stream == null) ? null : new Image(stream);
                }

                iv.setImage(img);
                setGraphic(iv);
            }
        });
    }

    // --------------------------------- Actions ---------------------------------

    @FXML
    private void onRefresh() {
        reload();
    }

    @FXML
    private void onBack() {
        Stage stage = (Stage) root.getScene().getWindow();
        util.ViewNavigator.switchTo(stage, "/view/main_view.fxml");
    }

    // --------------------------------- Reload pipeline ---------------------------------

    private void reload() {
        TimeWindow window = (timeWindowCombo != null && timeWindowCombo.getValue() != null)
                ? timeWindowCombo.getValue()
                : TimeWindow.ALL_TIME;

        Metric metric = (metricCombo != null && metricCombo.getValue() != null)
                ? metricCombo.getValue()
                : Metric.WINS;

        Map<String, Player> regByName = buildRegisteredPlayersMap();

        List<Game> games = registeredVsRegisteredGames(regByName);
        games = applyWindow(games, window);

        playerRows.setAll(buildPlayerRows(games, metric, regByName));
        teamRows.setAll(buildTeamRows(games, metric, regByName));

        if (playersTable != null) playersTable.refresh();
        if (teamsTable != null) teamsTable.refresh();
    }

    private Map<String, Player> buildRegisteredPlayersMap() {
        SysData sys = SysData.getInstance();
        sys.ensurePlayersLoaded();

        Map<String, Player> map = new HashMap<>();
        for (Game g : sys.getHistory().getGames()) {
            if (g == null) continue;

            addIfRegistered(map, g.getPlayer1OfficialName());
            addIfRegistered(map, g.getPlayer2OfficialName());
        }
        return map;
    }

    private void addIfRegistered(Map<String, Player> map, String officialName) {
        if (officialName == null || officialName.isBlank()) return;
        String key = officialName.trim().toLowerCase();
        if (map.containsKey(key)) return;

        Player p = SysData.getInstance().findPlayerByOfficialName(officialName.trim());
        if (p != null) map.put(key, p);
    }

    private List<Game> registeredVsRegisteredGames(Map<String, Player> regByName) {
        SysData sys = SysData.getInstance();
        sys.ensureHistoryLoaded();

        List<Game> out = new ArrayList<>();
        for (Game g : sys.getHistory().getGames()) {
            if (g == null) continue;

            String o1 = g.getPlayer1OfficialName();
            String o2 = g.getPlayer2OfficialName();
            if (o1 == null || o1.isBlank() || o2 == null || o2.isBlank()) continue;

            if (!regByName.containsKey(o1.trim().toLowerCase())) continue;
            if (!regByName.containsKey(o2.trim().toLowerCase())) continue;

            out.add(g);
        }
        return out;
    }

    private List<Game> applyWindow(List<Game> games, TimeWindow window) {
        if (window == TimeWindow.ALL_TIME) return games;

        LocalDate today = LocalDate.now();
        LocalDate start = switch (window) {
            case LAST_7_DAYS -> today.minusDays(7);
            case LAST_30_DAYS -> today.minusDays(30);
            case THIS_MONTH -> today.withDayOfMonth(1);
            default -> LocalDate.MIN;
        };

        return games.stream()
                .filter(g -> g.getDate() == null || !g.getDate().isBefore(start))
                .collect(Collectors.toList());
    }

    private List<LeaderboardRow> buildPlayerRows(List<Game> games, Metric metric, Map<String, Player> regByName) {
        Map<String, Stats> stats = new HashMap<>();

        for (Game g : games) {
            String o1 = g.getPlayer1OfficialName().trim();
            String o2 = g.getPlayer2OfficialName().trim();

            Stats s1 = stats.computeIfAbsent(o1.toLowerCase(), kk -> new Stats());
            Stats s2 = stats.computeIfAbsent(o2.toLowerCase(), kk -> new Stats());

            int score = g.getFinalScore();
            int durSec = g.getDurationSeconds();

            s1.addGame(score);
            s2.addGame(score);

            if (g.getResult() == GameResult.WIN) {
                s1.addWin(durSec);
                s2.addWin(durSec);
            }
        }

        List<Map.Entry<String, Stats>> sorted = sort(stats, metric);

        List<LeaderboardRow> rows = new ArrayList<>();
        int rank = 1;
        for (var e : sorted) {
            String key = e.getKey();
            Stats s = e.getValue();

            Player p = regByName.get(key);
            String displayName = (p != null) ? p.getOfficialName() : key;

            LeaderboardRow r = new LeaderboardRow(LeaderboardRow.Type.PLAYER, rank++, displayName);
            r.setGames(s.games);
            r.setWins(s.wins);
            r.setWinRate(s.winRate());
            r.setAvgScore(s.avgScore());
            r.setAvgWinTimeSeconds(s.avgWinTimeSeconds());
            r.setAvatar1Id((p != null) ? p.getAvatarId() : null);

            rows.add(r);
        }
        return rows;
    }

    private List<LeaderboardRow> buildTeamRows(List<Game> games, Metric metric, Map<String, Player> regByName) {
        Map<String, Stats> stats = new HashMap<>();

        for (Game g : games) {
            String o1 = g.getPlayer1OfficialName().trim();
            String o2 = g.getPlayer2OfficialName().trim();

            String key = teamKey(o1, o2);
            Stats s = stats.computeIfAbsent(key, kk -> new Stats());

            s.addGame(g.getFinalScore());
            if (g.getResult() == GameResult.WIN) {
                s.addWin(g.getDurationSeconds());
            }
        }

        List<Map.Entry<String, Stats>> sorted = sort(stats, metric);

        List<LeaderboardRow> rows = new ArrayList<>();
        int rank = 1;

        for (var e : sorted) {
            String key = e.getKey();
            Stats s = e.getValue();

            String[] parts = key.split("\\|\\|");
            String p1Key = parts[0];
            String p2Key = parts[1];

            Player p1 = regByName.get(p1Key);
            Player p2 = regByName.get(p2Key);

            String name1 = (p1 != null) ? p1.getOfficialName() : p1Key;
            String name2 = (p2 != null) ? p2.getOfficialName() : p2Key;

            LeaderboardRow r = new LeaderboardRow(LeaderboardRow.Type.TEAM, rank++, "");
            r.setPlayer1Name(name1);
            r.setPlayer2Name(name2);

            r.setGames(s.games);
            r.setWins(s.wins);
            r.setWinRate(s.winRate());
            r.setAvgScore(s.avgScore());
            r.setAvgWinTimeSeconds(s.avgWinTimeSeconds());

            r.setAvatar1Id((p1 != null) ? p1.getAvatarId() : null);
            r.setAvatar2Id((p2 != null) ? p2.getAvatarId() : null);

            rows.add(r);
        }

        return rows;
    }

    private static String teamKey(String a, String b) {
        String x = a.trim().toLowerCase();
        String y = b.trim().toLowerCase();
        return (x.compareTo(y) <= 0) ? x + "||" + y : y + "||" + x;
    }

    private static List<Map.Entry<String, Stats>> sort(Map<String, Stats> map, Metric metric) {
        Comparator<Map.Entry<String, Stats>> cmp = switch (metric) {
            case WINS -> Comparator.<Map.Entry<String, Stats>>comparingInt(e -> e.getValue().wins).reversed();
            case WIN_RATE -> Comparator.<Map.Entry<String, Stats>>comparingDouble(e -> e.getValue().winRate()).reversed();
            case AVG_SCORE -> Comparator.<Map.Entry<String, Stats>>comparingDouble(e -> e.getValue().avgScore()).reversed();
            case AVG_WIN_TIME -> Comparator.<Map.Entry<String, Stats>>comparingDouble(e -> {
                double t = e.getValue().avgWinTimeSeconds();
                return (t <= 0) ? Double.POSITIVE_INFINITY : t;
            });
        };

        cmp = cmp.thenComparing(e -> e.getValue().wins, Comparator.reverseOrder())
                 .thenComparing(e -> e.getValue().games, Comparator.reverseOrder())
                 .thenComparing(Map.Entry::getKey);

        return map.entrySet().stream().sorted(cmp).collect(Collectors.toList());
    }
}
