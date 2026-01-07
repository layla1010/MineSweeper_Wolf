package model;


//Represents a question cell. When a player reveals this cell, the game engine should present a question 

public class QuestionCell extends Cell {

    public QuestionCell(int row, int col) {
        super(row, col);
    }

    @Override
    public CellType getType() {
        return CellType.QUESTION;
    }

    @Override
    protected void onFirstReveal(CellRevealResult r) {
        r.addScore = true;
    }

    @Override
    protected void onAlwaysReveal(CellRevealResult r) {}
}

