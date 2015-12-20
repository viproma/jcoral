import com.sfh.dsb.DomainLocator;

public class DomainLocatorErrorTest
{
    public static void main(String[] args) throws Exception
    {
        try {
            new DomainLocator(null);
            System.exit(1);
        } catch (NullPointerException e) { }

        try {
            new DomainLocator("");
            System.exit(1);
        } catch (Exception e) { }
    }
}
