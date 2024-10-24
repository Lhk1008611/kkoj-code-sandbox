
public class Main {
    public static void main(String[] args)
    {
        try
        {
            long time = 60*60*1000;
            Thread.sleep(time);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }
}
