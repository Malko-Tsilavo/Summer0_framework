package mg.sprint.controller;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.sprint.annotation.Controller;
import mg.sprint.annotation.GetMapping;
import mg.sprint.reflection.Reflect;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

public class FrontController extends HttpServlet {

    private final HashMap<String, Mapping> urlMappings = new HashMap<>();

    @Override
    public void init() {
        String controllersPackage = this.getInitParameter("controllers_package");
        try {
            List<Class<?>> controllers = Reflect.getAnnotatedClasses(controllersPackage, Controller.class);

            for (Class<?> controller : controllers) {
                for (Method method : controller.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(GetMapping.class)) {
                        GetMapping getMapping = method.getAnnotation(GetMapping.class);
                        String url = getMapping.value();

                        // Check if the URL has already been mapped
                        if (urlMappings.containsKey(url)) {
                            throw new IllegalStateException(
                                    "URL " + url + " has already been mapped to another controller's method.");
                        }

                        urlMappings.put(url, new Mapping(controller, method));
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
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

        // Get the part of the URL after the base URL of the webapp
        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        String url = requestURI.substring(contextPath.length());
        Mapping mapping = urlMappings.get(url);

        if (mapping == null) {
            // Set the response status to 404 Not Found
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.println("<h1>Error 404</h1>");
            out.println("<p>La requete url " + url + " est introuvable.</p>");
        } else {
            out.println("<h1>URL: " + url + "</h1>");
            out.println("<p>Controller: " + mapping.getController().getName() + "</p>");
            out.println("<p>Method: " + mapping.getMethod().getName() + "</p>");
        }
    }
}
