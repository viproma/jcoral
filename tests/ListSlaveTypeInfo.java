import java.net.InetAddress;

import no.viproma.coral.master.ProviderCluster;
import no.viproma.coral.model.VariableDescription;


public class ListSlaveTypeInfo
{
    public static void main(String[] args) throws Exception
    {
        final int commandTimeout_ms = 1000;

        try (
            ProviderCluster cluster =
                new ProviderCluster(InetAddress.getByName("localhost"));
        ) {
            Thread.sleep(2000);
            for (ProviderCluster.SlaveType st :
                    cluster.getSlaveTypes(commandTimeout_ms)) {
                System.out.println(st.getName());
                System.out.println("  UUID     : " + st.getUUID());
                System.out.println("  Descr.   : " + st.getDescription());
                System.out.println("  Author   : " + st.getAuthor());
                System.out.println("  Version  : " + st.getVersion());
                System.out.println("  Variables:");
                for (VariableDescription vd : st.getVariables()) {
                    printVarDesc("    ", vd);
                }
                System.out.println("  Providers:");
                for (String p : st.getProviders()) {
                    System.out.println("    " + p);
                }
            }
        }
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
        System.out.println(vd.getCausality());
    }
}
