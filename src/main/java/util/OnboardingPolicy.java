package util;

public enum OnboardingPolicy {
    ALWAYS,        // auto-run every time (guest)
    ONCE_THEN_HOVER, // run once, then only hover hints (registered)
    NEVER          // admin
}
