package etu4112.framework.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import etu4112.framework.model.Mapping;
import etu4112.framework.model.ModelAndView;
import etu4112.framework.model.UrlMethode;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontController extends HttpServlet {

    private HashMap<UrlMethode, Mapping> mappingUrl;

    private String prefixe;
    private String suffixe;

    @Override
    public void init() throws ServletException {
        super.init();

    
        @SuppressWarnings("unchecked")
        HashMap<UrlMethode, Mapping> mapFromContext =
                (HashMap<UrlMethode, Mapping>) getServletContext().getAttribute("mappingUrl");

        if (mapFromContext != null) {
            this.mappingUrl = mapFromContext;
            System.out.println("[Framework] Sprint 4 - FrontController.init() : map récupéré depuis le contexte ("
                    + mappingUrl.size() + " route(s)).");
        } else {
            // Fallback : si le listener n'a pas tourné (ex: pas de context-param)
            this.mappingUrl = new HashMap<>();
            System.out.println("[Framework] ATTENTION : aucun map trouvé dans le contexte. Le FrameworkListener a-t-il été chargé ?");
        }

        this.prefixe = this.getInitParameter("prefixe");
        if (this.prefixe == null) {
            this.prefixe = "WEB-INF/views/";
        }

        this.suffixe = this.getInitParameter("suffixe");
        if (this.suffixe == null) {
            this.suffixe = ".jsp";
        }

        System.out.println("[Framework] Sprint 5 - View Resolver : prefixe='" + prefixe + "' suffixe='" + suffixe + "'");
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        String urlSaisi = (pathInfo != null && pathInfo.length() > 1) ? pathInfo.substring(1) : "";
        String requestMethod = request.getMethod().toUpperCase();

        UrlMethode currentUrlMethode = new UrlMethode(urlSaisi, requestMethod);

        if (!urlSaisi.isEmpty() && this.mappingUrl.containsKey(currentUrlMethode)) {
            Mapping mapping = this.mappingUrl.get(currentUrlMethode);

            try {
                // Instanciation et invocation de la méthode du contrôleur
                Class<?> cls = Class.forName(mapping.getClassName());
                Object instance = cls.getDeclaredConstructor().newInstance();
                Method m = cls.getDeclaredMethod(mapping.getMethodName());
                Object result = m.invoke(instance);

                // Sprint 5 : si le retour est un ModelAndView
                if (result instanceof ModelAndView) {
                    ModelAndView mav = (ModelAndView) result;

                    // Boucle sur les attributs du ModelAndView -> request.setAttribute
                    // Cast type HashMap<String, Object> -> on met chaque entrée dans la request
                    HashMap<String, Object> attributes = mav.getAttributes();
                    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                        request.setAttribute(entry.getKey(), entry.getValue());
                    }

                    // Concaténation : prefixe + view + suffixe
                    // Ex: "WEB-INF/views/" + "emp/liste" + ".jsp" = "WEB-INF/views/emp/liste.jsp"
                    String viewPath = this.prefixe + mav.getView() + this.suffixe;

                    System.out.println("[Framework] Sprint 5 - Forward vers : " + viewPath);

                    // Forward vers la JSP
                    RequestDispatcher dispatcher = request.getRequestDispatcher(viewPath);
                    dispatcher.forward(request, response);
                    return;

                } else if (result instanceof String) {
                    // Si la méthode retourne un String : on l'utilise comme nom de vue
                    String viewPath = this.prefixe + (String) result + this.suffixe;

                    System.out.println("[Framework] Sprint 5 - Forward (String) vers : " + viewPath);

                    RequestDispatcher dispatcher = request.getRequestDispatcher(viewPath);
                    dispatcher.forward(request, response);
                    return;

                } else {
                    // Retour void ou autre : affichage simple de confirmation
                    response.setContentType("text/html;charset=UTF-8");
                    try (PrintWriter out = response.getWriter()) {
                        out.println("<!DOCTYPE html><html><body>");
                        out.println("<h2>✅ Méthode invoquée avec succès</h2>");
                        out.println("<p>[" + requestMethod + "] /" + urlSaisi + " -> "
                                + mapping.getClassName() + "." + mapping.getMethodName() + "()</p>");
                        if (result != null) {
                            out.println("<p>Retour : " + result.toString() + "</p>");
                        }
                        out.println("</body></html>");
                    }
                }

            } catch (Exception e) {
                response.setContentType("text/html;charset=UTF-8");
                try (PrintWriter out = response.getWriter()) {
                    out.println("<!DOCTYPE html><html><body>");
                    out.println("<h2 style='color: red;'> Erreur lors de l'invocation</h2>");
                    out.println("<p>" + e.getMessage() + "</p>");
                    out.println("</body></html>");
                }
                e.printStackTrace();
            }

        } else {
            // URL non trouvée ou vide : page d'accueil debug
            response.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = response.getWriter()) {
                out.println("<!DOCTYPE html><html>");
                out.println("<head><title>Framework - Sprint 4 & 5</title></head>");
                out.println("<body style='font-family: sans-serif; margin: 20px;'>");

                out.println("<h2>--- Framework Sprint 4 (Listener) & Sprint 5 (ModelAndView) ---</h2>");
                out.println("<p>URI : " + request.getRequestURI() + "</p>");
                out.println("<p>Méthode HTTP : <strong>" + requestMethod + "</strong></p>");
                out.println("<p>View Resolver : prefixe=<strong>" + prefixe + "</strong> suffixe=<strong>" + suffixe + "</strong></p>");

                out.println("<hr>");

                if (urlSaisi.isEmpty()) {
                    out.println("<h3>Bienvenue ! Aucune URL spécifiée.</h3>");
                } else {
                    out.println("<h3 style='color: red;'> URL [" + requestMethod + "] '/" + urlSaisi + "' non trouvée.</h3>");
                }

                out.println("<h3>📋 Routes enregistrées :</h3>");
                out.println("<table border='1' cellpadding='10' style='border-collapse: collapse; width: 100%;'>");
                out.println("<tr style='background-color: #f2f2f2;'><th>Méthode HTTP</th><th>URL</th><th>Classe</th><th>Méthode</th></tr>");
                for (UrlMethode um : this.mappingUrl.keySet()) {
                    Mapping mapTable = this.mappingUrl.get(um);
                    out.println("<tr>");
                    out.println("<td><strong>" + um.getMethode() + "</strong></td>");
                    out.println("<td><strong>/" + um.getUrl() + "</strong></td>");
                    out.println("<td>" + mapTable.getClassName() + "</td>");
                    out.println("<td>" + mapTable.getMethodName() + "()</td>");
                    out.println("</tr>");
                }
                out.println("</table>");

                out.println("</body></html>");
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
}