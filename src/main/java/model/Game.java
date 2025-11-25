package model;

public class Game {
	
	private final GameConfig config;
    private final Board board1;
    private final Board board2;

    private int sharedHearts;
    private int score;
    private int minesLeft1;
    private int minesLeft2;
    private boolean isPlayer1Turn;
    private boolean paused;
    private long elapsedSeconds;
    
    public Game(GameConfig config) {
        this.config = config;
        this.board1 = new Board(config.getDifficulty());
        this.board2 = new Board(config.getDifficulty());

        this.sharedHearts   = config.getDifficulty().getInitialLives();
        this.score          = 0;
        this.minesLeft1     = board1.getMineCount();
        this.minesLeft2     = board2.getMineCount();
        this.isPlayer1Turn  = true;
        this.paused         = false;
        this.elapsedSeconds = 0;
    }
    
    
    public GameConfig getConfig() {
        return config;
    }

    public Difficulty getDifficulty() {
        return config.getDifficulty();
    }

    public Board getBoard1() {
        return board1;
    }

    public Board getBoard2() {
        return board2;
    }

    public int getSharedHearts() {
        return sharedHearts;
    }

    public int getScore() {
        return score;
    }

    public int getMinesLeft1() {
        return minesLeft1;
    }

    public int getMinesLeft2() {
        return minesLeft2;
    }

    public boolean isPlayer1Turn() {
        return isPlayer1Turn;
    }

    public boolean isPaused() {
        return paused;
    }

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    
    public void loseHeart() {
        if (sharedHearts > 0) {
            sharedHearts--;
        }
    }

    public void addScore(int delta) {
        score += delta;
        if (score < 0) score = 0;
    }

    public void decrementMinesLeftForPlayer1() {
        if (minesLeft1 > 0) {
            minesLeft1--;
        }
    }

    public void decrementMinesLeftForPlayer2() {
        if (minesLeft2 > 0) {
            minesLeft2--;
        }
    }

    public void switchTurn() {
        isPlayer1Turn = !isPlayer1Turn;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void incrementElapsedSeconds() {
        elapsedSeconds++;
    }

}
