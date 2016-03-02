package com.sfh.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import com.sfh.util.ProcessOutput;


/**
 * A class for capturing whatever another process prints to its standard output
 * and standard error streams.
 * <p>
 * This class implements <code>Callable</code> and may therefore be run as a
 * background task using the facilities in <code>java.util.concurrent</code>.
 * <p>
 * Here is an example of synchronous use:
 * <pre><code>    Process process = startProcessSomehow();
 *    ProcessOutput output = (new ProcessCapture(process)).call();
 *    System.out.println("Output:\n" + output.getOutput());
 *    System.out.println("Errors:\n" + output.getErrors());</code></pre>
 * <p>
 * Here is an example of asynchronous (background) use:
 * <pre><code>    import java.util.concurrent.ExecutorService;
 *    import java.util.concurrent.Executors;
 *    import java.util.concurrent.Future;
 *    // ...
 *
 *    // Start process and submit the capturing task to an executor, to be
 *    // run in the background
 *    Process process = startProcessSomehow();
 *    ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
 *    Future&lt;ProcessOutput&gt; futureOutput =
 *        taskExecutor.submit(new ProcessCapture(process));
 *
 *    // Do other things while the process is running
 *
 *    // Wait for process to terminate and print its output.
 *    ProcessOutput output = futureOutput.get();
 *    System.out.println("Output:\n" + output.getOutput());
 *    System.out.println("Errors:\n" + output.getErrors());</code></pre>
 */
public class ProcessCapture implements Callable<ProcessOutput>
{
    /**
     * Constructs an object for capturing output from the given process.
     * <p>
     * The stream objects returned by <code>process.getInputStream()</code>
     * and <code>process.getErrorStream()</code> will be saved for use by
     * {@link #call}.  It is a very bad idea to acquire and use these streams
     * anywhere else in the program, as this object will most likely operate
     * on them in a different thread.
     * <p>
     * One of these streams may be null (indicating that the stream has been
     * redirected), but not both.
     *
     * @throws IOException
     *      If an I/O error occurred.
     */
    public ProcessCapture(Process process) throws IOException
    {
        stdout_ = process.getInputStream();
        stderr_ = process.getErrorStream();
        if (stdout_ == null && stderr_ == null) {
            throw new IOException("Both process streams are null");
        }
    }

    /**
     * Captures all output from the process' standard streams.
     * <p>
     * The method returns when both streams have been exhausted, which is
     * typically when the process has terminated.  The method 
     *
     * @return
     *      Whatever the monitored process printed to its standard output and
     *      standard error streams.
     *
     * @throws IOException
     *      If an I/O error occurred.
     * @throws IllegalStateException
     *      If the method is called more than once.
     */
    public ProcessOutput call() throws IOException
    {
        if (stdout_ == null && stderr_ == null) {
            throw new IllegalStateException("Multiple calls to call()");
        }
        try (
            InputStreamReader stdoutReader =
                stdout_ == null ? null : new InputStreamReader(stdout_);
            InputStreamReader stderrReader =
                stderr_ == null ? null : new InputStreamReader(stderr_)
        ) {
            stdout_ = null;
            stderr_ = null;
            StringBuilder output = new StringBuilder();
            StringBuilder errors = new StringBuilder();
            InputStreamReader nonemptyReader = null;
            StringBuilder nonemptyOutput = null;

            final int bufferLength = 1024;
            char[] buffer = new char[bufferLength];
            while (true) {
                if (stdoutReader != null) {
                    int oLen = stdoutReader.read(buffer, 0, bufferLength);
                    if (oLen < 0) {
                        nonemptyReader = stderrReader;
                        nonemptyOutput = errors;
                        break;
                    }
                    output.append(buffer, 0, oLen);
                }
                if (stderrReader != null) {
                    int eLen = stderrReader.read(buffer, 0, bufferLength);
                    if (eLen < 0) {
                        nonemptyReader = stdoutReader;
                        nonemptyOutput = output;
                        break;
                    }
                    errors.append(buffer, 0, eLen);
                }
            }
            // Exhaust the stream that has not reached EOF yet.
            if (nonemptyReader != null) {
                while (true) {
                    int len = nonemptyReader.read(buffer, 0, bufferLength);
                    if (len < 0) break;
                    nonemptyOutput.append(buffer, 0, len);
                }
            }
            return new ProcessOutput(output.toString(), errors.toString());
        }
    }

    // =========================================================================
    private InputStream stdout_;
    private InputStream stderr_;
}
