package mg.sprint.controller;

import java.lang.reflect.Method;

public class Mapping {
    private final Class<?> controller;
    private final Method method;
    private final String verb;

    public Mapping(Class<?> controller, Method method, String verb) {
        this.controller = controller;
        this.method = method;
        this.verb = verb; 
    }

    public Class<?> getController() {
        return controller;
    }

    public Method getMethod() {
        return method;
    }

    public String getVerb() {
        return verb; 
    }
}
