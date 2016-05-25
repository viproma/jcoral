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
import com.sfh.dsb.Variable;
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
        Map<String, Integer> sineVariableIDs = new HashMap<String, Integer>();
        for (VariableDescription varDesc : sine.getVariables()) {
            sineVariableIDs.put(varDesc.getName(), varDesc.getID());
        }

        // ...and do the same for "identity"
        DomainController.SlaveType ident = slaveTypes.get("sfh.larky.identity");
        Map<String, Integer> identVariableIDs = new HashMap<String, Integer>();
        for (VariableDescription varDesc : ident.getVariables()) {
            identVariableIDs.put(varDesc.getName(), varDesc.getID());
        }

        // Spawn a new execution on this domain, instantiate a slave, and add
        // it to the execution.
        try (
        ExecutionController exe = ExecutionController.spawnExecution(domLoc);
        SlaveLocator sineLoc = dom.instantiateSlave(sine, slaveInstantiationTimeout_ms);
        SlaveLocator identLoc = dom.instantiateSlave(ident, slaveInstantiationTimeout_ms);
        ) {
        Future.SlaveID fSineID = exe.addSlave(sineLoc, commandTimeout_ms);
        Future.SlaveID fIdentID = exe.addSlave(identLoc, commandTimeout_ms);

        SlaveID sineID = fSineID.get();
        SlaveID identID = fIdentID.get();

        // Set a few variables.
        List<VariableSetting> sineSetVarOps = new ArrayList<VariableSetting>();
        sineSetVarOps.add(new VariableSetting(sineVariableIDs.get("a"), new ScalarValue(2.0)));
        sineSetVarOps.add(new VariableSetting(sineVariableIDs.get("w"), new ScalarValue(2*Math.PI)));

        List<VariableSetting> identSetVarOps = new ArrayList<VariableSetting>();
        identSetVarOps.add(new VariableSetting(
            identVariableIDs.get("realIn"), new Variable(sineID, sineVariableIDs.get("y"))));
        identSetVarOps.add(new VariableSetting(
            identVariableIDs.get("integerIn"), new ScalarValue(123)));
        identSetVarOps.add(new VariableSetting(
            identVariableIDs.get("booleanIn"), new ScalarValue(true)));
        identSetVarOps.add(new VariableSetting(
            identVariableIDs.get("stringIn"), new ScalarValue("Hello World")));

        exe.setVariables(sineID, sineSetVarOps, commandTimeout_ms).get();
        exe.setVariables(identID, identSetVarOps, commandTimeout_ms).get();

        // Run simulation
        exe.endConfig();
        exe.simulate(
            endTime,
            stepSize,
            null,
            stepTimeout_ms,
            commandTimeout_ms,
            new SimulationProgressMonitor() {
                final int percentStep_ = 10;
                int nextPercent_ = 0;
                public boolean progress(double t) {
                    double percent = 100.0 * t / endTime;
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
