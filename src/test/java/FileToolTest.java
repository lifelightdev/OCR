import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FileToolTest {
    @Test
    public void create_directory_if_not_existTest() {
        Boolean result = FileTool.createDirectory("temp");
        Assertions.assertTrue(result);
    }
}
