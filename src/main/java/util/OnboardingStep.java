package util;

public class OnboardingStep {
    private final String targetSelector; // e.g. "#playersLoginButton"
    private final String title;
    private final String text;

    public OnboardingStep(String targetSelector, String title, String text) {
        this.targetSelector = targetSelector;
        this.title = title;
        this.text = text;
    }

    public String getTargetSelector() { return targetSelector; }
    public String getTitle() { return title; }
    public String getText() { return text; }
}
