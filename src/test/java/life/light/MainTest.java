package life.light;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.Files.list;
import static life.light.Main.createWorkDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @Test
    public void create_work_directory() {
        try {
            createWorkDirectory();
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

    @Test
    public void work_directory_is_empty() {
        try {
            Main.emptyWorkDirectory();
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

    @Test
    public void extractPDFToTIFF() {
        try {
            File pdf = new File("src" + File.separator + "test" + File.separator + "resources" + File.separator + "test.pdf");
            Main.extractPDFToTIFF(pdf);
            Path directoryPath = Paths.get(Constant.TEMP + File.separator + Constant.TIFF);
            assertTrue(Files.isDirectory(directoryPath), "Le dossier temp n'existe pas.");
            long fileCount = list(directoryPath).filter(Files::isRegularFile).count();
            assertEquals(1, fileCount, "Le répertoire temp devrait contenir un fichier.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void extractTIFFToTxt() {
        try {
            File pdf = new File("src" + File.separator + "test" + File.separator + "resources" + File.separator + "test.pdf");
            Main.extractPDFToTIFF(pdf);
            Main.extractTIFFToTXT("C:\\Program Files\\Tesseract-OCR\\tessdata");
            Path directoryPath = Paths.get(Constant.TEMP + File.separator + Constant.TXT);
            assertTrue(Files.isDirectory(directoryPath), "Le dossier txt n'existe pas.");
            long fileCount = list(directoryPath).filter(Files::isRegularFile).count();
            assertEquals(1, fileCount, "Le répertoire txt devrait contenir un fichier.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void concatenationTextFiles() {
        try {
            Main.concatenationTextFiles();
            Path directoryPath = Paths.get(Constant.TEMP);
            assertTrue(Files.isDirectory(directoryPath), "Le dossier temp n'existe pas.");
            long fileCount = list(directoryPath).filter(Files::isRegularFile).count();
            assertEquals(1, fileCount, "Le répertoire temp devrait contenir un fichier.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}