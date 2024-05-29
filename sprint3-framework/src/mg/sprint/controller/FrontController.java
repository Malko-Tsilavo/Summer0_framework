package mg.sprint.controller;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.sprint.annotation.Controller;
import mg.sprint.annotation.GetMapping;
import mg.sprint.reflection.Reflect;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

public class FrontController extends HttpServlet {
    private final HashMap<String, Mapping> urlMappings = new HashMap<>();

    @Override
    public void init() {
        String controllersPackage = this.getInitParameter("controllers_package");
        try {
            // S'occupe du scan des fichiers
            List<Class<?>> controllers = Reflect.getAnnotatedClasses(controllersPackage, Controller.class);
            for (Class<?> controller : controllers) {
                for (Method method : controller.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(GetMapping.class)) {
                        GetMapping getMapping = method.getAnnotation(GetMapping.class);
                        String url = getMapping.value();
                        if (urlMappings.containsKey(url)) {
                            throw new IllegalStateException("L'URL " + url + " est deja mappee.");
                        }
                        urlMappings.put(url, new Mapping(controller, method));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        processRequest(req, resp);
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        String url = requestURI.substring(contextPath.length());
        Mapping mapping = urlMappings.get(url);

        if (mapping == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.println("<h1>Erreur 404</h1>");
            out.println("<p>L'URL demandee " + url + " n'a pas ete trouvee sur ce serveur.</p>");
        } else {
            try {
                // Instancier le contrôleur
                Object controllerInstance = mapping.getController().getDeclaredConstructor().newInstance();

                // Appeler la méthode du contrôleur
                Method method = mapping.getMethod();
                String result = (String) method.invoke(controllerInstance);

                // Envoyer la réponse
                resp.setContentType("text/plain");
                out.println(result);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                    | NoSuchMethodException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("<h1>500 Internal Server Error</h1>");
                out.println(
                        "<p>Une erreur s'est produite lors de l'invocation du contrôleur : " + e.getMessage() + "</p>");
                e.printStackTrace(out);
            }
        }
    }
}
