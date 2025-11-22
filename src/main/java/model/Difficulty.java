package model;

public enum Difficulty {
	EASY, MEDIUM, HARD;

    public int getRows() {
        return switch (this) {
            case EASY -> 9;
            case MEDIUM -> 13;
            case HARD -> 16;
        };
    }

    public int getCols() {
        return getRows(); // square boards
    }

    public int getMineCount() {
        return switch (this) {
            case EASY -> 10;
            case MEDIUM -> 26;
            case HARD -> 44;
        };
    }

    public int getQuestionCount() {
        return switch (this) {
            case EASY -> 6;
            case MEDIUM -> 7;
            case HARD -> 11;
        };
    }

    public int getSurpriseCount() {
        return switch (this) {
            case EASY -> 2;
            case MEDIUM -> 3;
            case HARD -> 4;
        };
    }

    //max hearts is always 10 and this returns the number of FULL hearts at start
    public int getStartingHearts() {
        return switch (this) {
            case EASY -> 10;
            case MEDIUM -> 8;
            case HARD -> 6;
        };
    }

}
