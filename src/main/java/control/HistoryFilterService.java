package control;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


 //Pure filtering/sorting/validation logic for History.
public final class HistoryFilterService {

    public static final String OPT_ALL = "All";
    public static final String OPT_DATE = "Date";
    public static final String OPT_PLAYER_NAME = "Player name";
    public static final String OPT_DIFFICULTY = "Difficulty";
    public static final String OPT_RESULT = "Result";

    public static final String SORT_NONE = "None";
    public static final String SORT_SCORE_HIGH_TO_LOW = "Score (high → low)";
    public static final String SORT_SCORE_LOW_TO_HIGH = "Score (low → high)";
    public static final String SORT_DATE_NEWEST = "Date (newest)";
    public static final String SORT_DATE_OLDEST = "Date (oldest)";
    public static final String SORT_DURATION_SHORT_TO_LONG = "Duration (short → long)";
    public static final String SORT_DURATION_LONG_TO_SHORT = "Duration (long → short)";

    private final DateTimeFormatter csvDateFormatter;

    public HistoryFilterService(DateTimeFormatter csvDateFormatter) {
        this.csvDateFormatter = Objects.requireNonNull(csvDateFormatter, "csvDateFormatter");
    }

    //Decides what filter type to use *for this action* without changing UI selection.
    
    public String resolveEffectiveFilterType(String selectedType, String typedText) {
        String selected = safeTrim(selectedType);
        if (selected.isEmpty()) selected = OPT_ALL;

        if (!OPT_ALL.equals(selected)) {
            return selected;
        }

        String typed = safeTrim(typedText);
        if (typed.isEmpty()) {
            return OPT_ALL;
        }

        if (isResultWord(typed)) return OPT_RESULT;
        if (isDifficultyWord(typed)) return OPT_DIFFICULTY;
        return OPT_PLAYER_NAME;
    }

   
        public static final class ValidationResult {
        public final boolean ok;
        public final String title;
        public final String message;

        private ValidationResult(boolean ok, String title, String message) {
            this.ok = ok;
            this.title = title;
            this.message = message;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult error(String title, String message) {
            return new ValidationResult(false, title, message);
        }
    }

    //Validates user intent given effective filter type and inputs, Result filter is STRICT (only known keywords allowed).
    public ValidationResult validate(String effectiveType, String typedText, LocalDate selectedDate) {
        String type = safeTrim(effectiveType);
        if (type.isEmpty() || OPT_ALL.equals(type)) {
            return ValidationResult.ok();
        }

        if (OPT_DATE.equals(type)) {
            if (selectedDate == null) {
                return ValidationResult.error(
                        "Missing date",
                        "You chose to filter by date but didn’t select any date.\nPlease pick a date from the calendar."
                );
            }
            if (selectedDate.isAfter(LocalDate.now())) {
                return ValidationResult.error(
                        "Invalid date",
                        "You selected a day that hasn’t come yet.\nPlease choose a date that already happened."
                );
            }
            return ValidationResult.ok();
        }

        String text = safeTrim(typedText);
        if (text.isEmpty()) {
            return ValidationResult.error("Missing value", "Please enter a value in the text field for this filter.");
        }

        if (OPT_PLAYER_NAME.equals(type)) {
            if (text.length() < 2) {
                return ValidationResult.error("Name too short", "Please enter at least 2 characters when filtering by player name.");
            }
            if (isDifficultyWord(text)) {
                return ValidationResult.error(
                        "Filter mismatch",
                        "You typed a difficulty (\"" + text + "\").\nEither change the filter to \"Difficulty\" or keep typing a player name."
                );
            }
            if (isResultWord(text)) {
                return ValidationResult.error(
                        "Filter mismatch",
                        "You typed a result (\"" + text + "\").\nEither change the filter to \"Result\" or keep typing a player name."
                );
            }
            return ValidationResult.ok();
        }

        if (OPT_DIFFICULTY.equals(type)) {
            if (isResultWord(text)) {
                return ValidationResult.error(
                        "Filter mismatch",
                        "You typed a result (\"" + text + "\").\nUse \"Result\" filter for WIN / LOSE / GIVE UP."
                );
            }

            int diffWords = countDifficultyWords(text);
            if (diffWords > 1) {
                return ValidationResult.error(
                        "Too many difficulties",
                        "Please filter by one difficulty at a time.\nValid values are: Easy, Medium, Hard."
                );
            }

            if (canonicalDifficultyToken(text) == null) {
                return ValidationResult.error(
                        "Invalid difficulty",
                        "Unknown difficulty \"" + text + "\".\nValid values are: Easy, Medium, Hard."
                );
            }
            return ValidationResult.ok();
        }

        if (OPT_RESULT.equals(type)) {
            if (isDifficultyWord(text)) {
                return ValidationResult.error(
                        "Filter mismatch",
                        "You typed a difficulty (\"" + text + "\").\nUse \"Difficulty\" filter for Easy / Medium / Hard."
                );
            }

            if (canonicalResultToken(text) == null) {
                return ValidationResult.error(
                        "Invalid result keyword",
                        "You can only filter by Result using these keywords:\n" +
                                "win, won, w\n" +
                                "lose, lost, l, loss\n" +
                                "give, gave up, give up, giveup, gaveup, g"
                );
            }
            return ValidationResult.ok();
        }

        return ValidationResult.ok();
    }

