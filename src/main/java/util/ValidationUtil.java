package util;

public class ValidationUtil {

    private ValidationUtil() {} // prevent instantiation

    public static boolean allDistinctIgnoreCase(String... values) {
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                if (values[i].equalsIgnoreCase(values[j])) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

