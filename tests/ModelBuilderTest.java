import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import com.sfh.dsb.DomainController;
import com.sfh.dsb.DomainLocator;
import com.sfh.dsb.ExecutionController;
import com.sfh.dsb.EntityNotFoundException;
import com.sfh.dsb.ModelBuilder;
import com.sfh.dsb.ModelConstructionException;
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

        // Test that the methods above handle errors properly
        try { model.addSlave("foo:)", "sfh.larky.sine"); assert(false); } catch (IllegalArgumentException e) { }
        try { model.addSlave("_foo", "sfh.larky.sine"); assert(false); } catch (IllegalArgumentException e) { }
        try { model.addSlave("3foo", "sfh.larky.sine"); assert(false); } catch (IllegalArgumentException e) { }
        try { model.addSlave("foo", "someUnknownType"); assert(false); } catch (EntityNotFoundException e) { }
        try { model.addSlave("sine", "sfh.larky.sine"); assert(false); } catch (ModelConstructionException e) { }

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

        assert (model.getSlaveTypeOf("id").getName().equals("sfh.larky.identity"));
        assert (model.getSlaveTypeOf("sine").getName().equals("sfh.larky.sine"));
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
