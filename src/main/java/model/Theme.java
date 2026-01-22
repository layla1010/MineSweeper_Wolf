package model;

public enum Theme {
    COLORFUL("Colorful theme", "/css/theme.css"),
    WOLF("Wolf theme", "/css/wolf.css"),
	CYBER_BLUE("Cyber theme", "/css/Cyber.css");

    private final String displayName;
    private final String cssPath;

    Theme(String displayName, String cssPath) {
        this.displayName = displayName;
        this.cssPath = cssPath;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCssPath() {
        return cssPath;
    }
}
