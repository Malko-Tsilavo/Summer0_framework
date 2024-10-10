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
            // Créer deux listes temporaires pour stocker les URL et leurs actions associées
            ArrayList<String> listeUrl = new ArrayList<>();
            ArrayList<VerbAction> listeVerbAction = new ArrayList<>();
            
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
                        // Créer une nouvelle action pour cette méthode
                        VerbAction newVerbAction = new VerbAction();
                        newVerbAction.setMethod(method);
                        newVerbAction.setVerb(verb);

                        // Ajouter l'URL et l'action aux listes temporaires
                        listeUrl.add(url);
                        listeVerbAction.add(newVerbAction);

                        // Ajouter le mapping si l'URL n'existe pas encore
                        if (!urlMappings.containsKey(url)) {
                            ArrayList<VerbAction> verbActions = new ArrayList<>();
                            verbActions.add(newVerbAction);
                            Mapping newMapping = new Mapping(controller, verbActions);
                            urlMappings.put(url, newMapping);
                        }
                    }
                }
            }
            // Appel de la fonction pour vérifier et traiter les doublons d'URL et de verbes
            AnnotationProcessor.VerbActionProcess(listeUrl, listeVerbAction);

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

        // Récupérer le verbe HTTP de la requête (GET ou POST)
        String requestVerb = req.getMethod(); 
        
        // Trouver l'action correspondant au verbe HTTP dans la liste des actions
        VerbAction matchedAction = null;
        for (VerbAction action : mapping.getVerbActions()) {
            if (action.getVerb().equalsIgnoreCase(requestVerb)) {
                matchedAction = action;
                break;
            }
        }

        // Si aucune méthode correspondant au verbe n'est trouvée, retourner une erreur 405
        if (matchedAction == null) {
            resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            out.println("<p>La méthode " + requestVerb + " n'est pas autorisée pour l'URL " + url + ".</p>");
            return;
        }

        try {
            // Instancier le contrôleur
            Object controllerInstance = mapping.getController().getDeclaredConstructor().newInstance();

            // Vérifier et gérer les sessions
            AnnotationProcessor.sessionProcess(controllerInstance, req);

            // Récupérer la méthode à partir de l'action correspondant au verbe
            Method method = matchedAction.getMethod();
            List<Object> methodParams = new ArrayList<>();

            // Gestion des paramètres
            AnnotationProcessor.ParameterProcess(method, methodParams, req, resp);

            // Appeler la méthode du contrôleur
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