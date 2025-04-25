package life.light;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class FileTool {

    private static final Logger LOGGER = LogManager.getLogger();

    public static Boolean createDirectory(String directory) {
        return new File(directory).mkdirs();
    }

    public static Boolean deleteAllFiles(String directory) {
        File pathDirectory = new File(directory);
        File[] files = pathDirectory.listFiles();
        if (null != files) {
            for (File fichier : files) {
                if (fichier.isFile()) {
                    boolean delete = fichier.delete();
                    if (!delete) {
                        LOGGER.info("Le fichier {} n'a pas été supprimé.", fichier.getName());
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static void deleteAllFiles(File directory) {
        File[] files = directory.listFiles();
        if (null != files) {
            for (File file : files) {
                if (file.isFile()) {
                    boolean delete = file.delete();
                    if (!delete) {
                        LOGGER.info("Le fichier {} n'a pas été supprimé.", file.getName());
                    }
                }
            }
        }
    }
}