    //Filters games based on effective type and provided inputs,Assumes validation has already passed; still defensive.
    public List<model.Game> filter(List<model.Game> source, String effectiveType, String typedText, LocalDate selectedDate) {
        if (source == null) return List.of();

        String type = safeTrim(effectiveType);
        if (type.isEmpty() || OPT_ALL.equals(type)) {
            return new ArrayList<>(source);
        }

        if (OPT_DATE.equals(type)) {
            if (selectedDate == null) return new ArrayList<>(source);

            List<model.Game> out = new ArrayList<>();
            for (model.Game g : source) {
                if (g == null) continue;
                Optional<LocalDate> gd = safeParseGameDate(g.getDateAsString());
                if (gd.isPresent() && gd.get().equals(selectedDate)) {
                    out.add(g);
                }
            }
            return out;
        }

        String qRaw = safeTrim(typedText);
        if (qRaw.isEmpty()) return new ArrayList<>(source);

        String qLower = qRaw.toLowerCase();
        List<model.Game> out = new ArrayList<>();

        for (model.Game g : source) {
            if (g == null) continue;

            switch (type) {
                case OPT_PLAYER_NAME -> {
                    String p1 = safeLower(g.getPlayer1Nickname());
                    String p2 = safeLower(g.getPlayer2Nickname());
                    if (p1.contains(qLower) || p2.contains(qLower)) out.add(g);
                }

                case OPT_DIFFICULTY -> {
                    String canon = canonicalDifficultyToken(qRaw);
                    if (canon == null) break;

                    String diff = safeLower(g.getDifficulty().name()); // easy/medium/hard
                    if (diff.equals(canon)) out.add(g);
                }

                case OPT_RESULT -> {
                    String canonQuery = canonicalResultToken(qRaw);
                    if (canonQuery == null) break;

                    String res = safeLower(g.getResult().name()); // e.g., GIVE_UP
                    String canonRes = canonicalResultToken(res);  // handles give_up via normalization

                    if (canonQuery.equals(canonRes)) out.add(g);
                }

                default -> out.add(g);
            }
        }

        return out;
    }

    //Sorts a list according to the selected sort option label.
    public List<model.Game> sort(List<model.Game> source, String sortLabel) {
        if (source == null) return List.of();

        String sort = safeTrim(sortLabel);
        if (sort.isEmpty() || SORT_NONE.equals(sort)) {
            return new ArrayList<>(source);
        }

        List<model.Game> list = new ArrayList<>(source);

        switch (sort) {
            case SORT_SCORE_HIGH_TO_LOW ->
                    list.sort(Comparator.comparingInt(model.Game::getFinalScore).reversed());

            case SORT_SCORE_LOW_TO_HIGH ->
                    list.sort(Comparator.comparingInt(model.Game::getFinalScore));

            case SORT_DATE_NEWEST ->
                    list.sort(Comparator.<model.Game, LocalDate>comparing(
                            g -> safeParseGameDate(g.getDateAsString()).orElse(LocalDate.MIN)
                    ).reversed());

            case SORT_DATE_OLDEST ->
                    list.sort(Comparator.<model.Game, LocalDate>comparing(
                            g -> safeParseGameDate(g.getDateAsString()).orElse(LocalDate.MIN)
                    ));

            case SORT_DURATION_SHORT_TO_LONG ->
                    list.sort(Comparator.comparingInt(model.Game::getDurationSeconds));

            case SORT_DURATION_LONG_TO_SHORT ->
                    list.sort(Comparator.comparingInt(model.Game::getDurationSeconds).reversed());

            default -> {
            }
        }

        return list;
    }

    
    private static String normalizeToken(String s) {
        if (s == null) return "";
        return s.toLowerCase().replace(" ", "").replace("_", "");
    }

    private static String safeTrim(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static String safeLower(String s) {
        return (s == null) ? "" : s.toLowerCase();
    }

    private static Optional<LocalDate> safeParseGameDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return Optional.empty();
        try {
            return Optional.of(LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    public boolean isDifficultyWord(String text) {
        String t = normalizeToken(text);
        return t.equals("easy")
                || t.equals("medium") || t.equals("med") || t.equals("meduim")
                || t.equals("hard");
    }

    private int countDifficultyWords(String text) {
        String t = normalizeToken(text);
        int count = 0;
        if (t.contains("easy")) count++;
        if (t.contains("medium") || t.contains("med") || t.contains("meduim")) count++;
        if (t.contains("hard")) count++;
        return count;
    }

    private String canonicalDifficultyToken(String text) {
        String t = normalizeToken(text);
        if (t.equals("easy")) return "easy";
        if (t.equals("hard")) return "hard";
        if (t.equals("medium") || t.equals("med") || t.equals("meduim")) return "medium";
        return null;
    }

    public boolean isResultWord(String text) {
        return canonicalResultToken(text) != null;
    }

    //Canonical result token mapping (win/lose/giveup), strict on known keywords.
    public String canonicalResultToken(String text) {
        String t = normalizeToken(text);
        if (t.isEmpty()) return null;

        if (t.equals("w") || t.equals("win") || t.equals("won")) return "win";
        if (t.equals("l") || t.equals("lose") || t.equals("lost") || t.equals("loss")) return "lose";
        if (t.equals("g") || t.equals("give") || t.equals("giveup") || t.equals("gaveup")) return "giveup";

        return null;
    }
}
