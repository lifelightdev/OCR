package life.light;

import java.io.File;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        createWorkDirectory();
    }

    public static void createWorkDirectory() {
        FileTool.createDirectory(Constant.TEMP);
        FileTool.createDirectory(Constant.TEMP + File.separator + Constant.TIFF);
        FileTool.createDirectory(Constant.TEMP + File.separator + Constant.TXT);
    }
}