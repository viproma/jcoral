import java.io.File;
import java.util.ArrayList;
import java.util.List;

import no.viproma.coral.fmi.Importer;
import no.viproma.coral.fmi.FMU;
import no.viproma.coral.master.AddedSlave;
import no.viproma.coral.master.Execution;
import no.viproma.coral.model.SlaveID;
import no.viproma.coral.net.SlaveLocator;
import no.viproma.coral.slave.InProcessRunner;
import no.viproma.coral.slave.Instance;
import no.viproma.coral.slave.InstanceFactory;


public class InProcessExecutionTest
{
    private static class Slave extends Thread
    {
        public Slave(FMU fmu, String outputFilePrefix) throws Exception
        {
            try {
                baseSlave_ = fmu.instantiateSlave();
                loggingSlave_ = InstanceFactory.newCSVLoggingInstance(baseSlave_, outputFilePrefix);
                runner_ = new InProcessRunner(loggingSlave_);
                locator_ = runner_.getLocator();
            } catch (Exception e) {
                if (runner_ != null)        runner_.close();
                if (loggingSlave_ != null)  loggingSlave_.close();
                if (baseSlave_ != null)     baseSlave_.close();
                throw e;
            }
        }

        public SlaveLocator getLocator()
        {
            return locator_;
        }

        public void run()
        {
            try (
                // Acquire the resources to ensure they are released at the
                // end of this function.
                Instance baseSlave = baseSlave_;
                Instance loggingSlave = loggingSlave_;
                InProcessRunner runner = runner_;
            ) {
                // Make sure that this object is unusable after run() has
                // been called.
                runner_ = null;
                loggingSlave_ = null;
                baseSlave_ = null;

                // Run the slave
                runner.run();

            } catch (Exception e) {
                System.err.println("Error: Exception thrown in slave thread:");
                e.printStackTrace(System.err);
            }
        }

        private Instance baseSlave_;
        private Instance loggingSlave_;
        private InProcessRunner runner_;
        private SlaveLocator locator_;
    }


    public static void main(String[] args) throws Exception
    {
        final File testDataDir = new File(System.getenv("JCORAL_TEST_DATA_DIR"));
        final File testOutputDir = new File(System.getenv("JCORAL_TEST_OUTPUT_DIR"));

        final int NO_TIMEOUT = -1;
        try (
            // Import FMUs.
            Importer importer = new Importer();
            FMU fmu1 = importer.importFMU(new File(testDataDir, "identity.fmu"));
            FMU fmu2 = importer.importUnpackedFMU(new File(testDataDir, "sine_fmu_unpacked"));

            // Create execution.
            Execution execution = new Execution("InProcessExecutionTest");
        ) {
            testOutputDir.mkdirs();

            // Run each slave in its own background thread.
            Slave slave1 = new Slave(fmu1, testOutputDir + "/");
            slave1.start();

            Slave slave2 = new Slave(fmu2, testOutputDir + "/");
            slave2.start();

            // Add slaves.
            List<AddedSlave> slavesToAdd = new ArrayList<AddedSlave>();
            slavesToAdd.add(new AddedSlave(slave1.getLocator(), "slave1"));
            slavesToAdd.add(new AddedSlave(slave2.getLocator(), "slave2"));
            execution.addSlaves(slavesToAdd, NO_TIMEOUT);

            // Run simulation.
            execution.simulate(1.0, 0.1, NO_TIMEOUT, NO_TIMEOUT);
        }
    }
}
