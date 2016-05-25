import java.util.LinkedList;
import java.util.Queue;

import com.sfh.dsb.DomainController;
import com.sfh.dsb.DomainLocator;
import com.sfh.dsb.ExecutionController;
import com.sfh.dsb.ModelBuilder;
import com.sfh.dsb.ModelSlaveMap;
import com.sfh.dsb.ScalarValue;
import com.sfh.dsb.ScenarioBuilder;
import com.sfh.dsb.ScenarioEvent;
import com.sfh.dsb.SimulationProgressMonitor;


public class SineScenarioTest
{
    public static void main(String[] args) throws Exception
    {
        // Define misc. timeouts
        final int slaveInstantiationTimeout_ms = 30*1000;
        final int commandTimeout_ms = 1000;
        final int stepTimeout_ms = 2000; // Step commands typically take longer

        // ...and other quantities
        final double stepSize = 0.01;
        final double endTime = 9.0;

        // Connect to domain
        try (
        DomainLocator domLoc = new DomainLocator("tcp://localhost");
        DomainController dom = new DomainController(domLoc)
        ) {
        Thread.sleep(2000); // wait for the info to trickle in

        // Build the model
        ModelBuilder model = new ModelBuilder(dom);
        model.addSlave("sine1", "sfh.larky.sine");
        model.addSlave("sine2", "sfh.larky.sine");
        model.setInitialVariableValue("sine1", "b", new ScalarValue(2.0));
        model.setInitialVariableValue("sine1", "w", new ScalarValue(2*Math.PI));
        model.setInitialVariableValue("sine2", "b", new ScalarValue(4.0));
        model.setInitialVariableValue("sine2", "w", new ScalarValue(4*Math.PI));

        // Define a scenario "blueprint"
        ScenarioBuilder scenarioBuilder = new ScenarioBuilder();
        scenarioBuilder.addEvent(6.0, "sine2", "w", new ScalarValue(2*Math.PI));
        scenarioBuilder.addEvent(3.0, "sine1", "b", new ScalarValue(3.0));

        // Spawn a new execution on this domain and apply the model
        try (ExecutionController exe = ExecutionController.spawnExecution(domLoc)) {
        ModelSlaveMap slaveMap =
            model.apply(exe, slaveInstantiationTimeout_ms, commandTimeout_ms);

        // Generate a scenario for this execution
        Queue<ScenarioEvent> scenario = scenarioBuilder.build(slaveMap);
        assert scenario.element().getTimePoint() == 3.0;

        // Run simulation
        exe.simulate(
            endTime,
            stepSize,
            scenario,
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
