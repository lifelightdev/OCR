package life.light;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.Files.list;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    public void create_work_directory() {
        try {
            Main.createWorkDirectory();
            Path directoryPath = Paths.get(Constant.TEMP);
            assertTrue(Files.isDirectory(directoryPath), "Le dossier temp n'existe pas.");
            long fileCount = list(directoryPath).filter(Files::isRegularFile).count();
            assertEquals(0, fileCount, "Le répertoire temp ne devrait pas contenir de fichier.");
            directoryPath = Paths.get(Constant.TEMP + File.separator + Constant.TIFF);
            assertTrue(Files.isDirectory(directoryPath), "Le dossier d'image TIFF n'existe pas.");
            assertTrue(list(directoryPath).findAny().isEmpty(), "Le dossier d'image TIFF n'est pas vide.");
            directoryPath = Paths.get(Constant.TEMP + File.separator + Constant.TXT);
            assertTrue(Files.isDirectory(directoryPath), "Le dossier texte n'existe pas.");
            assertTrue(list(directoryPath).findAny().isEmpty(), "Le dossier texte n'est pas vide.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}