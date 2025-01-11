package mg.sprint.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;

import mg.sprint.annotation.Maximum;
import mg.sprint.annotation.Minimum;
import mg.sprint.annotation.Nullable;
import mg.sprint.annotation.Numeric;

//Classe verifiant les erreurs 
public class Validator {
    public static void checkVerification(Object obj,HashMap<String,String> listError) throws Exception {
        Class<?> clazz = obj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true); 
            
            for (Annotation annotation : field.getAnnotations()) {
                Object value = field.get(obj);
                
                if (annotation instanceof Minimum) {
                    Minimum minAnnotation = (Minimum) annotation;
                    double minValue = minAnnotation.value();
                    
                    if (value instanceof Number && ((Number) value).doubleValue() < minValue) {
                        listError.put(field.getName(),field.getName()
                         + " est inférieur au minimum autorisé: " + minValue);
                    }
                }
                
                if (annotation instanceof Maximum) {
                    Maximum maxAnnotation = (Maximum) annotation;
                    double maxValue = maxAnnotation.value();
                    
                    if (value instanceof Number && ((Number) value).doubleValue() > maxValue) {
                        listError.put(field.getName(),field.getName() + " dépasse le maximum autorisé: " + maxValue);
                    }
                }

                if (annotation instanceof Nullable) {
                    if (value == null) {
                        listError.put(field.getName(),field.getName() + " ne peut pas être nul.");
                    }
                }

                if (annotation instanceof Numeric) {
                    if (value == null) {
                        listError.put(field.getName(),
                            "Le champ " + field.getName() + " annoté avec @Numeric ne peut pas être null."
                        );
                    }

                    String valueAsString = value.toString(); // Convertit la valeur en chaîne
                    if (!valueAsString.matches("\\d+")) { // Vérifie si elle est composée uniquement de chiffres
                        listError.put(field.getName(),
                            "Le champ " + field.getName() + " doit être composé uniquement de chiffres. Valeur actuelle : " + valueAsString
                        );
                    }
                }
            }
        }
    }
}
