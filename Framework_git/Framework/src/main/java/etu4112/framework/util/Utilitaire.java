package etu4112.framework.util;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import etu4112.framework.annotation.Monannotation;
import etu4112.framework.annotation.Url;
import etu4112.framework.model.Mapping;
import etu4112.framework.model.UrlMethode;

public class Utilitaire {
    // Cette liste va stocker l'historique du scan pour qu'on puisse comprendre le bug
    public static List<String> debugLogs = new ArrayList<>();

    public static List<Class<?>> scanPackageForAnnotation(String packageName, Class<? extends java.lang.annotation.Annotation> annotationClass) {
        List<Class<?>> annotatedClasses = new ArrayList<>();
        debugLogs.clear();
        debugLogs.add("Début du scan pour le package : '" + packageName + "'");

        try {
            String packagePath = packageName.replace('.', '/');
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            Enumeration<URL> resources = classLoader.getResources(packagePath);

            if (!resources.hasMoreElements()) {
                debugLogs.add("⚠️ AUCUNE ressource trouvée pour le chemin : " + packagePath);
            }

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                debugLogs.add("Ressource trouvée ! Protocole : '" + resource.getProtocol() + "' | URL : " + resource.toString());

                String decodedPath = URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8);

                if (decodedPath.startsWith("file:")) {
                    decodedPath = decodedPath.substring(5);
                }
                if (decodedPath.contains(".jar!")) {
                    decodedPath = decodedPath.substring(0, decodedPath.indexOf(".jar!") + 4);
                }

                File directory = new File(decodedPath);
                debugLogs.add("Chemin de fichier créé : " + directory.getAbsolutePath() + " (Existe : " + directory.exists() + ")");

                if (directory.exists() && directory.isDirectory()) {
                    findClasses(directory, packageName, annotationClass, annotatedClasses);
                }
            }
        } catch (Exception e) {
            debugLogs.add("❌ Erreur pendant le scan : " + e.getMessage());
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            debugLogs.add(sw.toString());
        }
        return annotatedClasses;
    }

    /**
     * Sprint 4 - Approche orientée objet : void, passage par référence.
     * Le HashMap est passé en argument et rempli directement.
     * Pas de return : quand on modifie le map ici, ça change partout (passage par référence).
     * 
     * @param scanPackage  Les packages à scanner, séparés par des virgules
     * @param mappingUrl   Le HashMap à remplir (passé par référence)
     */
    public static void scanAndFillMap(String scanPackage, HashMap<UrlMethode, Mapping> mappingUrl) {
        System.out.println("[Framework] Sprint 4 - scanAndFillMap (void, passage par référence)...");

        String[] packagesToScan = scanPackage.split(",");

        for (String pack : packagesToScan) {
            String p = pack.trim();
            System.out.println("[Framework] Package à scanner : '" + p + "'");

            // Scan des classes annotées @Monannotation
            List<Class<?>> classes = scanPackageForAnnotation(p, Monannotation.class);

            // Remplissage du map passé par référence (void, pas de return)
            for (Class<?> clazz : classes) {
                Method[] methods = clazz.getDeclaredMethods();

                for (Method method : methods) {
                    if (method.isAnnotationPresent(Url.class)) {
                        Url urlAnnotation = method.getAnnotation(Url.class);
                        String urlValue = urlAnnotation.value();
                        String methodValue = urlAnnotation.method().toUpperCase();

                        UrlMethode urlMethode = new UrlMethode(urlValue, methodValue);
                        Mapping mapping = new Mapping(clazz.getName(), method.getName());

                        // Passage par référence : on modifie le map directement
                        mappingUrl.put(urlMethode, mapping);

                        System.out.println("[Framework] Route associée : [" + methodValue + "] /" + urlValue
                                + " -> " + clazz.getSimpleName() + "." + method.getName() + "()");
                    }
                }
            }
        }

        System.out.println("[Framework] scanAndFillMap terminé. " + mappingUrl.size() + " route(s).");
    }

    private static void findClasses(File directory, String packageName, Class<? extends java.lang.annotation.Annotation> annotationClass, List<Class<?>> annotatedClasses) {
        File[] files = directory.listFiles();
        if (files == null) {
            debugLogs.add("Dossier vide ou inaccessible : " + directory.getAbsolutePath());
            return;
        }

        debugLogs.add("Scan du dossier : " + directory.getAbsolutePath() + " (" + files.length + " fichiers trouvés)");

        for (File file : files) {
            if (file.isDirectory()) {
                String subPackageName = packageName.isEmpty() ? file.getName() : packageName + "." + file.getName();
                findClasses(file, subPackageName, annotationClass, annotatedClasses);
            } else if (file.getName().endsWith(".class")) {
                try {
                    String className = file.getName().substring(0, file.getName().length() - 6);
                    String fullClassName = packageName.isEmpty() ? className : packageName + "." + className;

                    debugLogs.add("Fichier .class trouvé : " + fullClassName);

                    Class<?> clazz = Class.forName(fullClassName);
                    if (clazz.isAnnotationPresent(annotationClass)) {
                        annotatedClasses.add(clazz);
                        debugLogs.add("🎉 CLASSE ANNOTÉE VALIDÉE : " + fullClassName);
                    }
                } catch (Exception | NoClassDefFoundError e) {
                    debugLogs.add("Impossible de charger la classe " + file.getName() + " : " + e.getMessage());
                }
            }
        }
    }
}