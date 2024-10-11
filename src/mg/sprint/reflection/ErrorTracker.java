package mg.sprint.reflection;

import java.util.ArrayList;
import java.util.List;

public class ErrorTracker {
    private static List<ErrorDetails> errors = new ArrayList<>();

    public static void addError(int statusCode, String message) {
        errors.add(new ErrorDetails(statusCode, message));
    }

    public static List<ErrorDetails> getErrors() {
        return errors;
    }

    public static boolean hasErrors() {
        return !errors.isEmpty();
    }

    public static void clearErrors() {
        errors.clear();  // Pour effacer les erreurs une fois traitées
    }
}