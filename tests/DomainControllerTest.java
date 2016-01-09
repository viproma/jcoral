import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.sfh.dsb.DomainController;
import com.sfh.dsb.DomainLocator;
import com.sfh.dsb.SlaveLocator;
import com.sfh.dsb.VariableDescription;


public class DomainControllerTest
{
    public static void main(String[] args) throws Exception
    {
        try (DomainLocator domLoc = new DomainLocator("tcp://localhost")) {
        try (DomainController dom = new DomainController(domLoc)) {
            Thread.sleep(2000);
            Set<DomainController.SlaveType> slaveTypeSet = dom.getSlaveTypes();
            System.out.format("Number of slave types: %d\n", slaveTypeSet.size());
            Map<String, DomainController.SlaveType> slaveTypes =
                new HashMap<String, DomainController.SlaveType>();
            for (DomainController.SlaveType st : slaveTypeSet) {
                slaveTypes.put(st.getName(), st);
                System.out.println(st.getName());
                System.out.println("  " + st.getUUID());
                System.out.println("  " + st.getDescription());
                System.out.println("  " + st.getAuthor());
                System.out.println("  " + st.getVersion());
                System.out.println("  Variables:");
                for (VariableDescription vd : st.getVariables()) {
                    printVarDesc("    ", vd);
                }
                System.out.println("  Providers:");
                for (String p : st.getProviders()) {
                    System.out.println("    " + p);
                }
            }

            SlaveLocator slaveLoc = dom.instantiateSlave(
                slaveTypes.get("sfh.larky.identity"),
                2000 /*ms*/);
            slaveLoc.close();
        }}
    }

    public static void printVarDesc(String prefix, VariableDescription vd)
    {
        System.out.print(prefix);
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
