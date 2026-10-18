package etu4112.framework.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import etu4112.framework.annotation.RestAPI;
import etu4112.framework.model.Mapping;
import etu4112.framework.model.ModelAndView;
import etu4112.framework.model.UrlMethode;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import etu4112.framework.upload.FileUpload;

@MultipartConfig
public class FrontController extends HttpServlet {

    private HashMap<UrlMethode, Mapping> mappingUrl;

    // Sprint 5 - View Resolver : préfixe et suffixe pour les chemins JSP
    private String prefixe;
    private String suffixe;

    @Override
    public void init() throws ServletException {
        super.init();

        // Sprint 4 : récupération du map rempli par le FrameworkListener au déploiement
        // Le listener a déjà fait le scan et rempli le map (passage par référence, void)
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

        // Sprint 5 - Lecture des init-param préfixe et suffixe (view resolver)
        // Dans web.xml : <init-param> prefixe = WEB-INF/views/ , suffixe = .jsp
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

        if (serveStaticResource(request, response, pathInfo)) {
            return;
        }

        UrlMethode currentUrlMethode = new UrlMethode(urlSaisi, requestMethod);

        // Vérification : l'URL existe-t-elle dans le dictionnaire ?
        if (!urlSaisi.isEmpty() && this.mappingUrl.containsKey(currentUrlMethode)) {
            Mapping mapping = this.mappingUrl.get(currentUrlMethode);

            try {
                // Instanciation et invocation de la méthode du contrôleur
                Class<?> cls = Class.forName(mapping.getClassName());
                Object instance = cls.getDeclaredConstructor().newInstance();
                Method m = findControllerMethod(cls, mapping.getMethodName());
                Object[] arguments = resolveArguments(m, request);

                // Sprint 6 : Vérifier si la méthode a l'annotation @RestAPI
                boolean isRestAPI = m.isAnnotationPresent(RestAPI.class);

                // Sprint 6 : Si @RestAPI est présente, retourner du JSON
                if (isRestAPI) {
                    response.setContentType("application/json;charset=UTF-8");
                    Object result = invoke(m, instance, arguments);

                    PrintWriter out = response.getWriter();
                    if (result instanceof String) {
                        // Si c'est une String, l'écrire directement
                        out.print(result);
                    } else {
                        // Sinon, convertir en JSON avec Gson
                        Gson gson = new Gson();
                        String json = gson.toJson(result);
                        out.print(json);
                    }
                    out.flush();
                    return;
                }

                // Sprint 5 : Si pas @RestAPI, exécuter normalement
                Object result = invoke(m, instance, arguments);

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
                    //
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
                    //
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
                    out.println("<h2 style='color: red;'>❌ Erreur lors de l'invocation</h2>");
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
                    out.println("<h3 style='color: red;'>❌ URL [" + requestMethod + "] '/" + urlSaisi + "' non trouvée.</h3>");
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

    private boolean serveStaticResource(HttpServletRequest request,
            HttpServletResponse response, String pathInfo) throws IOException {
        if (pathInfo == null || pathInfo.isEmpty() || pathInfo.endsWith("/")
                || "GET".equalsIgnoreCase(request.getMethod())
                && pathInfo.equals("/upload")) {
            return false;
        }

        String resourcePath = pathInfo.startsWith("/") ? pathInfo : "/" + pathInfo;
        try (InputStream resource = getServletContext().getResourceAsStream(resourcePath)) {
            if (resource == null) {
                return false;
            }

            String contentType = getServletContext().getMimeType(resourcePath);
            if (contentType != null) {
                response.setContentType(contentType);
            }
            resource.transferTo(response.getOutputStream());
            return true;
        }
    }

    private Method findControllerMethod(Class<?> controllerType, String methodName)
            throws NoSuchMethodException {
        for (Method method : controllerType.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new NoSuchMethodException("Méthode '" + methodName + "' introuvable dans "
                + controllerType.getName() + ".");
    }

    /**
     * Sprint 7/7 bis : construit les arguments d'une méthode à partir des
     * paramètres HTTP. Une méthode sans argument reçoit un tableau vide.
     * Un argument simple est associé au paramètre portant le nom de l'argument.
     * Un argument objet est créé puis ses champs sont associés aux paramètres
     * portant le nom de ces champs.
     */
    private Object[] resolveArguments(Method method, HttpServletRequest request) throws Exception {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            return new Object[0];
        }

        Object[] arguments = new Object[parameterTypes.length];
        if (parameterTypes.length == 1 && !isSimpleType(parameterTypes[0])) {
            arguments[0] = bindObject(parameterTypes[0], request);
            return arguments;
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            String parameterName = method.getParameters()[i].getName();
            arguments[i] = resolveParameter(parameterTypes[i], parameterName, request);
        }
        return arguments;
    }

    private Object resolveParameter(Class<?> type, String name, HttpServletRequest request)
            throws Exception {
        if (type == FileUpload.class) {
            return isMultipart(request) ? toFileUpload(request.getPart(name)) : null;
        }

        String value = request.getParameter(name);
        return convertValue(value, type, name);
    }

    private Object bindObject(Class<?> objectType, HttpServletRequest request) throws Exception {
        Object object = objectType.getDeclaredConstructor().newInstance();
        for (Field field : getInstanceFields(objectType)) {
            Object convertedValue = resolveParameter(field.getType(), field.getName(), request);
            if (convertedValue == null && field.getType().isPrimitive()) {
                continue;
            }
            if (convertedValue == null && request.getParameter(field.getName()) == null
                    && field.getType() != FileUpload.class) {
                continue;
            }
            boolean accessible = field.isAccessible();
            try {
                if (!accessible) {
                    field.setAccessible(true);
                }
                field.set(object, convertedValue);
            } finally {
                if (!accessible) {
                    field.setAccessible(false);
                }
            }
        }
        return object;
    }

    private boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }

    private FileUpload toFileUpload(Part part) throws IOException {
        if (part == null || part.getSize() == 0) {
            return null;
        }

        FileUpload fileUpload = new FileUpload();
        fileUpload.setFileName(part.getSubmittedFileName());
        fileUpload.setContentType(part.getContentType());
        fileUpload.setSize(part.getSize());
        try (java.io.InputStream inputStream = part.getInputStream()) {
            fileUpload.setContent(inputStream.readAllBytes());
        }
        return fileUpload;
    }

    private Field[] getInstanceFields(Class<?> objectType) {
        Map<String, Field> fields = new HashMap<>();
        Class<?> currentType = objectType;
        while (currentType != null && currentType != Object.class) {
            for (Field field : currentType.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers) && !field.isSynthetic()) {
                    fields.putIfAbsent(field.getName(), field);
                }
            }
            currentType = currentType.getSuperclass();
        }
        return fields.values().toArray(new Field[0]);
    }

