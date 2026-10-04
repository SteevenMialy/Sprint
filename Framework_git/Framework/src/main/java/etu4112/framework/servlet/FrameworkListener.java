package etu4112.framework.servlet;

import java.util.HashMap;

import etu4112.framework.model.Mapping;
import etu4112.framework.model.UrlMethode;
import etu4112.framework.util.Utilitaire;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Sprint 4 - FrameworkListener
 * Exécuté par Tomcat au moment du DÉPLOIEMENT (t0), et non au premier chargement (t1).
 * 
 * Approche orientée objet :
 * - On crée le HashMap ici
 * - On le passe par référence à Utilitaire.scanAndFillMap() (void, pas de return)
 * - On le stocke dans le ServletContext pour que le FrontController le récupère
 */
@WebListener
public class FrameworkListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[Framework] Sprint 4 - FrameworkListener : déploiement détecté (t0)");
        System.out.println("[Framework] Scan des classes au DÉPLOIEMENT (pas au premier chargement)...");

        ServletContext context = sce.getServletContext();

        // Récupération du(des) package(s) depuis context-param dans web.xml
        String scanPackage = context.getInitParameter("scan_package");
        if (scanPackage == null || scanPackage.trim().isEmpty()) {
            scanPackage = "";
        }

        // Approche OO : on crée le map, on le passe par référence (void)
        HashMap<UrlMethode, Mapping> mappingUrl = new HashMap<>();

        // Appel void : le map est rempli par référence, pas de return
        Utilitaire.scanAndFillMap(scanPackage, mappingUrl);

        // Stocker dans le contexte pour que le FrontController le récupère
        context.setAttribute("mappingUrl", mappingUrl);

        System.out.println("[Framework] Sprint 4 - Listener terminé. " + mappingUrl.size() + " route(s) enregistrée(s) dans le contexte.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[Framework] FrameworkListener : application arrêtée.");
    }
}
