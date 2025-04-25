import java.io.File;

public class FileTool {
    public static Boolean createDirectory(String directory) {
        return new File(directory).mkdirs();
    }

    public static Boolean deleteAllFiles(String directory) {
        return false;
    }
}
