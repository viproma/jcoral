import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.viproma.coral.master.AddedSlave;
import no.viproma.coral.master.Execution;
import no.viproma.coral.master.ExecutionOptions;
import no.viproma.coral.master.ProviderCluster;
import no.viproma.coral.model.ScalarValue;
import no.viproma.coral.master.SimulationProgressMonitor;
import no.viproma.coral.master.SlaveConfig;
import no.viproma.coral.model.SlaveID;
import no.viproma.coral.net.SlaveLocator;
import no.viproma.coral.model.Variable;
import no.viproma.coral.model.VariableDescription;
import no.viproma.coral.model.VariableSetting;


public class ExecutionTest
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

        // Connect to slave providers
        try (
        ProviderCluster cluster =
            new ProviderCluster(InetAddress.getByName("localhost"));
        ) {
        Thread.sleep(2000); // Allow time to discover providers

        // Obtain list of slave types and store it in a map
        Map<String, ProviderCluster.SlaveType> slaveTypes =
            new HashMap<String, ProviderCluster.SlaveType>();
        for (ProviderCluster.SlaveType st :
                cluster.getSlaveTypes(commandTimeout_ms)) {
            slaveTypes.put(st.getSlaveTypeDescription().getName(), st);
        }

        // Make a list of variables for the "sine" model type
        ProviderCluster.SlaveType sine = slaveTypes.get("no.viproma.demo.sine");
        if (sine == null) {
            throw new Exception("Slave type 'no.viproma.demo.sine' not available");
        }
        Map<String, Integer> sineVariableIDs = new HashMap<String, Integer>();
        for (VariableDescription varDesc : sine.getSlaveTypeDescription().getVariables()) {
            sineVariableIDs.put(varDesc.getName(), varDesc.getID());
        }

        // ...and do the same for "identity"
        ProviderCluster.SlaveType ident = slaveTypes.get("no.viproma.demo.identity");
        if (ident == null) {
            throw new Exception("Slave type 'no.viproma.demo.identity' not available");
        }
        Map<String, Integer> identVariableIDs = new HashMap<String, Integer>();
        for (VariableDescription varDesc : ident.getSlaveTypeDescription().getVariables()) {
            identVariableIDs.put(varDesc.getName(), varDesc.getID());
        }

        // Create a new execution
        ExecutionOptions exeOptions = new ExecutionOptions();
        exeOptions.setSimTime(0.0, endTime);
        try (
        Execution exe = new Execution("ExecutionControllerTest", exeOptions);
        ) {

        // Spawn slaves and add them to the execution
        List<AddedSlave> slavesToAdd = new ArrayList<AddedSlave>();
        slavesToAdd.add(new AddedSlave(
            cluster.instantiateSlave(sine,  slaveInstantiationTimeout_ms),
            "sine"));
        slavesToAdd.add(new AddedSlave(
            cluster.instantiateSlave(ident, slaveInstantiationTimeout_ms),
            "ident"));
        exe.addSlaves(slavesToAdd, commandTimeout_ms);

        SlaveID sineID  = slavesToAdd.get(0).getID();
        SlaveID identID = slavesToAdd.get(1).getID();

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

        List<SlaveConfig> slaveConfigs = new ArrayList<SlaveConfig>();
        slaveConfigs.add(new SlaveConfig(sineID,  sineSetVarOps));
        slaveConfigs.add(new SlaveConfig(identID, identSetVarOps));
        exe.reconfigure(slaveConfigs, commandTimeout_ms);

        // Run simulation
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
