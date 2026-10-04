package etu4112.framework.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import etu4112.framework.annotation.Url;
import etu4112.framework.model.Mapping;
import etu4112.framework.model.UrlMethode;
import etu4112.framework.util.Utilitaire;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontController extends HttpServlet {

    private List<Class<?>> classesAnnotees;

    private HashMap<UrlMethode, Mapping> mappingUrl = new HashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            System.out.println("[Framework] Sprint 2 - Remplissage du dictionnaire...");

            List<Class<?>> classes = Utilitaire.scanPackageForAnnotation("test.controller",
                    etu4112.framework.annotation.Monannotation.class);

            for (Class<?> clazz : classes) {
                Method[] methods = clazz.getDeclaredMethods();

                for (Method method : methods) {
                    // Si la méthode possède l'annotation @Url
                    if (method.isAnnotationPresent(Url.class)) {
                        Url urlAnnotation = method.getAnnotation(Url.class);
                        String urlValue = urlAnnotation.value(); // Ex: "user-liste"
                        String httpMethod = urlAnnotation.method(); // Ex: "GET" ou "POST"

                        // On crée l'UrlMethode associé
                        UrlMethode urlMethode = new UrlMethode(urlValue, httpMethod);

                        // Vérifier si l'URL avec la méthode HTTP existe déjà
                        if (this.mappingUrl.containsKey(urlMethode)) {
                            Mapping existingMapping = this.mappingUrl.get(urlMethode);
                            throw new ServletException("CONFLIT D'URL : L'URL '" + urlValue + "' avec la méthode '" + httpMethod + 
                                    "' est déjà définie dans " + existingMapping.getClassName() + "." + existingMapping.getMethodName() + 
                                    "(). Conflit avec " + clazz.getSimpleName() + "." + method.getName() + "()");
                        }

                        // On crée le Mapping associé
                        Mapping mapping = new Mapping(clazz.getName(), method.getName(), urlMethode);

                        // On l'ajoute dans notre HashMap
                        this.mappingUrl.put(urlMethode, mapping);
                        System.out.println("[Framework] Route associée : /" + urlValue + " [" + httpMethod + "] -> " + clazz.getSimpleName()
                                + "." + method.getName() + "()");
                    }
                }
            }
            System.out.println("[Framework] Dictionnaire complété avec " + mappingUrl.size() + " route(s).");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");
        
        // Récupérer la méthode HTTP (GET ou POST)
        String httpMethod = request.getMethod();
        
        try (PrintWriter out = response.getWriter()) {
            // 1. On récupère d'abord l'URL saisie dans le chemin de navigation (ex:
            // /user-liste)
            String pathInfo = request.getPathInfo();
            String urlSaisi = (pathInfo != null && pathInfo.length() > 1) ? pathInfo.substring(1) : "";

            // 2. On récupère l'éventuelle URL corrigée passée par paramètre (ex:
            // ?url=user-liste)
            String urlParam = request.getParameter("url");

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head><title>Sprint 3 - Framework avec méthodes HTTP</title></head>");
            out.println("<body style='font-family: sans-serif; margin: 20px;'>");

            out.println("<h2>--- Framework Sprint 3 ---</h2>");
            out.println("<p>Méthode HTTP : " + httpMethod + "</p>");
            out.println("<p>URI complète : " + request.getRequestURI() + "</p>");
            out.println("<p>URL relative : " + request.getPathInfo() + "</p>");

            out.println("<hr>");

            out.println("<h2>--- Routes disponibles ---</h2>");
            out.println("<ul>");
            if (this.mappingUrl.isEmpty()) {
                out.println("<li style='color: red;'>Aucune route trouvée.</li>");
            }else{
                // On affiche les routes avec leurs méthodes HTTP
                for (UrlMethode key : this.mappingUrl.keySet()) {
                    Mapping m = this.mappingUrl.get(key);
                    out.println("<li><strong>/" + key.getUrl() + "</strong> [" + key.getMethod() + "] -> " + m.getClassName() + "." + m.getMethodName() + "()</li>");
                }
            }
            out.println("</ul>");
            
            // ==========================================
            // SCÉNARIO 3 : L'utilisateur a corrigé son URL (via le paramètre ?url=...)
            // ==========================================
            if (urlParam != null && !urlParam.isEmpty()) {
                UrlMethode urlMethode = new UrlMethode(urlParam, httpMethod);
                if (this.mappingUrl.containsKey(urlMethode)) {
                    Mapping mapping = this.mappingUrl.get(urlMethode);
                    executeMethod(mapping, request, response, out);
                } else {
                    out.println("<p style='color: red;'> L'URL de correction '" + urlParam
                            + "' avec la méthode " + httpMethod + " n'est pas valide.</p>");
                    showAvailableUrls(out, urlParam, request);
                }
            }

            // ==========================================
            // SCÉNARIO 1 : L'URL dans la barre d'adresse est valide dès le début
            // ==========================================
            else if (!urlSaisi.isEmpty()) {
                UrlMethode urlMethode = new UrlMethode(urlSaisi, httpMethod);
                if (this.mappingUrl.containsKey(urlMethode)) {
                    Mapping mapping = this.mappingUrl.get(urlMethode);
                    executeMethod(mapping, request, response, out);
                } else {
                    out.println("<div style='border: 2px solid #dc3545; background: #fdf2f2; padding: 15px; border-radius: 5px;'>");
                    out.println("<h3 style='color: #dc3545; margin-top: 0;'> URL '/" + urlSaisi + "' avec méthode " + httpMethod + " non trouvée.</h3>");
                    showAvailableUrls(out, urlSaisi, request);
                    out.println("</div>");
                }
            }

            // ==========================================
            // SCÉNARIO 2 : L'URL est vide
            // ==========================================
            else {
                out.println("<div style='border: 2px solid #6c757d; background: #f8f9fa; padding: 15px; border-radius: 5px;'>");
                out.println("<h3 style='color: #6c757d; margin-top: 0;'>Bienvenue ! Aucune URL spécifiée.</h3>");
                out.println("<p>Veuillez choisir une route :</p>");
                showAvailableUrls(out, null, request);
                out.println("</div>");
            }
            out.println("</body>");
            out.println("</html>");

        }
    }
    
    private void executeMethod(Mapping mapping, HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        try {
            out.println("<div style='border: 2px solid #28a745; background: #e8f5e9; padding: 15px; border-radius: 5px; margin-bottom: 20px;'>");
            out.println("<h3 style='color: #28a745; margin-top: 0;'>Exécution de la méthode</h3>");
            out.println("<p>Classe : <strong>" + mapping.getClassName() + "</strong></p>");
            out.println("<p>Méthode : <strong>" + mapping.getMethodName() + "()</strong></p>");
            out.println("<p>URL : <strong>/" + mapping.getUrlMethode().getUrl() + "</strong> [" + mapping.getUrlMethode().getMethod() + "]</p>");
            out.println("</div>");
            
            // Instanciation de la classe et invocation de la méthode
            Class<?> clazz = Class.forName(mapping.getClassName());
            Object instance = clazz.getDeclaredConstructor().newInstance();
            Method method = clazz.getDeclaredMethod(mapping.getMethodName());
            method.invoke(instance);
            
            out.println("<p style='color: green; font-weight: bold;'>Méthode exécutée avec succès !</p>");
            
        } catch (Exception e) {
            out.println("<p style='color: red;'> Erreur lors de l'exécution : " + e.getMessage() + "</p>");
            e.printStackTrace();
        }
    }
    
    private void showAvailableUrls(PrintWriter out, String currentUrl, HttpServletRequest request) {
        out.println("<p>Routes disponibles :</p>");
        out.println("<ul>");
        for (UrlMethode key : this.mappingUrl.keySet()) {
            Mapping m = this.mappingUrl.get(key);
            out.println("<li style='margin-bottom: 8px;'>");
            out.println("<a href='" + request.getContextPath() + "/App/?url=" + key.getUrl()
                    + "' style='font-weight: bold; color: #0056b3;'>/" + key.getUrl() + "</a>");
            out.println(" <span style='color: #666;'>[" + key.getMethod() + "]</span>");
            out.println("</li>");
        }
        out.println("</ul>");
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