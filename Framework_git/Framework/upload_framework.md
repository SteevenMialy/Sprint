# Gestion de l'upload dans le framework

## Principe

Quand un formulaire envoie un fichier, ce n'est pas du texte. Le navigateur envoie les données en mode "multipart" : chaque champ du formulaire devient une **Part** (une partie de la requête).

- un champ texte → on le lit avec `request.getParameter("nom")` (comme avant)
- un champ fichier → on le lit avec `request.getPart("nom")`

Le fichier est du **binaire** : une suite d'octets. En Java, on le représente avec un `byte[]`.

Le prof demande de créer une classe spéciale pour les fichiers. Ainsi, quand le framework voit qu'un paramètre est de type `FileUpload`, il sait qu'il doit lire une Part et non un texte.

---

## Étape 1 : la classe FileUpload

Cette classe contient les informations du fichier reçu.

```java
package framework.upload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUpload {
    private String fileName;     // nom d'origine, ex: photo.png
    private String contentType;  // type du fichier, ex: image/png
    private long size;           // taille en octets
    private byte[] content;      // le contenu binaire

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public byte[] getContent() { return content; }
    public void setContent(byte[] content) { this.content = content; }

    // Écrit le fichier sur le disque dans le dossier donné
    public void save(String dossier) throws IOException {
        Path dir = Paths.get(dossier);
        Files.createDirectories(dir);
        String nomSecurise = Paths.get(fileName).getFileName().toString();
        Files.write(dir.resolve(nomSecurise), content);
    }
}
```

Pourquoi `getFileName()` dans `save` : un utilisateur malveillant pourrait envoyer un nom comme `../../fichier`. Cette ligne garde seulement le nom, sans chemin, pour que le fichier reste dans le dossier prévu.

---

## Étape 2 : activer le multipart sur le FrontController

Ajoute l'annotation `@MultipartConfig` sur ta servlet :

```java
@MultipartConfig
public class FrontController extends HttpServlet {
    ...
}
```

Sans elle, `request.getPart()` lance une erreur. Avec elle, `getParameter()` continue de marcher pour les champs texte, donc ton code existant ne change pas.

Si tu n'utilises pas d'annotation de servlet, tu peux mettre `<multipart-config/>` dans la déclaration de la servlet dans `web.xml`.

Attention à l'import : selon ta version de Tomcat, c'est `jakarta.servlet.http.Part` (Tomcat 10 et plus) ou `javax.servlet.http.Part` (Tomcat 9 et moins). Utilise le même que pour ta `HttpServlet`.

---

## Étape 3 : convertir une Part en FileUpload

Dans le framework, ajoute cette méthode :

```java
private FileUpload toFileUpload(Part part) throws IOException {
    if (part == null || part.getSize() == 0) {
        return null; // aucun fichier choisi
    }
    FileUpload f = new FileUpload();
    f.setFileName(part.getSubmittedFileName());
    f.setContentType(part.getContentType());
    f.setSize(part.getSize());
    f.setContent(part.getInputStream().readAllBytes());
    return f;
}
```

- `getSubmittedFileName()` : le nom du fichier chez l'utilisateur
- `getInputStream().readAllBytes()` : lit tout le contenu et le met dans un `byte[]`
- le test au début gère le cas où l'utilisateur n'a rien choisi dans le formulaire

---

## Étape 4 : détecter le type dans le remplissage des paramètres

Là où ton framework construit les arguments de la méthode du contrôleur, ajoute un test sur le type du paramètre :

```java
Class<?> type = parametre.getType();

if (type == FileUpload.class) {
    args[i] = toFileUpload(request.getPart(nomDuChamp));
} else {
    // ton code habituel (String, int, objet...)
}
```

`nomDuChamp` est le `name` de l'input dans le formulaire HTML. Il doit être identique au nom du paramètre Java (ou à ce que tu utilises déjà pour les autres paramètres, par exemple une annotation).

### Si le fichier est un attribut d'un objet

Si ton framework remplit des objets (par exemple `Produit` avec un attribut `FileUpload photo`), il faut faire le même test au moment de remplir chaque attribut :

```java
if (attribut.getType() == FileUpload.class) {
    valeur = toFileUpload(request.getPart(nomDuChamp));
}
```

Sans ce test, ton code essaierait de convertir un texte en `FileUpload` et planterait.

---

## Étape 5 : protéger contre les requêtes non multipart

`getPart()` lance une erreur si la requête n'est pas de type multipart (par exemple un formulaire sans `enctype`, ou une requête GET). Vérifie avant :

```java
private boolean isMultipart(HttpServletRequest request) {
    String type = request.getContentType();
    return type != null && type.toLowerCase().startsWith("multipart/");
}
```

Puis, dans l'étape 4 :

```java
if (type == FileUpload.class) {
    if (isMultipart(request)) {
        args[i] = toFileUpload(request.getPart(nomDuChamp));
    } else {
        args[i] = null;
    }
}
```

---

## Le formulaire HTML (côté utilisateur du framework)

Ce n'est pas dans le framework, mais il faut le savoir pour tester. Il faut absolument `method="post"` et `enctype="multipart/form-data"` :

```html
<form action="upload" method="post" enctype="multipart/form-data">
    <input type="text" name="titre">
    <input type="file" name="photo">
    <button type="submit">Envoyer</button>
</form>
```

Sans `enctype="multipart/form-data"`, le navigateur envoie seulement le nom du fichier, pas son contenu.

---

## Exemple d'utilisation dans un contrôleur

```java
@Url("/upload")
public ModelView upload(String titre, FileUpload photo) throws IOException {
    ModelView mv = new ModelView("resultat.jsp");
    if (photo != null) {
        photo.save("C:/uploads");
        mv.addObject("nom", photo.getFileName());
        mv.addObject("taille", photo.getSize());
    }
    return mv;
}
```

---

## Plusieurs fichiers (optionnel)

Si le paramètre est un tableau `FileUpload[]` (input avec `multiple`), il faut lire toutes les parts portant ce nom :

```java
List<FileUpload> liste = new ArrayList<>();
for (Part p : request.getParts()) {
    if (p.getName().equals(nomDuChamp) && p.getSize() > 0) {
        liste.add(toFileUpload(p));
    }
}
args[i] = liste.toArray(new FileUpload[0]);
```

---

## Résumé

1. Créer la classe `FileUpload` avec `save()`.
2. Mettre `@MultipartConfig` sur le FrontController.
3. Écrire `toFileUpload(Part)`.
4. Tester `type == FileUpload.class` dans le remplissage des paramètres (et des attributs d'objet).
5. Vérifier que la requête est multipart avant `getPart()`.

## Pour tester

1. Lance un formulaire avec `enctype="multipart/form-data"`.
2. Envoie un fichier : il doit apparaître dans le dossier donné à `save()`.
3. Envoie le formulaire sans choisir de fichier : le paramètre doit être `null`, sans erreur.
4. Vérifie qu'un champ texte du même formulaire (`titre`) est toujours bien lu.
