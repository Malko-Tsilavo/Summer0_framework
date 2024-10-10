package mg.sprint.controller;

import java.lang.reflect.Method;

public class VerbAction {
    private Method method;
    private String verb;
    public Method getMethod() {
        return method;
    }
    public String getVerb() {
        return verb;
    }
    public void setMethod(Method method) {
        this.method = method;
    }
    public void setVerb(String verb) {
        this.verb = verb;
    }
    
    
}
