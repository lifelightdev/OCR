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

}
