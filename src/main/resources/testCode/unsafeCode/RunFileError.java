import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        String userDir = System.getProperty("user.dir");
        String filePath = userDir + File.separator + "src/test/resources/木马.bat";
        Process exec = Runtime.getRuntime().exec(filePath);
        exec.waitFor();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(exec.getInputStream(), StandardCharsets.UTF_8));
        String outputLine;
        StringBuilder result = new StringBuilder();
        while ((outputLine = bufferedReader.readLine()) != null) {
            result.append("\n").append(outputLine);
        }
        System.out.println(result);
    }
}
