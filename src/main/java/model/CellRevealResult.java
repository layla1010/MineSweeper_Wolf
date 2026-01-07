package model;

public class CellRevealResult {

    public final CellType type;

    public boolean loseHeart = false;
    public boolean triggerExplosion = false;
    public boolean addScore = false;

    public CellRevealResult(CellType type) {
        this.type = type;
    }
}
