package mg.sprint.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.sprint.annotation.Controller;
import mg.sprint.annotation.GetMapping;
import mg.sprint.annotation.RequestObject;
import mg.sprint.annotation.RequestSubParameter;
import mg.sprint.reflection.Reflect;
import mg.sprint.reflection.AnnotationProcessor;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FrontController extends HttpServlet {
    private final HashMap<String, Mapping> urlMappings = new HashMap<>();

    @Override
    public void init() throws ServletException {
        String controllersPackage = getInitParameter("controllers_package");
        try {
            // Scanner les fichiers pour trouver les classes annotées avec @Controller
            List<Class<?>> controllers = Reflect.getAnnotatedClasses(controllersPackage, Controller.class);
            // Verifie si le controller est vide ou non
            if (controllers.isEmpty()) {
                throw new IllegalStateException("Le package " + controllersPackage + " est vide ou n'existe pas.");
            }

            // Pour chaque classe de contrôleur, récupérer les méthodes annotées avec
            // @GetMapping
            for (Class<?> controller : controllers) {
                for (Method method : controller.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(GetMapping.class)) {
                        GetMapping getMapping = method.getAnnotation(GetMapping.class);
                        String url = getMapping.value();

                        // Vérifier que l'URL n'est pas associee avec plusieurs controller
                        if (urlMappings.containsKey(url)) {
                            throw new IllegalStateException(
                                    "L'URL " + url + " est utiliser par plusieurs controllers.");
                        }

                        // Ajouter l'URL et la méthode à la liste des mappings
                        urlMappings.put(url, new Mapping(controller, method));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        try {
            processRequest(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        try {
            processRequest(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException, Exception {
        PrintWriter out = resp.getWriter();
        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        String url = requestURI.substring(contextPath.length());
        Mapping mapping = urlMappings.get(url);

        // Si le mapping pour l'URL n'existe pas, retourner une erreur 404
        if (mapping == null) {
            // Si l'URL est inexistant
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.println("<h1>Erreur 404</h1>");
            out.println("<p>L'URL " + url + " est introuvable sur ce serveur, veuillez essayer un autre.</p>");
        }

        try {
            // Instancier le contrôleur
            Object controllerInstance = mapping.getController().getDeclaredConstructor().newInstance();
            // Préparer les paramètres de la méthode du contrôleur
            Method method = mapping.getMethod();
            List<Object> methodParams = new ArrayList<>();

            // Gérer les paramètres de la méthode
            for (Parameter parameter : method.getParameters()) {
                if (parameter.getType().equals(HttpServletRequest.class)) {
                    methodParams.add(req);
                } else if (parameter.getType().equals(HttpServletResponse.class)) {
                    methodParams.add(resp);
                } else if (parameter.isAnnotationPresent(RequestObject.class)) {
                    // Gérer les objets annotés avec @RequestObject
                    Class<?> parameterType = parameter.getType();
                    Object parameterObject = parameterType.getDeclaredConstructor().newInstance();

                    // Pour chaque champ de l'objet, gérer les annotations @RequestSubParameter
                    for (Field field : parameterType.getDeclaredFields()) {
                        if (field.isAnnotationPresent(RequestSubParameter.class)) {
                            String paramName = field.getAnnotation(RequestSubParameter.class).value();
                            String paramValue = req.getParameter(paramName);
                            if (paramValue != null) {
                                field.setAccessible(true);
                                Object convertedValue = AnnotationProcessor.convertValue(field.getType(), paramValue);
                                field.set(parameterObject, convertedValue);
                            }
                        }
                    }

                    methodParams.add(parameterObject);
                } else {
                    methodParams.add(null);
                }
            }

            // Appeler la méthode du contrôleur avec les paramètres récupérés
            Object result = method.invoke(controllerInstance, methodParams.toArray());

            // Gérer le type de retour de la méthode du contrôleur
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
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                | NoSuchMethodException e) {
            // En cas d'erreur lors de l'invocation du contrôleur, renvoyer une erreur 500
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("<h1>500 Internal Server Error</h1>");
            out.println(
                    "<p>Une erreur s'est produite lors de l'invocation du contrôleur : " + e.getMessage() + "</p>");
            e.printStackTrace(out);
        }
    }
}
