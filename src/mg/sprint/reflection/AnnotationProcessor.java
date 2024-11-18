package mg.sprint.reflection;

import com.google.gson.Gson;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import mg.sprint.annotation.Controller;
import mg.sprint.annotation.Maximum;
import mg.sprint.annotation.Minimum;
import mg.sprint.annotation.Nullable;
import mg.sprint.annotation.Numeric;
import mg.sprint.annotation.RequestObject;
import mg.sprint.annotation.RequestSubParameter;
import mg.sprint.reflection.AnnotationProcessor;
import mg.sprint.session.MySession;
import mg.sprint.controller.ModelView;
import mg.sprint.controller.VerbAction;
import mg.sprint.controller.FileManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;

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

    public static void processFileUpload(HttpServletRequest req, FileManager fileManager) throws Exception {
        Collection<Part> parts = req.getParts(); 

        for (Part part : parts) {
            // Vérifier si la partie est un fichier (et non un champ de formulaire classique)
            if (part.getSubmittedFileName() != null) {
                String fileName = part.getSubmittedFileName();
                fileManager.setFileName(fileName);

                InputStream fileContent = part.getInputStream();
                byte[] fileBytes = fileContent.readAllBytes();
                fileManager.setFileContent(fileBytes);

                fileContent.close();
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

    public static void RestapiProcess(Object result,HttpServletRequest req, HttpServletResponse resp) throws Exception{
    // Gestion des réponses REST avec JSON
        Gson gson = new Gson();
        String jsonResponse;

        if (result instanceof ModelView) {
            ModelView modelView = (ModelView) result;
            jsonResponse = gson.toJson(modelView.getData());
        } else {
            jsonResponse = gson.toJson(result);
        }

        // Configurer et envoyer la réponse JSON
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(jsonResponse);

    }

    public static void UsualProcess(Object result,HttpServletRequest req, HttpServletResponse resp) throws Exception{
        PrintWriter out = resp.getWriter();
        if (result instanceof String) {
            out.println(result);
        } else if (result instanceof ModelView) {
            ModelView modelView = (ModelView) result;
            HashMap<String, Object> data = modelView.getData();
            // Transférer les données vers la vue
            for (String key : data.keySet()) {
                req.setAttribute(key, data.get(key));
            }
            // Faire une redirection vers la vue associée
            RequestDispatcher dispatcher = req.getRequestDispatcher(modelView.getUrl());
            dispatcher.forward(req, resp);
        } else {
            throw new Exception("Type de retour non reconnu");
        }
    }

    public static void checkVerification(Object obj) throws Exception {
        Class<?> clazz = obj.getClass();
        
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true); 
            
            for (Annotation annotation : field.getAnnotations()) {
                Object value = field.get(obj);
                
                if (annotation instanceof Minimum) {
                    Minimum minAnnotation = (Minimum) annotation;
                    double minValue = minAnnotation.value();
                    
                    if (value instanceof Number && ((Number) value).doubleValue() < minValue) {
                        throw new IllegalArgumentException(field.getName() + " est inférieur au minimum autorisé: " + minValue);
                    }
                }
                
                if (annotation instanceof Maximum) {
                    Maximum maxAnnotation = (Maximum) annotation;
                    double maxValue = maxAnnotation.value();
                    
                    if (value instanceof Number && ((Number) value).doubleValue() > maxValue) {
                        throw new IllegalArgumentException(field.getName() + " dépasse le maximum autorisé: " + maxValue);
                    }
                }

                if (annotation instanceof Nullable) {
                    if (value == null) {
                        throw new IllegalArgumentException(field.getName() + " ne peut pas être nul.");
                    }
                }

                if (annotation instanceof Numeric) {
                    if (value == null) {
                        throw new IllegalArgumentException(
                            "Le champ " + field.getName() + " annoté avec @Numeric ne peut pas être null."
                        );
                    }

                    String valueAsString = value.toString(); // Convertit la valeur en chaîne
                    if (!valueAsString.matches("\\d+")) { // Vérifie si elle est composée uniquement de chiffres
                        throw new IllegalArgumentException(
                            "Le champ " + field.getName() + " doit être composé uniquement de chiffres. Valeur actuelle : " + valueAsString
                        );
                    }
                }
            }
        }
    }

    public static void ParameterProcess(Method method, List<Object> methodParams, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        for (Parameter parameter : method.getParameters()) {
            if (parameter.getType().equals(HttpServletRequest.class)) {
                methodParams.add(req);
            } else if (parameter.getType().equals(HttpServletResponse.class)) {
                methodParams.add(resp);
            } else if (parameter.isAnnotationPresent(RequestObject.class)) {
                // Gérer les objets annotés avec @RequestObject
                Class<?> parameterType = parameter.getType();
    
                // Vérifier si l'objet est de type FileManager
                if (parameterType.equals(FileManager.class)) {
                    FileManager fileManager = new FileManager();
                    // Traiter le fichier uploadé avec la méthode processFileUpload
                    AnnotationProcessor.processFileUpload(req, fileManager);
                    methodParams.add(fileManager); // Ajouter le fileManager traité aux paramètres de la méthode
                } else {
                    // Gérer les autres objets annotés avec @RequestObject
                    Object parameterObject = parameterType.getDeclaredConstructor().newInstance();
                    // Exécuter le traitement des annotations pour les autres types d'objets
                    AnnotationProcessor.execute(parameterObject, req);
                    AnnotationProcessor.checkVerification(parameterObject);
                    methodParams.add(parameterObject);
                }
            } else if (parameter.getType().equals(MySession.class)) {
                methodParams.add(new MySession(req.getSession()));
            } else {
                methodParams.add(null);
            }
        }
    } 

    // Gestion des url pour eviter qu'ils ont le même verb
    public static void VerbActionProcess(ArrayList<String> listeUrl,ArrayList<VerbAction> listeVerbAction,List<String> list){
        for (int i = 0; i < listeUrl.size(); i++) {
            for (int j = i + 1; j < listeUrl.size(); j++) {
                if (listeUrl.get(i).equals(listeUrl.get(j))) {
                    // Vérifier si les actions associées ont le même verbe
                    if (listeVerbAction.get(i).getVerb().equals(listeVerbAction.get(j).getVerb())) {
                        list.add("L'URL " + listeUrl.get(i) + " est déjà associée au verbe " + listeVerbAction.get(i).getVerb());    
                    }
                }
            }
        }
    }

    // Gestion des erreurs présent
    public static void init_error(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (ErrorTracker.hasErrors()) {
            List<ErrorDetails> errors = ErrorTracker.getErrors();
    
            ErrorDetails firstError = errors.get(0);
    
            resp.setStatus(firstError.getStatusCode());
    
            resp.setContentType("text/html");
    
            PrintWriter writer = resp.getWriter();
            writer.write("<html>");
            writer.write("<head>");
            writer.write("<title>Erreur " + firstError.getStatusCode() + "</title>"); 
            writer.write("</head>");
            writer.write("<body>");
            writer.write("<h1>Erreur " + firstError.getStatusCode() + "</h1>"); 
    
            for (ErrorDetails errorDetails : errors) {
                writer.write("<p>" + errorDetails.getMessage() + "</p>"); 
            }
    
            writer.write("</body>");
            writer.write("</html>");
    
            // Nettoyer le writer après écriture
            writer.flush();
    
            ErrorTracker.clearErrors();
            return;
        }
    }
}