package etu4112.framework.model;

import java.util.HashMap;


public class ModelAndView {

    private String view;
    private HashMap<String, Object> data = new HashMap<>();

    // Constructeurs
    public ModelAndView() {
    }

    public ModelAndView(String view) {
        this.view = view;
    }

  
    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }


    public void setAttribute(String key, Object value) {
        this.data.put(key, value);
    }


    public HashMap<String, Object> getAttributes() {
        return data;
    }
}
