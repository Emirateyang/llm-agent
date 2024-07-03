package com.llmagent.openai;

import com.llmagent.util.UUIDUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.Base64;

public class FilePersistor {
    static Path persistFromUri(URI uri, Path destinationFolder) {
        try {
            Path fileName = Paths.get(uri.getPath()).getFileName();
            Path destinationFilePath = destinationFolder.resolve(fileName);
            try (InputStream inputStream = uri.toURL().openStream()) {
                Files.copy(inputStream, destinationFilePath, StandardCopyOption.REPLACE_EXISTING);
            }

            return destinationFilePath;
        } catch (IOException e) {
            throw new RuntimeException("Error persisting file from URI: " + uri, e);
        }
    }

    public static Path persistFromBase64String(String base64EncodedString, Path destinationFolder) throws IOException {
        byte[] decodedBytes = Base64.getDecoder().decode(base64EncodedString);
        Path destinationFile = destinationFolder.resolve(randomFileName());

        Files.write(destinationFile, decodedBytes, StandardOpenOption.CREATE);

        return destinationFile;
    }

    private static String randomFileName() {
        return UUIDUtil.randomUUID().substring(0, 20);
    }
}
