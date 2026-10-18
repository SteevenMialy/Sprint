package etu4112.framework.upload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUpload {

    private String fileName;
    private String contentType;
    private long size;
    private byte[] content;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public void save(String folder) throws IOException {
        if (fileName == null || content == null) {
            throw new IOException("Aucun fichier à sauvegarder.");
        }

        Path directory = Paths.get(folder);
        Files.createDirectories(directory);
        String safeFileName = Paths.get(fileName).getFileName().toString();
        Files.write(directory.resolve(safeFileName), content);
    }
}
