package etu4112.framework.model;

public class UrlMethode {
    private String url;
    private String method;

    public UrlMethode(String url, String method) {
        this.url = url;
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        UrlMethode that = (UrlMethode) obj;
        return url.equals(that.url) && method.equals(that.method);
    }

    @Override
    public int hashCode() {
        return url.hashCode() + method.hashCode();
    }

    @Override
    public String toString() {
        return "UrlMethode{url='" + url + "', method='" + method + "'}";
    }
}
