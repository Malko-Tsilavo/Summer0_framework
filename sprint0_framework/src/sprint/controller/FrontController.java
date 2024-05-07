package sprint.controller;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ProcessRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ProcessRequest(req, resp);
    }

    public void ProcessRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String url = req.getRequestURL().toString();
        PrintWriter out = resp.getWriter();
        out.println("URL inserer: " + url);
        // Ici, vous pouvez ajouter le code pour traiter l'URL selon vos besoins
    }
}
