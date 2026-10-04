package test.controller;

import etu4112.framework.annotation.Monannotation;
import etu4112.framework.annotation.Url;

@Monannotation
public class TestSprint3 {

    // 1. Test avec la méthode GET explicite
    @Url(value = "test-sprint3-get", method = "GET")
    public void testerGet() {
        System.out.println("======> La méthode testerGet() a été invoquée ! (Méthode GET) <======");
    }

    // 2. Test avec la méthode POST explicite
    @Url(value = "test-sprint3-post", method = "POST")
    public void testerPost() {
        System.out.println("======> La méthode testerPost() a été invoquée ! (Méthode POST) <======");
    }

    // 3. Test avec méthode par défaut (elle doit devenir GET selon l'annotation)
    @Url("test-sprint3-default")
    public void testerDefault() {
        System.out.println("======> La méthode testerDefault() a été invoquée ! (Par défaut, donc GET) <======");
    }
}
