import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import no.viproma.coral.master.Execution;
import no.viproma.coral.master.ExecutionOptions;
import no.viproma.coral.master.EntityNotFoundException;
import no.viproma.coral.master.ModelBuilder;
import no.viproma.coral.master.ModelConstructionException;
import no.viproma.coral.master.ProviderCluster;
import no.viproma.coral.model.ScalarValue;
import no.viproma.coral.master.SimulationProgressMonitor;


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

        // Connect to slave providers
        try (
        ProviderCluster cluster =
            new ProviderCluster(InetAddress.getByName("localhost"));
        ) {
        Thread.sleep(2000); // Allow time to discover providers

        // Build the model
        ModelBuilder model = new ModelBuilder(cluster, commandTimeout_ms);
        model.addSlave("sine", "no.viproma.demo.sine");
        model.addSlave("id",   "no.viproma.demo.identity");
        model.setInitialVariableValue("sine", "a", new ScalarValue(2.0));
        model.setInitialVariableValue("sine", "w", new ScalarValue(2*Math.PI));
        model.setInitialVariableValue("id", "integerIn", new ScalarValue(123));
        model.setInitialVariableValue("id", "booleanIn", new ScalarValue(true));
        model.setInitialVariableValue("id", "stringIn", new ScalarValue("Hello World"));
        model.connectVariables("sine", "y", "id", "realIn");

        // Test that the methods above handle errors properly
        try { model.addSlave("foo:)", "no.viproma.demo.sine"); assert(false); } catch (IllegalArgumentException e) { }
        try { model.addSlave("_foo", "no.viproma.demo.sine"); assert(false); } catch (IllegalArgumentException e) { }
        try { model.addSlave("3foo", "no.viproma.demo.sine"); assert(false); } catch (IllegalArgumentException e) { }
        try { model.addSlave("foo", "someUnknownType"); assert(false); } catch (EntityNotFoundException e) { }
        try { model.addSlave("sine", "no.viproma.demo.sine"); assert(false); } catch (ModelConstructionException e) { }

        try { model.setInitialVariableValue("foo", "a", new ScalarValue(2.0)); assert(false); } catch (EntityNotFoundException e) { }
        try { model.setInitialVariableValue("id", "foo", new ScalarValue(2.0)); assert(false); } catch (EntityNotFoundException e) { }
        try { model.setInitialVariableValue("id", "realIn", new ScalarValue(123)); assert(false); } catch (ModelConstructionException e) { }

        try { model.connectVariables("Xsine", "y", "id", "realIn"); assert(false); } catch (EntityNotFoundException e) { }
        try { model.connectVariables("sine", "Xy", "id", "realIn"); assert(false); } catch (EntityNotFoundException e) { }
        try { model.connectVariables("sine", "y", "Xid", "realIn"); assert(false); } catch (EntityNotFoundException e) { }
        try { model.connectVariables("sine", "y", "id", "XrealIn"); assert(false); } catch (EntityNotFoundException e) { }

        try { model.connectVariables("sine", "x", "id", "realIn"); assert(false); }  catch (ModelConstructionException e) { }
        try { model.connectVariables("sine", "y", "id", "realOut"); assert(false); } catch (ModelConstructionException e) { }
        try { model.connectVariables("id", "realOut", "sine", "a"); assert(false); } catch (ModelConstructionException e) { }
        try { model.connectVariables("sine", "y", "id", "stringIn"); assert(false); } catch (ModelConstructionException e) { }

        // Test accessor methods
        TreeSet<String> slaveNames = new TreeSet<String>();
        for (String sn : model.getSlaveNames()) slaveNames.add(sn);
        assert (slaveNames.size() == 2);
        assert (slaveNames.first().equals("id"));
        assert (slaveNames.last().equals("sine"));

        assert (model.getSlaveTypeOf("id").getSlaveTypeDescription().getName().equals("no.viproma.demo.identity"));
        assert (model.getSlaveTypeOf("sine").getSlaveTypeDescription().getName().equals("no.viproma.demo.sine"));
        try { model.getSlaveTypeOf("foo"); assert(false); } catch (EntityNotFoundException e) { }

        assert (model.getInitialVariableValue("sine", "a").getRealValue() == 2.0);
        assert (model.getInitialVariableValue("id", "integerIn").getIntegerValue() == 123);
        assert (model.getInitialVariableValue("sine", "b") == null);
        try { model.getInitialVariableValue("foo", "a"); assert(false); } catch (EntityNotFoundException e) { }
        try { model.getInitialVariableValue("id", "foo"); assert(false); } catch (EntityNotFoundException e) { }

        List<ModelBuilder.Connection> allConnections = model.getConnections();
        assert(allConnections.size() == 1);
        assert(allConnections.get(0).getOutput().getSlaveName().equals("sine"));
        assert(allConnections.get(0).getOutput().getVariable().getName().equals("y"));
        assert(allConnections.get(0).getInput().getSlaveName().equals("id"));
        assert(allConnections.get(0).getInput().getVariable().getName().equals("realIn"));

        assert(model.getConnectionsTo("sine").isEmpty());
        List<ModelBuilder.Connection> idConnections = model.getConnectionsTo("id");
        assert(idConnections.size() == 1);
        assert(idConnections.get(0).getOutput().getSlaveName().equals("sine"));
        assert(idConnections.get(0).getOutput().getVariable().getName().equals("y"));
        assert(idConnections.get(0).getInput().getSlaveName().equals("id"));
        assert(idConnections.get(0).getInput().getVariable().getName().equals("realIn"));
        try { model.getConnectionsTo("foo"); assert(false); } catch (EntityNotFoundException e) { }

        HashSet<String> unconnected = new HashSet<String>();
        for (ModelBuilder.Variable v : model.getUnconnectedInputs()) {
            unconnected.add(v.getSlaveName() + "." + v.getVariable().getName());
        }
        assert(unconnected.size() == 4);
        assert(unconnected.contains("id.integerIn"));
        assert(unconnected.contains("id.booleanIn"));
        assert(unconnected.contains("id.stringIn"));
        assert(unconnected.contains("sine.x"));

        // Create a new execution and apply the model
        ExecutionOptions exeOptions = new ExecutionOptions();
        exeOptions.setSimTime(0.0, endTime);
        try (Execution exe = new Execution("ModelBuilderTest", exeOptions)) {
        model.apply(exe, slaveInstantiationTimeout_ms, commandTimeout_ms);

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
