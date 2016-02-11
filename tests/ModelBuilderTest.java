import com.sfh.dsb.DomainController;
import com.sfh.dsb.DomainLocator;
import com.sfh.dsb.ExecutionController;
import com.sfh.dsb.ModelBuilder;
import com.sfh.dsb.ScalarValue;
import com.sfh.dsb.SimulationProgressMonitor;


public class ModelBuilderTest
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

        // Build the model
        ModelBuilder model = new ModelBuilder(dom);
        model.addSlave("sine", "sfh.larky.sine");
        model.addSlave("id",   "sfh.larky.identity");
        model.setInitialVariableValue("sine", "a", new ScalarValue(2.0));
        model.setInitialVariableValue("sine", "w", new ScalarValue(2*Math.PI));
        model.setInitialVariableValue("id", "integerIn", new ScalarValue(123));
        model.setInitialVariableValue("id", "booleanIn", new ScalarValue(true));
        model.setInitialVariableValue("id", "stringIn", new ScalarValue("Hello World"));
        model.connectVariables("sine", "y", "id", "realIn");

        // Spawn a new execution on this domain and apply the model
        try (ExecutionController exe = ExecutionController.spawnExecution(domLoc)) {
        model.apply(exe, slaveInstantiationTimeout_ms, commandTimeout_ms);

        // Run simulation
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
