package etu4112.framework.model;

import java.util.HashMap;

/**
 * Sprint 5 - ModelAndView
 * Encapsule le nom logique de la vue et les attributs à transmettre à la JSP.
 * Approche orientée objet : setAttribute fait un map.put en interne (passage par référence).
 */
public class ModelAndView {

    private String view;
    private HashMap<String, Object> data = new HashMap<>();

    // Constructeurs
    public ModelAndView() {
    }

    public ModelAndView(String view) {
        this.view = view;
    }

    // --- Vue ---

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    // --- Attributs (données pour la JSP) ---

    /**
     * Ajoute un attribut clé/valeur qui sera transmis à la JSP via request.setAttribute.
     * Approche OO : on modifie le map par référence, pas besoin de return.
     */
    public void setAttribute(String key, Object value) {
        this.data.put(key, value);
    }

    /**
     * Récupère tous les attributs à transmettre à la vue.
     */
    public HashMap<String, Object> getAttributes() {
        return data;
    }
}
