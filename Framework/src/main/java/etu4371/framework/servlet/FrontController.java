package etu4371.framework.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import etu4371.framework.annotation.Url;
import etu4371.framework.model.Mapping;
import etu4371.framework.model.UrlMethode;
import etu4371.framework.util.Utilitaire;
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
            System.out.println("[Framework] Sprint 3 - Remplissage du dictionnaire avec Method HTTP...");

            // Récupération du(des) package(s) depuis web.xml
            String scanPackage = this.getInitParameter("scan_package");
            if (scanPackage == null || scanPackage.trim().isEmpty()) {
                scanPackage = ""; 
            }

            String[] packagesToScan = scanPackage.split(",");
            List<Class<?>> classes = new java.util.ArrayList<>();

            for (String pack : packagesToScan) {
                String p = pack.trim();
                System.out.println("[Framework] Package à scanner : '" + p + "'");
                List<Class<?>> scanned = Utilitaire.scanPackageForAnnotation(p,
                        etu4371.framework.annotation.Monannotation.class);
                classes.addAll(scanned);
            }
            this.classesAnnotees = classes;

            for (Class<?> clazz : classes) {
                Method[] methods = clazz.getDeclaredMethods();

                for (Method method : methods) {
                    if (method.isAnnotationPresent(Url.class)) {
                        Url urlAnnotation = method.getAnnotation(Url.class);
                        String urlValue = urlAnnotation.value(); 
                        String methodValue = urlAnnotation.method().toUpperCase();
                        
                        UrlMethode urlMethode = new UrlMethode(urlValue, methodValue);
                        Mapping mapping = new Mapping(clazz.getName(), method.getName());

                        this.mappingUrl.put(urlMethode, mapping);
                        System.out.println("[Framework] Route associée : [" + methodValue + "] /" + urlValue + " -> " + clazz.getSimpleName()
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
        try (PrintWriter out = response.getWriter()) {
            String pathInfo = request.getPathInfo();
            String urlSaisi = (pathInfo != null && pathInfo.length() > 1) ? pathInfo.substring(1) : "";
            String requestMethod = request.getMethod().toUpperCase();

            String urlParam = request.getParameter("url");

            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head><title>Sprint 1, 2 & 3 - Debug complet</title></head>");
            out.println("<body style='font-family: sans-serif; margin: 20px;'>");

            out.println("<h2>--- Framework Sprint 0 ---</h2>");
            out.println("<p>URI complète : " + request.getRequestURI() + "</p>");
            out.println("<p>URL relative pour le futur routeur : " + request.getPathInfo() + "</p>");
            out.println("<p>Méthode HTTP GET/POST : <strong>" + requestMethod + "</strong></p>");

            out.println("<hr>");

            out.println("<h2>--- Framework Sprint 1 (Classes annotées) ---</h2>");
            out.println("<ul>");
            if (this.classesAnnotees == null || this.classesAnnotees.isEmpty()) {
                out.println("<li style='color: red;'>Aucune classe annotée trouvée.</li>");
            } else {
                for (Class<?> clazz : this.classesAnnotees) {
                    out.println("<li><strong>" + clazz.getName() + "</strong></li>");
                }
            }
            out.println("</ul>");

            out.println("<hr>");

            out.println("<h2>--- Framework Sprint 2 & 3 (URLs associées & Méthode) ---</h2>");
            out.println("<ul>");
            if (this.mappingUrl == null || this.mappingUrl.isEmpty()) {
                out.println("<li style='color: red;'>Aucune URL trouvée.</li>");
            } else {
                for (UrlMethode um : this.mappingUrl.keySet()) {
                    Mapping m = this.mappingUrl.get(um);
                    out.println("<li><strong>[" + um.getMethode() + "] /" + um.getUrl() + "</strong> -> " + m.getClassName() + "." + m.getMethodName() + "()</li>");
                }
            }
            out.println("</ul>");
            out.println("<hr>");

            // Execution Verification & Instantiation (Sprint 3 B)
            UrlMethode paramUrlMethode = (urlParam != null && !urlParam.isEmpty()) ? new UrlMethode(urlParam, requestMethod) : null;
            UrlMethode currentUrlMethode = new UrlMethode(urlSaisi, requestMethod);

            // ==========================================
            // SCÉNARIO 3 : URL corrigée via paramètre
            // ==========================================
            if (paramUrlMethode != null) {
                if (this.mappingUrl.containsKey(paramUrlMethode)) {
                    Mapping mapping = this.mappingUrl.get(paramUrlMethode);
                    out.println("<div style='border: 2px solid #0056b3; background: #e6f2ff; padding: 15px; border-radius: 5px;'>");
                    out.println("<h3 style='color: #0056b3; margin-top: 0;'>🎯 URL Corrigée Validée</h3>");
                    out.println("<p>L'URL demandée <strong>[" + paramUrlMethode.getMethode() + "] /" + urlParam + "</strong> correspond à :</p>");
                    out.println("<ul>");
                    out.println("<li><strong>Classe :</strong> " + mapping.getClassName() + "</li>");
                    out.println("<li><strong>Méthode associée :</strong> " + mapping.getMethodName() + "()</li>");
                    out.println("</ul>");
                    
                    try {
                        Class<?> cls = Class.forName(mapping.getClassName());
                        Object instance = cls.getDeclaredConstructor().newInstance();
                        Method m = cls.getDeclaredMethod(mapping.getMethodName());
                        m.invoke(instance);
                        out.println("<p style='color: green;'><strong>✅ Méthode invoquée avec succès ! (Instance créée)</strong></p>");
                    } catch (Exception e) {
                        out.println("<p style='color: red;'><strong>❌ Erreur lors de l'invocation : " + e.getMessage() + "</strong></p>");
                    }
                    
                    out.println("</div>");
                } else {
                    out.println("<p style='color: red;'>⚠️ L'URL de correction '[" + requestMethod + "] /" + urlParam + "' n'est pas valide.</p>");
                }
            }
            // ==========================================
            // SCÉNARIO 1 : URL dans la barre d'adresse
            // ==========================================
            else if (!urlSaisi.isEmpty() && this.mappingUrl.containsKey(currentUrlMethode)) {
                Mapping mapping = this.mappingUrl.get(currentUrlMethode);
                out.println("<div style='border: 2px solid #28a745; background: #e8f5e9; padding: 15px; border-radius: 5px; margin-bottom: 20px;'>");
                out.println("<h3 style='color: #28a745; margin-top: 0;'>🎉 URL Actuelle Validée : [" + currentUrlMethode.getMethode() + "] /" + urlSaisi + "</h3>");
                out.println("<p>Correspondance directe : <strong>" + mapping.getClassName() + "." + mapping.getMethodName() + "()</strong></p>");
                
                try {
                    Class<?> cls = Class.forName(mapping.getClassName());
                    Object instance = cls.getDeclaredConstructor().newInstance();
                    Method m = cls.getDeclaredMethod(mapping.getMethodName());
                    m.invoke(instance);
                    out.println("<p style='color: green;'><strong>✅ Méthode invoquée avec succès ! (Instance créée)</strong></p>");
                } catch (Exception e) {
                    out.println("<p style='color: red;'><strong>❌ Erreur lors de l'invocation : " + e.getMessage() + "</strong></p>");
                }
                
                out.println("</div>");

                out.println("<h3>📋 Liste complète des routes du Framework :</h3>");
                out.println("<table border='1' cellpadding='10' style='border-collapse: collapse; width: 100%; text-align: left;'>");
                out.println("<tr style='background-color: #f2f2f2;'><th>Méthode HTTP</th><th>URL (Route)</th><th>Classe de Contrôleur</th><th>Méthode Cible</th></tr>");
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
            }
            // ==========================================
            // SCÉNARIO 2 : URL vide ou erronée
            // ==========================================
            else {
                out.println("<div style='border: 2px solid #dc3545; background: #fdf2f2; padding: 15px; border-radius: 5px;'>");
                if (urlSaisi.isEmpty()) {
                    out.println("<h3 style='color: #6c757d; margin-top: 0;'>Bienvenue ! Aucune URL spécifiée.</h3>");
                } else {
                    out.println("<h3 style='color: #dc3545; margin-top: 0;'>❌ URL [" + requestMethod + "] '/" + urlSaisi + "' non trouvée.</h3>");
                }

                out.println("<p>Veuillez choisir ou corriger votre URL parmi les routes existantes :</p>");
                out.println("<ul>");
                for (UrlMethode um : this.mappingUrl.keySet()) {
                    out.println("<li style='margin-bottom: 8px;'>");
                    if (um.getMethode().equals("GET")) {
                        out.println("<a href='" + request.getContextPath() + "/App/?url=" + um.getUrl() + "' style='font-weight: bold; color: #0056b3;'>[" + um.getMethode() + "] /" + um.getUrl() + "</a>");
                    } else {
                        out.println("<span style='font-weight: bold; color: #6c757d;'>[" + um.getMethode() + "] /" + um.getUrl() + " (Nécessite form " + um.getMethode() + ")</span>");
                    }
                    out.println("</li>");
                }
                out.println("</ul>");
                out.println("</div>");
            }
            out.println("</div>");
            out.println("</body>");
            out.println("</html>");
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