import java.util.Iterator;
import java.util.Set;
import com.sfh.dsb.DomainController;
import com.sfh.dsb.DomainLocator;
import com.sfh.dsb.VariableDescription;


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
                System.out.print("  Variables:");
                Iterator<VariableDescription> variables = st.getVariables();
                while (variables.hasNext()) {
                    System.out.print("    ");
                    printVarDesc(variables.next());
                    System.out.println();
                }
                System.out.println("  Providers:");
                Iterator<String> providers = st.getProviders();
                while (providers.hasNext()) System.out.println("    " + providers.next() + "; ");
            }
        }}
    }

    public static void printVarDesc(VariableDescription vd)
    {
        System.out.print(vd.getID());
        System.out.print(" ");
        System.out.print(vd.getName());
        System.out.print(" ");
        System.out.print(vd.getDataType());
        System.out.print(" ");
        System.out.print(vd.getVariability());
        System.out.print(" ");
        System.out.print(vd.getCausality());
    }
}
