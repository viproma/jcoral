import java.net.InetAddress;

import no.viproma.coral.master.ProviderCluster;
import no.viproma.coral.model.SlaveTypeDescription;
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
                SlaveTypeDescription std = st.getSlaveTypeDescription();
                System.out.println(std.getName());
                System.out.println("  UUID     : " + std.getUUID());
                System.out.println("  Descr.   : " + std.getDescription());
                System.out.println("  Author   : " + std.getAuthor());
                System.out.println("  Version  : " + std.getVersion());
                System.out.println("  Variables:");
                for (VariableDescription vd : std.getVariables()) {
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
