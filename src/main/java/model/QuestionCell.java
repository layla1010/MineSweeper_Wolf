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
}

