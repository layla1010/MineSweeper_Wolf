package model;

/**
* Represents a question cell.
* When a player reveals this cell, the game engine should present
* a question according to the difficulty rules and then update
* the score / lives based on the answer.
*/

public class QuestionCell extends Cell {

    public QuestionCell(int row, int col) {
        super(row, col);
    }

    @Override
    public CellType getType() {
        return CellType.QUESTION;
    }
}

