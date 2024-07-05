package mg.sprint.session;

import jakarta.servlet.http.HttpSession;

public class MySession {
    private HttpSession session;

    public MySession(HttpSession session) {
        this.session = session;
    }

    public HttpSession getSession() {
        return session;
    }

    public void setSession(HttpSession session) {
        this.session = session;
    }

    public Object get(String key) {
        return session.getAttribute(key);
    }

    public void add(String key, Object object) {
        if (this.get(key) == null) {
            session.setAttribute(key, object);
        }
    }

    public void update(String key, Object object) {
        if (this.get(key) != null) {
            session.setAttribute(key, object);
        }
    }

    // Méthode pour supprimer un objet de la session par sa clé
    public void delete(String key) {
        session.removeAttribute(key);
    }
}
