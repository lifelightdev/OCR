package life.light;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

public class FileToolTest {
    @Test
    public void create_directory_if_not_exist() {
        // delete directory if exist
        File test = new File("test");
        test.delete();
        Boolean result = FileTool.createDirectory("test");
        Assertions.assertTrue(result);
    }

    @Test
    public void delete_all_files_in_directory() {
        FileTool.createDirectory("test");
        new File("test"+File.separator+"file1");
        Boolean result = FileTool.deleteAllFiles("test");
        Assertions.assertTrue(result);
    }

}
