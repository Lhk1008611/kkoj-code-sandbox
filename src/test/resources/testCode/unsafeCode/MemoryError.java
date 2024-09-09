import java.util.List;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args)
    {
        List<Byte[]> list = new ArrayList<>();
        while (true)
        {
            list.add(new Byte[1024*1024]);
        }
    }
}
