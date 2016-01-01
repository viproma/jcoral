import java.util.HashMap;
import java.util.Map;
import com.sfh.dsb.DomainController;
import com.sfh.dsb.DomainLocator;
import com.sfh.dsb.ExecutionController;
import com.sfh.dsb.Future;
import com.sfh.dsb.SlaveID;
import com.sfh.dsb.SlaveLocator;


public class ExecutionControllerTest
{
    public static void main(String[] args) throws Exception
    {
        // Connect to domain and obtain list of slave types
        try (DomainLocator domLoc = new DomainLocator("tcp://localhost")) {
        try (DomainController dom = new DomainController(domLoc)) {
        Thread.sleep(2000); // wait for the info to trickle in
        Map<String, DomainController.SlaveType> slaveTypes =
            new HashMap<String, DomainController.SlaveType>();
        for (DomainController.SlaveType st : dom.getSlaveTypes()) {
            slaveTypes.put(st.getName(), st);
        }

        // Spawn a new execution on this domain
        try (ExecutionController exe = ExecutionController.spawnExecution(domLoc)) {
        exe.endConfig();
        exe.beginConfig();
        exe.setSimulationTime(200.0);

        // Instantiate a slave
        try (SlaveLocator slaveLoc = dom.instantiateSlave(
                slaveTypes.get("sfh.larky.identity"),
                2000 /*ms*/)) {
        Future.SlaveID sid = exe.addSlave(slaveLoc, 5000 /*ms*/);
        sid.waitForResult();
        if (!sid.waitForResult(1000)) throw new Exception();
        sid.get();
        }}}}
    }
}
