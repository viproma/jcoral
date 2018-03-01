import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import no.viproma.coral.fmi.Importer;
import no.viproma.coral.fmi.FMU;
import no.viproma.coral.master.AddedSlave;
import no.viproma.coral.master.Execution;
import no.viproma.coral.model.SlaveID;
import no.viproma.coral.net.SlaveLocator;
import no.viproma.coral.slave.Instance;
import no.viproma.coral.slave.InstanceFactory;
import no.viproma.coral.slave.NetworkedRunner;


public class NetworkedExecutionTest
{
    private static class Slave extends Thread
    {
        public Slave(FMU fmu, String outputFilePrefix, int commTimeout_s) throws Exception
        {
            // baseSlave will be closed by InstanceFactory.newCSVLoggingInstance()
            // (unless it throws, which is why we call close() in the exception
            // handler just to be safe).
            Instance baseSlave = fmu.instantiateSlave();
            try {
                slave_ = InstanceFactory.newCSVLoggingInstance(baseSlave, outputFilePrefix);
                runner_ = new NetworkedRunner(
                    slave_,
                    InetAddress.getByName("127.0.0.1"),
                    commTimeout_s);
                locator_ = runner_.getLocator();
            } catch (Exception e) {
                if (runner_ != null)    runner_.close();
                if (slave_ != null)     slave_.close();
                baseSlave.close();
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
                Instance slave = slave_;
                NetworkedRunner runner = runner_;
            ) {
                // Make sure that this object is unusable after run() has
                // been called.
                runner_ = null;
                slave_ = null;

                // Run the slave
                runner.run();

            } catch (Exception e) {
                System.err.println("Error: Exception thrown in slave thread:");
                e.printStackTrace(System.err);
            }
        }

        private Instance slave_;
        private NetworkedRunner runner_;
        private SlaveLocator locator_;
    }


    public static void main(String[] args) throws Exception
    {
        final File testDataDir = new File(System.getenv("JCORAL_TEST_DATA_DIR"));
        final File testOutputDir = new File(System.getenv("JCORAL_TEST_OUTPUT_DIR"));

        final int slaveCommTimeout_s = 60;
        final int commandTimeout_ms = 1000;
        final int stepTimeout_ms = 1000;
        try (
            // Import FMUs.
            Importer importer = new Importer();
            FMU fmu1 = importer.importFMU(new File(testDataDir, "identity.fmu"));
            FMU fmu2 = importer.importUnpackedFMU(new File(testDataDir, "sine_fmu_unpacked"));

            // Create execution.
            Execution execution = new Execution("NetworkedExecutionTest");
        ) {
            testOutputDir.mkdirs();

            // Run each slave in its own background thread.
            Slave slave1 = new Slave(fmu1, testOutputDir + "/", slaveCommTimeout_s);
            slave1.start();

            Slave slave2 = new Slave(fmu2, testOutputDir + "/", slaveCommTimeout_s);
            slave2.start();

            // Add slaves.
            List<AddedSlave> slavesToAdd = new ArrayList<AddedSlave>();
            slavesToAdd.add(new AddedSlave(slave1.getLocator(), "slave1"));
            slavesToAdd.add(new AddedSlave(slave2.getLocator(), "slave2"));
            execution.addSlaves(slavesToAdd, commandTimeout_ms);

            // Run simulation.
            execution.simulate(1.0, 0.1, stepTimeout_ms, commandTimeout_ms);
        }
    }
}
