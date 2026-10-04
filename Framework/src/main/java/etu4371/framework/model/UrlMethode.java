package etu4371.framework.model;

import java.util.Objects;

public class UrlMethode {
    private String url;
    private String methode;

    public UrlMethode(String url, String methode) {
        this.url = url;
        this.methode = methode;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethode() {
        return methode;
    }

    public void setMethode(String methode) {
        this.methode = methode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlMethode that = (UrlMethode) o;
        return Objects.equals(url, that.url) && Objects.equals(methode, that.methode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, methode);
    }
}
