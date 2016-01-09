import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sfh.dsb.DomainController;
import com.sfh.dsb.DomainLocator;
import com.sfh.dsb.ExecutionController;
import com.sfh.dsb.Future;
import com.sfh.dsb.ScalarValue;
import com.sfh.dsb.SimulationProgressMonitor;
import com.sfh.dsb.SlaveID;
import com.sfh.dsb.SlaveLocator;
import com.sfh.dsb.VariableDescription;
import com.sfh.dsb.VariableSetting;


public class ExecutionControllerTest
{
    public static void main(String[] args) throws Exception
    {
        // Define misc. timeouts
        final int slaveInstantiationTimeout_ms = 30*1000;
        final int commandTimeout_ms = 1000;
        final int stepTimeout_ms = 2000; // Step commands typically take longer

        // ...and other quantities
        final double stepSize = 0.01;
        final double endTime = 1.0;

        // Connect to domain
        try (
        DomainLocator domLoc = new DomainLocator("tcp://localhost");
        DomainController dom = new DomainController(domLoc)
        ) {
        Thread.sleep(2000); // wait for the info to trickle in

        // Obtain list of slave types and store it in a map
        Map<String, DomainController.SlaveType> slaveTypes =
            new HashMap<String, DomainController.SlaveType>();
        for (DomainController.SlaveType st : dom.getSlaveTypes()) {
            slaveTypes.put(st.getName(), st);
        }

        // Make a list of variables for the "sine" model type
        DomainController.SlaveType sine = slaveTypes.get("sfh.larky.sine");
        Map<String, Integer> variableIDs = new HashMap<String, Integer>();
        for (VariableDescription varDesc : sine.getVariables()) {
            variableIDs.put(varDesc.getName(), varDesc.getID());
        }

        // Spawn a new execution on this domain, instantiate a slave, and add
        // it to the execution.
        try (
        ExecutionController exe = ExecutionController.spawnExecution(domLoc);
        SlaveLocator slaveLoc = dom.instantiateSlave(sine, slaveInstantiationTimeout_ms);
        ) {
        SlaveID slaveID = exe.addSlave(slaveLoc, commandTimeout_ms).get();

        // Set a few variables.
        List<VariableSetting> setVarOps = new ArrayList<VariableSetting>();
        setVarOps.add(new VariableSetting(variableIDs.get("a"), new ScalarValue(2.0)));
        setVarOps.add(new VariableSetting(variableIDs.get("w"), new ScalarValue(2*Math.PI)));
        exe.setVariables(slaveID, setVarOps, commandTimeout_ms).get();

        // Run simulation
        exe.endConfig();
        exe.simulate(
            endTime,
            stepSize,
            stepTimeout_ms,
            commandTimeout_ms,
            new SimulationProgressMonitor() {
                final int percentStep_ = 10;
                int nextPercent_ = 0;
                public boolean progress(double t, double p) {
                    double percent = p * 100;
                    if (percent >= nextPercent_) {
                        System.out.println("t = " + t + " (" + nextPercent_ + "%)");
                        nextPercent_ += percentStep_;
                    }
                    return true;
                }
            });

        // Close the try-with-resources statements we've opened above
        }}
    }
}