    private boolean isSimpleType(Class<?> type) {
        return type == String.class
                || type == Integer.class || type == int.class
                || type == Long.class || type == long.class
                || type == Double.class || type == double.class
                || type == Float.class || type == float.class
                || type == Short.class || type == short.class
                || type == Byte.class || type == byte.class
                || type == Boolean.class || type == boolean.class
                || type == Character.class || type == char.class;
    }

    private Object convertValue(String value, Class<?> targetType, String parameterName) {
        if (value == null || value.trim().isEmpty()) {
            if (!targetType.isPrimitive()) {
                return null;
            }
            throw new IllegalArgumentException("Le paramètre '" + parameterName
                    + "' est obligatoire pour le type primitif " + targetType.getSimpleName() + ".");
        }

        try {
            if (targetType == String.class) return value;
            if (targetType == Integer.class || targetType == int.class) return Integer.valueOf(value);
            if (targetType == Long.class || targetType == long.class) return Long.valueOf(value);
            if (targetType == Double.class || targetType == double.class) return Double.valueOf(value);
            if (targetType == Float.class || targetType == float.class) return Float.valueOf(value);
            if (targetType == Short.class || targetType == short.class) return Short.valueOf(value);
            if (targetType == Byte.class || targetType == byte.class) return Byte.valueOf(value);
            if (targetType == Boolean.class || targetType == boolean.class) return Boolean.valueOf(value);
            if (targetType == Character.class || targetType == char.class) {
                if (value.length() != 1) {
                    throw new IllegalArgumentException("Le paramètre '" + parameterName
                            + "' doit contenir un seul caractère.");
                }
                return value.charAt(0);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Le paramètre '" + parameterName
                    + "' a une valeur invalide pour le type " + targetType.getSimpleName() + ".", e);
        }
        throw new IllegalArgumentException("Type de paramètre non supporté : "
                + targetType.getName() + " (paramètre '" + parameterName + "').");
    }

    private Object invoke(Method method, Object instance, Object[] arguments) throws Exception {
        try {
            return method.invoke(instance, arguments);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
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