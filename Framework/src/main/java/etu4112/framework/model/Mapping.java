package etu4112.framework.model;

public class Mapping {
    private String className;
    private String methodName;
    private UrlMethode urlMethode;

    public Mapping(String className, String methodName, UrlMethode urlMethode) {
        this.className = className;
        this.methodName = methodName;
        this.urlMethode = urlMethode;
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    public UrlMethode getUrlMethode() { return urlMethode; }
    public void setUrlMethode(UrlMethode urlMethode) { this.urlMethode = urlMethode; }
}
