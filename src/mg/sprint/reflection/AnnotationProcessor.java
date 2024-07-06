package mg.sprint.reflection;

import mg.sprint.annotation.RequestObject;
import mg.sprint.annotation.RequestSubParameter;
import mg.sprint.session.MySession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

// Classe pour traiter les annotations
public class AnnotationProcessor {

    // Méthode pour traiter les paramètres d'un objet annoté avec @RequestObject
    public static void processRequestObject(Object obj, HttpServletRequest request) throws Exception {
        Class<?> clazz = obj.getClass();

        // Parcourir les champs de la classe
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(RequestSubParameter.class)) {
                RequestSubParameter paramAnnotation = field.getAnnotation(RequestSubParameter.class);
                String paramName = paramAnnotation.value();
                try {
                    // Rendre le champ accessible pour la modification
                    field.setAccessible(true);
                    // Récupérer la valeur du paramètre à partir de la requête HTTP
                    String parameterValue = request.getParameter(paramName);
                    if (parameterValue != null) {
                        // Convertir la valeur du paramètre en type approprié
                        Object convertedValue = convertValue(field.getType(), parameterValue);
                        // Affecter la valeur convertie au champ de l'objet
                        field.set(obj, convertedValue);
                    }
                    // Révoquer l'accessibilité du champ
                    field.setAccessible(false);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else {
                // Si le champ n'est pas annoté, lancer une exception
                throw new Exception("L'attribut " + field.getName() + " de la classe " + clazz.getName()
                        + " doit avoir l'annotation @RequestSubParameter.");
            }
        }
    }

    public static Object execute(Object obj, HttpServletRequest request) throws Exception {
        processRequestObject(obj, request);
        return obj;
    }

    // Méthode pour convertir les valeurs des paramètres en type approprié
    public static Object convertValue(Class<?> type, String value) {
        if (type == String.class) {
            return value;
        } else if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        return null;
    }

    public static void sessionProcess(Object controllerInstance, HttpServletRequest request) {
        List<Field> fields = Arrays.asList(controllerInstance.getClass().getDeclaredFields());
        for (Field field : fields) {
            if (field.getType() == MySession.class) {
                HttpSession httpSession = request.getSession();
                try {
                    field.setAccessible(true);
                    field.set(controllerInstance, new MySession(httpSession));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        Method[] methods = controllerInstance.getClass().getDeclaredMethods();
        for (Method method : methods) {
            Parameter[] parameters = method.getParameters();
            for (Parameter parameter : parameters) {
                if (parameter.getType() == MySession.class) {
                    HttpSession httpSession = request.getSession();
                    try {
                        method.setAccessible(true);
                        Object[] args = new Object[parameters.length];
                        for (int i = 0; i < parameters.length; i++) {
                            if (parameters[i].getType() == MySession.class) {
                                args[i] = new MySession(httpSession);
                            } else {
                                args[i] = null; // Vous devrez ici ajuster pour gérer d'autres types
                            }
                        }
                        method.invoke(controllerInstance, args);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}