package test.controller;

import etu4112.framework.annotation.Monannotation;
import etu4112.framework.annotation.Url;
@Monannotation
public class Test1 {
    @Url(value = "user-liste", method = "GET")
    public void getToutesLesListes() {
        // Code futur
    }

    @Url(value = "user-liste", method = "GET")
    public void getToutes() {
        // Code futur
    }


    @Url(value = "user-delete", method = "POST")
    public void supprimerUtilisateur() {
        // Code futur
    }
}
