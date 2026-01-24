package control;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import model.Game;
import model.GameResult;
import model.Player;
import model.SysData;
import util.OnboardingManager;
import util.OnboardingPolicy;
import util.OnboardingStep;

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

    // Players table (simplified)
    @FXML private TableView<LeaderboardRow> playersTable;
    @FXML private TableColumn<LeaderboardRow, Number> pRankCol;
    @FXML private TableColumn<LeaderboardRow, String> pAvatarCol;
    @FXML private TableColumn<LeaderboardRow, String> pNameCol;
    @FXML private TableColumn<LeaderboardRow, Number> pGamesCol;
    @FXML private TableColumn<LeaderboardRow, String> pMetricCol;

    // Teams table (simplified)
    @FXML private TableView<LeaderboardRow> teamsTable;
    @FXML private TableColumn<LeaderboardRow, Number> tRankCol;
    @FXML private TableColumn<LeaderboardRow, String> tAvatar1Col;
    @FXML private TableColumn<LeaderboardRow, String> tP1NameCol;
    @FXML private TableColumn<LeaderboardRow, String> tAvatar2Col;
    @FXML private TableColumn<LeaderboardRow, String> tP2NameCol;
    @FXML private TableColumn<LeaderboardRow, Number> tGamesCol;
    @FXML private TableColumn<LeaderboardRow, String> tMetricCol;

    // Players podium
    @FXML private ImageView p1Avatar;
    @FXML private ImageView p2Avatar;
    @FXML private ImageView p3Avatar;
    @FXML private Label p1Name;
    @FXML private Label p2Name;
    @FXML private Label p3Name;
    @FXML private Label p1Stat;
    @FXML private Label p2Stat;
    @FXML private Label p3Stat;

    // Teams podium
    @FXML private ImageView t1Avatar1;
    @FXML private ImageView t1Avatar2;
    @FXML private ImageView t2Avatar1;
    @FXML private ImageView t2Avatar2;
    @FXML private ImageView t3Avatar1;
    @FXML private ImageView t3Avatar2;
    @FXML private Label t1Name;
    @FXML private Label t2Name;
    @FXML private Label t3Name;
    @FXML private Label t1Stat;
    @FXML private Label t2Stat;
    @FXML private Label t3Stat;

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

    // ------------------------------- Init -------------------------------

    @FXML
    private void initialize() {
    	
    	 // Guided onboarding (login must ALWAYS show because user not known yet)
        List<OnboardingStep> LeaderBoardSteps = List.of(
                new OnboardingStep("#timeWindowCombo", "Time Window Filter",
                        "Select the time range used to calculate rankings (e.g., recent games vs. all-time). The podium and tables update based on this filter."),
                new OnboardingStep("#metricCombo", "Sort By",
                        "Choose the metric that determines ranking (for example: wins, win rate, games played). All results—including the podium and tables—are sorted using this metric."),
                new OnboardingStep("#refreshBtn", "Refresh",
                        "Click Refresh to apply the selected filters and reload the leaderboard results immediately."),
                new OnboardingStep("#tabs", "Player & Team Rankings",
                        "Switch between Player and Team rankings. Both tabs reflect the selected time window and sorting metric.")
        );

        OnboardingManager.runWithPolicy(
                "onboarding.competitve_insights",
                root,
                LeaderBoardSteps,
                OnboardingPolicy.ALWAYS,
                null
        );
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

        // Rank columns with medals
        configureRankColumn(pRankCol);
        configureRankColumn(tRankCol);

        // Players fixed columns
        if (pNameCol != null)  pNameCol.setCellValueFactory(c -> c.getValue().nameProperty());
        if (pGamesCol != null) pGamesCol.setCellValueFactory(c -> c.getValue().gamesProperty());
        configureAvatarColumn(pAvatarCol, LeaderboardRow::getAvatar1Id);

        // Teams fixed columns
        if (tP1NameCol != null) tP1NameCol.setCellValueFactory(c -> c.getValue().player1NameProperty());
        if (tP2NameCol != null) tP2NameCol.setCellValueFactory(c -> c.getValue().player2NameProperty());
        if (tGamesCol != null)  tGamesCol.setCellValueFactory(c -> c.getValue().gamesProperty());

        configureAvatarColumn(tAvatar1Col, LeaderboardRow::getAvatar1Id);
        configureAvatarColumn(tAvatar2Col, LeaderboardRow::getAvatar2Id);

        // Dynamic metric columns (value computed from current metric selection)
        configurePlayersMetricColumn();
        configureTeamsMetricColumn();

        if (playersTable != null) playersTable.setItems(playerRows);
        if (teamsTable != null)   teamsTable.setItems(teamRows);

     // Podium players
        applyPodiumAvatarClips(p1Avatar, 70);
        applyPodiumAvatarClips(p2Avatar, 56);
        applyPodiumAvatarClips(p3Avatar, 56);

        // Podium teams
        applyPodiumAvatarClips(t1Avatar1, 56);
        applyPodiumAvatarClips(t1Avatar2, 56);
        applyPodiumAvatarClips(t2Avatar1, 48);
        applyPodiumAvatarClips(t2Avatar2, 48);
        applyPodiumAvatarClips(t3Avatar1, 48);
        applyPodiumAvatarClips(t3Avatar2, 48);

        reload();
    }

    // ------------------------------- Column config helpers -------------------------------

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
                setAlignment(Pos.CENTER);
            }
        });
    }

    private void configureAvatarColumn(TableColumn<LeaderboardRow, String> col, AvatarIdGetter getter) {
        if (col == null) return;

        // ✅ THIS IS THE MISSING PIECE
        col.setCellValueFactory(cd ->
                new SimpleStringProperty(getter.get(cd.getValue()))
        );

        col.setCellFactory(tc -> new TableCell<>() {
            private final ImageView iv = new ImageView();

            {
                makeCircular(iv, 40);
                setAlignment(Pos.CENTER);
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


    private void configurePlayersMetricColumn() {
        if (pMetricCol == null) return;

        pMetricCol.setCellValueFactory(cd -> {
            LeaderboardRow r = cd.getValue();
            Metric m = (metricCombo != null && metricCombo.getValue() != null) ? metricCombo.getValue() : Metric.WINS;
            return new SimpleStringProperty(formatMetricValue(r, m));
        });

        pMetricCol.setStyle("-fx-alignment: CENTER;");
    }

    private void configureTeamsMetricColumn() {
        if (tMetricCol == null) return;

        tMetricCol.setCellValueFactory(cd -> {
            LeaderboardRow r = cd.getValue();
            Metric m = (metricCombo != null && metricCombo.getValue() != null) ? metricCombo.getValue() : Metric.WINS;
            return new SimpleStringProperty(formatMetricValue(r, m));
        });

        tMetricCol.setStyle("-fx-alignment: CENTER;");
    }

    private static String metricHeader(Metric m) {
        return switch (m) {
            case WINS -> "Wins";
            case WIN_RATE -> "Win Rate";
            case AVG_SCORE -> "Avg Score";
            case AVG_WIN_TIME -> "Avg Time";
        };
    }

    private static String formatMetricValue(LeaderboardRow r, Metric m) {
        return switch (m) {
            case WINS -> String.valueOf(r.getWins());
            case WIN_RATE -> String.format(Locale.US, "%.1f%%", r.getWinRate());
            case AVG_SCORE -> String.format(Locale.US, "%.1f", r.getAvgScore());
            case AVG_WIN_TIME -> r.getAvgWinTimeText();
        };
    }

    private static String podiumStat(LeaderboardRow r, Metric m) {
        String metricText = switch (m) {
            case WINS -> "Wins: " + r.getWins();
            case WIN_RATE -> String.format(Locale.US, "Win Rate: %.1f%%", r.getWinRate());
            case AVG_SCORE -> String.format(Locale.US, "Avg Score: %.1f", r.getAvgScore());
            case AVG_WIN_TIME -> "Avg Time: " + r.getAvgWinTimeText();
        };
        return metricText + "   |   Games: " + r.getGames();
    }

    // ------------------------------- Actions -------------------------------

    @FXML
    private void onRefresh() {
        reload();
    }

    @FXML
    private void onBack() {
        Stage stage = (Stage) root.getScene().getWindow();
        util.ViewNavigator.switchTo(stage, "/view/main_view.fxml");
    }

    // ------------------------------- Reload pipeline -------------------------------

    private void reload() {
        TimeWindow window = (timeWindowCombo != null && timeWindowCombo.getValue() != null)
                ? timeWindowCombo.getValue()
                : TimeWindow.ALL_TIME;

        Metric metric = (metricCombo != null && metricCombo.getValue() != null)
                ? metricCombo.getValue()
                : Metric.WINS;

        // update headers for the dynamic metric columns
        if (pMetricCol != null) pMetricCol.setText(metricHeader(metric));
        if (tMetricCol != null) tMetricCol.setText(metricHeader(metric));

        Map<String, Player> regByName = buildRegisteredPlayersMap();

        List<Game> games = registeredVsRegisteredGames(regByName);
        games = applyWindow(games, window);

        List<LeaderboardRow> allPlayers = buildPlayerRows(games, metric, regByName);
        List<LeaderboardRow> allTeams   = buildTeamRows(games, metric, regByName);

        updatePlayersPodium(allPlayers, metric);
        updateTeamsPodium(allTeams, metric);

        // remove top 3 rows from tables (because podium shows them)
        playerRows.setAll(allPlayers.stream().skip(3).toList());
        teamRows.setAll(allTeams.stream().skip(3).toList());

        if (playersTable != null) playersTable.refresh();
        if (teamsTable != null)   teamsTable.refresh();
    }

    private void updatePlayersPodium(List<LeaderboardRow> rows, Metric metric) {
        LeaderboardRow r1 = (rows.size() > 0) ? rows.get(0) : null;
        LeaderboardRow r2 = (rows.size() > 1) ? rows.get(1) : null;
        LeaderboardRow r3 = (rows.size() > 2) ? rows.get(2) : null;

        setPodiumSlotPlayer(r1, p1Avatar, p1Name, p1Stat, metric);
        setPodiumSlotPlayer(r2, p2Avatar, p2Name, p2Stat, metric);
        setPodiumSlotPlayer(r3, p3Avatar, p3Name, p3Stat, metric);
    }

    private void setPodiumSlotPlayer(LeaderboardRow r,
                                     ImageView avatar,
                                     Label name,
                                     Label stat,
                                     Metric metric) {
        if (avatar == null || name == null || stat == null) return;

        if (r == null) {
            avatar.setImage(null);
            name.setText("—");
            stat.setText("—");
            return;
        }

        avatar.setImage(resolveAvatarImage(r.getAvatar1Id()));
        name.setText(safeText(r.getName()));
        stat.setText(podiumStat(r, metric));
    }

    private void updateTeamsPodium(List<LeaderboardRow> rows, Metric metric) {
        LeaderboardRow r1 = (rows.size() > 0) ? rows.get(0) : null;
        LeaderboardRow r2 = (rows.size() > 1) ? rows.get(1) : null;
        LeaderboardRow r3 = (rows.size() > 2) ? rows.get(2) : null;

        setPodiumSlotTeam(r1, t1Avatar1, t1Avatar2, t1Name, t1Stat, metric);
        setPodiumSlotTeam(r2, t2Avatar1, t2Avatar2, t2Name, t2Stat, metric);
        setPodiumSlotTeam(r3, t3Avatar1, t3Avatar2, t3Name, t3Stat, metric);
    }

    private void setPodiumSlotTeam(LeaderboardRow r,
                                   ImageView a1, ImageView a2,
                                   Label name,
                                   Label stat,
                                   Metric metric) {
        if (a1 == null || a2 == null || name == null || stat == null) return;

        if (r == null) {
            a1.setImage(null);
            a2.setImage(null);
            name.setText("—");
            stat.setText("—");
            return;
        }

        a1.setImage(resolveAvatarImage(r.getAvatar1Id()));
        a2.setImage(resolveAvatarImage(r.getAvatar2Id()));

        String teamName = safeText(r.getPlayer1Name()) + " + " + safeText(r.getPlayer2Name());
        name.setText(teamName);

        stat.setText(podiumStat(r, metric));
    }

    
    private static void makeCircular(ImageView iv, double size) {
        if (iv == null) return;

        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);

        // Clip the image pixels to a circle
        Circle clip = new Circle(size / 2.0, size / 2.0, size / 2.0);
        iv.setClip(clip);

        // If size might change later, keep clip synced (safe)
        iv.layoutBoundsProperty().addListener((obs, oldB, b) -> {
            double w = b.getWidth();
            double h = b.getHeight();
            double r = Math.min(w, h) / 2.0;
            clip.setCenterX(w / 2.0);
            clip.setCenterY(h / 2.0);
            clip.setRadius(r);
        });
    }

    private static void applyPodiumAvatarClips(ImageView iv, double size) {
        makeCircular(iv, size);
    }
    
    private Image resolveAvatarImage(String avatarId) {
        Image img = util.AvatarManager.resolveAvatar(avatarId);
        if (img != null) return img;

        var stream = getClass().getResourceAsStream("/Images/S5.png");
        return (stream == null) ? null : new Image(stream);
    }

    private static String safeText(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    // ------------------------------- Data extraction -------------------------------

    private Map<String, Player> buildRegisteredPlayersMap() {
        SysData sys = SysData.getInstance();
        sys.ensurePlayersLoaded();

        Map<String, Player> map = new HashMap<>();
        for (Player p : sys.getAllPlayers()) { 
            if (p == null) continue;
            String name = p.getOfficialName();
            if (name == null || name.isBlank()) continue;

            map.put(name.trim().toLowerCase(), p);
        }
        return map;
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

            if (!regByName.containsKey(o1.trim().toLowerCase(Locale.ROOT))) continue;
            if (!regByName.containsKey(o2.trim().toLowerCase(Locale.ROOT))) continue;

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
        Map<String, String> canonicalName = new HashMap<>(); // <-- remember original casing

        for (Game g : games) {
            String o1 = g.getPlayer1OfficialName().trim();
            String o2 = g.getPlayer2OfficialName().trim();

            String k1 = o1.toLowerCase();
            String k2 = o2.toLowerCase();

            canonicalName.putIfAbsent(k1, o1);
            canonicalName.putIfAbsent(k2, o2);

            Stats s1 = stats.computeIfAbsent(k1, kk -> new Stats());
            Stats s2 = stats.computeIfAbsent(k2, kk -> new Stats());

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

            // prefer Player official name, otherwise use canonical from history (not lowercase key)
            String displayName = (p != null)
                    ? p.getOfficialName()
                    : canonicalName.getOrDefault(key, key);

            LeaderboardRow r = new LeaderboardRow(LeaderboardRow.Type.PLAYER, rank++, displayName);
            r.setGames(s.games);
            r.setWins(s.wins);
            r.setWinRate(s.winRate());
            r.setAvgScore(s.avgScore());
            r.setAvgWinTimeSeconds(s.avgWinTimeSeconds());

            // avatar: if Player was found -> use it; else keep null (will show default)
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
        String x = a.trim().toLowerCase(Locale.ROOT);
        String y = b.trim().toLowerCase(Locale.ROOT);
        return (x.compareTo(y) <= 0) ? x + "||" + y : y + "||" + x;
    }

    private static List<Map.Entry<String, Stats>> sort(Map<String, Stats> map, Metric metric) {
        Comparator<Map.Entry<String, Stats>> cmp = switch (metric) {
            case WINS -> Comparator.<Map.Entry<String, Stats>>comparingInt(e -> e.getValue().wins).reversed();
            case WIN_RATE -> Comparator.<Map.Entry<String, Stats>>comparingDouble(e -> e.getValue().winRate()).reversed();
            case AVG_SCORE -> Comparator.<Map.Entry<String, Stats>>comparingDouble(e -> e.getValue().avgScore()).reversed();
            case AVG_WIN_TIME -> Comparator.<Map.Entry<String, Stats>>comparingDouble(e -> {
                double t = e.getValue().avgWinTimeSeconds();
                return (t <= 0) ? Double.POSITIVE_INFINITY : t; // smaller is better; no wins => bottom
            });
        };

        cmp = cmp.thenComparing(e -> e.getValue().wins, Comparator.reverseOrder())
                .thenComparing(e -> e.getValue().games, Comparator.reverseOrder())
                .thenComparing(Map.Entry::getKey);

        return map.entrySet().stream().sorted(cmp).collect(Collectors.toList());
    }
}
