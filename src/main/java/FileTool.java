import java.io.File;

public class FileTool {
    public static Boolean createDirectory(String temp) {
        return new File(temp).mkdirs();
    }
}
