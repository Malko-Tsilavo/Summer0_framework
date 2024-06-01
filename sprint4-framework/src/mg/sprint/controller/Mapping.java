package mg.sprint.controller;

import java.lang.reflect.Method;

public class Mapping {
    private Class<?> controller; // Classe du contrôleur
    private Method method; // Méthode du contrôleur

    public Mapping(Class<?> controller, Method method) {
        this.controller = controller;
        this.method = method;
    }

    // Getter et setter pour la classe du contrôleur
    public Class<?> getController() {
        return controller;
    }

    public void setController(Class<?> controller) {
        this.controller = controller;
    }

    // Getter et setter pour la méthode du contrôleur
    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }
}
