import java.util.Iterator;
import java.util.Set;
import com.sfh.dsb.DomainController;
import com.sfh.dsb.DomainLocator;


public class DomainControllerTest
{
    public static void main(String[] args) throws Exception
    {
        try (DomainLocator domLoc = new DomainLocator("tcp://localhost")) {
        try (DomainController dom = new DomainController(domLoc)) {
            Thread.sleep(2000);
            Set<DomainController.SlaveType> slaveTypes = dom.getSlaveTypes();
            System.out.format("Number of slave types: %d\n", slaveTypes.size());
            for (DomainController.SlaveType st : slaveTypes) {
                System.out.println(st.getName());
                System.out.println("  " + st.getUUID());
                System.out.println("  " + st.getDescription());
                System.out.println("  " + st.getAuthor());
                System.out.println("  " + st.getVersion());
                System.out.print("  ");
                Iterator<String> providers = st.getProviders();
                while (providers.hasNext()) System.out.print(providers.next() + "; ");
                System.out.println();
            }
        }}
    }
}
