package life.light;

import java.io.File;
import static life.light.FileTool.deleteAllFiles;

public class Main {

    public static void main(String[] args) {
        createWorkDirectory();
    }

    public static void createWorkDirectory() {
        FileTool.createDirectory(Constant.TEMP);
        FileTool.createDirectory(Constant.TEMP + File.separator + Constant.TIFF);
        FileTool.createDirectory(Constant.TEMP + File.separator + Constant.TXT);
    }

    public static void emptyWorkDirectory() {
        deleteAllFiles(new File(Constant.TEMP));
        deleteAllFiles(new File(Constant.TEMP + File.separator + Constant.TIFF));
        deleteAllFiles(new File(Constant.TEMP + File.separator + Constant.TXT));
    }

}