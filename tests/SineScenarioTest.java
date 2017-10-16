import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.SortedMap;

import no.viproma.coral.master.Execution;
import no.viproma.coral.master.ExecutionOptions;
import no.viproma.coral.master.ModelBuilder;
import no.viproma.coral.master.ModelSlaveMap;
import no.viproma.coral.master.ProviderCluster;
import no.viproma.coral.master.ScenarioBuilder;
import no.viproma.coral.master.ScenarioEvent;
import no.viproma.coral.master.SimulationProgressMonitor;
import no.viproma.coral.model.ScalarValue;


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
        ProviderCluster cluster =
            new ProviderCluster(InetAddress.getByName("localhost"));
        ) {
        Thread.sleep(2000); // wait for the info to trickle in

        // Build the model
        ModelBuilder model = new ModelBuilder(cluster, commandTimeout_ms);
        model.addSlave("sine1", "no.viproma.demo.sine");
        model.addSlave("sine2", "no.viproma.demo.sine");
        model.setInitialVariableValue("sine1", "b", new ScalarValue(2.0));
        model.setInitialVariableValue("sine1", "w", new ScalarValue(2*Math.PI));
        model.setInitialVariableValue("sine2", "b", new ScalarValue(4.0));
        model.setInitialVariableValue("sine2", "w", new ScalarValue(4*Math.PI));

        // Define a scenario "blueprint"
        ScenarioBuilder scenarioBuilder = new ScenarioBuilder();
        scenarioBuilder.addEvent(6.0, "sine2", "w", new ScalarValue(2*Math.PI));
        scenarioBuilder.addEvent(3.0, "sine1", "b", new ScalarValue(3.0));

        // Test different retrieval methods
        ScenarioBuilder.Event event = scenarioBuilder.getEvents().iterator().next();
        assert(event.getTimePoint() == 6.0);
        assert(event.getSlaveName().equals("sine2"));
        assert(event.getVariableName().equals("w"));
        assert(event.getNewValue().getRealValue() == 2*Math.PI);

        SortedMap<Double, List<ScenarioBuilder.Event>> eventsByTime =
            scenarioBuilder.getEventsByTime();
        assert(eventsByTime.size() == 2);
        assert(eventsByTime.get(3.0).size() == 1);
        assert(eventsByTime.get(3.0).get(0).getSlaveName().equals("sine1"));

        // Create a new execution and apply the model
        ExecutionOptions exeOptions = new ExecutionOptions();
        exeOptions.setSimTime(0.0, endTime);
        try (Execution exe = new Execution("SineScenarioTest", exeOptions)) {
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
