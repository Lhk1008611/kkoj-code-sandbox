
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws IOException {
        String userDir = System.getProperty("user.dir");
        String filePath = userDir + File.separator + "src/test/resources/木马.bat";
        String dangerousCode = "java -version 2>&1";
        Files.write(Paths.get(filePath), Arrays.asList(dangerousCode));
        System.out.println("危险程序植入成功");
    }
}
