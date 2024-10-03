package mg.sprint.controller;

import com.google.gson.Gson;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import mg.sprint.annotation.Controller;
import mg.sprint.annotation.Get;
import mg.sprint.annotation.Post;
import mg.sprint.annotation.RequestObject;
import mg.sprint.annotation.RequestSubParameter;
import mg.sprint.annotation.Restapi;
import mg.sprint.annotation.Url;
import mg.sprint.reflection.Reflect;
import mg.sprint.reflection.AnnotationProcessor;
import mg.sprint.session.MySession;

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
            // Pour chaque classe de contrôleur, récupérer les méthodes annotées avec @Url
            for (Class<?> controller : controllers) {
                for (Method method : controller.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Url.class)) {
                        // Récupérer l'annotation @Url
                        Url urlAnnotation = method.getAnnotation(Url.class);
                        String url = urlAnnotation.value();

                        // Vérifier si la méthode est annotée avec @Post ou @Get
                        boolean hasPost = method.isAnnotationPresent(Post.class);
                        boolean hasGet = method.isAnnotationPresent(Get.class);

                        // Déterminer le verbe (GET par défaut si ni @Post ni @Get ne sont présents)
                        String verb;
                        if (hasPost && hasGet) {
                            throw new IllegalStateException("La méthode " + method.getName() + " ne peut pas avoir à la fois @Post et @Get.");
                        } else if (hasPost) {
                            verb = "POST";
                        } else {
                            verb = "GET";
                        }

                        // Vérifier si l'URL est déjà associée à un verbe différent dans urlMappings
                        if (urlMappings.containsKey(url)) {
                            Mapping existingMapping = urlMappings.get(url);
                            String existingVerb = existingMapping.getVerb();
                            if (!existingVerb.equals(verb)) {
                                throw new IllegalStateException(
                                    "L'URL " + url + " est déjà associée au verbe " + existingVerb);
                            }
                        }

                        // Ajouter l'URL, le contrôleur, la méthode et le verbe au mapping
                        urlMappings.put(url, new Mapping(controller, method, verb));
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
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.println("<h1>Erreur 404</h1>");
            out.println("<p>L'URL " + url + " est introuvable sur ce serveur, veuillez essayer un autre.</p>");
            return;
        }
        // Vérifier que le verbe de la requête correspond au verbe attendu
        String requestVerb = req.getMethod(); // "GET" ou "POST"
        String expectedVerb = mapping.getVerb();

        if (!requestVerb.equalsIgnoreCase(expectedVerb)) {
            // Si le verbe ne correspond pas, retourner une erreur 
            resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            out.println("<p>La méthode " + requestVerb + " n'est pas autorisée pour l'URL " + url + ".</p>");
            return;
        }

        try {
            // Le reste du traitement continue si le verbe est correct
            Object controllerInstance = mapping.getController().getDeclaredConstructor().newInstance();

            // Vérifier et gérer les sessions
            AnnotationProcessor.sessionProcess(controllerInstance, req);

            // Préparer les paramètres de la méthode du contrôleur
            Method method = mapping.getMethod();
            List<Object> methodParams = new ArrayList<>();

            //Gestion des paramètre
            AnnotationProcessor.ParameterProcess(method, methodParams, req, resp);

            Object result = method.invoke(controllerInstance, methodParams.toArray());

            // Vérifier si la méthode est annotée avec @Restapi
            if (method.isAnnotationPresent(Restapi.class)) {
                AnnotationProcessor.RestapiProcess(result, req, resp);
            } else {
                AnnotationProcessor.UsualProcess(result, req, resp);
            }

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            // En cas d'erreur lors de l'invocation du contrôleur, renvoyer une erreur 500
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("<h1>500 Internal Server Error</h1>");
            out.println("<p>Une erreur s'est produite lors de l'invocation du contrôleur : " + e.getMessage() + "</p>");
            e.printStackTrace(out);
        } catch (Exception e) {
            // Afficher le message de l'exception personnalisée
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("<h1>400 Bad Request</h1>");
            out.println("<p>" + e.getMessage() + "</p>");
        }
    }

}