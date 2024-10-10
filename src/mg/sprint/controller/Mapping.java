package mg.sprint.controller;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class Mapping {
    private Class<?> controller;
    private ArrayList<VerbAction> verbActions;
    public ArrayList<VerbAction> getVerbActions() {
        return verbActions;
    }

    public void setVerbActions(ArrayList<VerbAction> verbActions) {
        this.verbActions = verbActions;
    }


    public Class<?> getController() {
        return controller;
    }
    public void setController(Class<?> controller) {
        this.controller = controller;
    }

    public Mapping(Class<?> controller, ArrayList<VerbAction> verbActions) {
        this.controller = controller;
        this.verbActions = verbActions;
    }

}