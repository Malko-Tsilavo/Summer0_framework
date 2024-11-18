package mg.sprint.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Maximum {
    int value();  // Définit la valeur maximum
}

