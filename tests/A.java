import com.sfh.dsb.DomainController;
import com.sfh.dsb.DomainLocator;
import com.sfh.dsb.SlaveLocator;


public class A
{
    public static void main(String[] args)
    {
        DomainLocator dLoc = new DomainLocator("tcp://localhost");
        dLoc.close();
    }
}
