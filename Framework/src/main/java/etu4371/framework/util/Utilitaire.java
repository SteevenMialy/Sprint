package etu4371.framework.util;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
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