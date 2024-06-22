package mg.sprint.reflection;

import mg.sprint.annotation.RequestObject;
import mg.sprint.annotation.RequestSubParameter;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Field;

// Classe pour traiter les annotations
public class AnnotationProcessor {

    // Méthode pour traiter les objets annotés avec @RequestObject
    public static void processObjectParams(Object obj, HttpServletRequest request) {
        Class<?> clazz = obj.getClass();

        // Parcourir les champs de la classe
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(RequestSubParameter.class)) {
                // Récupérer l'annotation
                RequestSubParameter paramAnnotation = field.getAnnotation(RequestSubParameter.class);
                // Récupérer le nom du paramètre à partir de l'annotation
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
            }
        }
    }

    // Méthode pour traiter les paramètres d'un objet annoté avec @RequestObject
    public static void processRequestObject(Object obj, HttpServletRequest request) {
        Class<?> clazz = obj.getClass();

        // Parcourir les champs de la classe
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(RequestSubParameter.class)) {
                // Récupérer l'annotation
                RequestSubParameter paramAnnotation = field.getAnnotation(RequestSubParameter.class);
                // Récupérer le nom du paramètre à partir de l'annotation
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
            }
        }
    }

    // Méthode pour exécuter le traitement des annotations sur un objet
    public static Object execute(Object obj, HttpServletRequest request) {
        processObjectParams(obj, request);
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
}
