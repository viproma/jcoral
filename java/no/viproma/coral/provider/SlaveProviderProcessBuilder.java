/* Copyright 2014-2017, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.regex.Pattern;


/**
 * A convenience class for starting a slave provider process, similar to
 * {@link java.lang.ProcessBuilder} but tailored to Coral slave providers.
 * <p>
 * To use this class, simply create an instance, set the desired options,
 * and call {@link #start start()} to start the process.  The same object
 * may be used multiple times to start several processes.
 * <pre><code>    SlaveProviderProcessBuilder spb = new SlaveProviderProcessBuilder(
 *        new File("C:\\path\\to\\slave_provider.exe"),
 *        new File("C:\\path\\to\\working\\directory"));
 *    spb.addFMUPath(new File("C:\\path\\to\\mymodel.fmu"));
 *    spb.addFMUPath(new File("C:\\path\\to\\my\\fmu\\directory"));
 *    spb.setOutputDirectory(new File("C:\\path\\to\\outputfile\\directory"));
 *    Process sp = spb.start();</code></pre>
 */
public class SlaveProviderProcessBuilder
{
    /**
     * Constructor.
     *
     * @param executable
     *      The path to the slave provider executable.  If this is a relative
     *      path, it will be interpreted relative to the calling program's
     *      current working directory at the time {@link #start start()} is
     *      called.
     * @param workingDirectory
     *      The directory which will be used as the slave provider's working
     *      directory.  This does not necessarily have to be the directory
     *      in which the executable is located.  If this is a relative path,
     *      it will be interpreted relative to the calling program's current
     *      working directory at the time {@link #start start()} is called.
     *      This may be null, which will be equivalent to <code>"."</code>.
     *
     * @throws FileNotFoundException
     *      If <code>executable</code> does not refer to an executable file
     *      or <code>workingDirectory</code> does not refer to a directory.
     */
    public SlaveProviderProcessBuilder(File executable, File workingDirectory)
        throws FileNotFoundException
    {
        requireExecutable(executable);
        if (workingDirectory != null) requireDirectory(workingDirectory);
        executable_ = executable;
        workingDirectory_ = workingDirectory;
        fmus_ = new ArrayList<File>();
    }

    /** Forwards to {@link #SlaveProviderProcessBuilder(File, File)} with
     *  <code>workingDirectory = null</code>.
     */
    public SlaveProviderProcessBuilder(File executable)
        throws FileNotFoundException
    {
        this(executable, null);
    }

    /**
     * Adds a path to the slave provider's FMU search path list.
     * <p>
     * <code>fmuPath</code> may refer to a single FMU (a file), or a directory
     * which will be searched recursively for FMUs.
     *
     * @param fmuPath
     *      A path to a file or a directory.
     *      Use <code>null</code> to reset this option to its default value.
     *
     * @throws FileNotFoundException
     *      If <code>fmuPath</code> does not refer to an existing file or
     *      directory.
     */
    public void addFMUPath(File fmuPath) throws FileNotFoundException
    {
        if (!fmuPath.exists()) {
            throw new FileNotFoundException("FMU path not found: " + fmuPath.toString());
        }
        fmus_.add(fmuPath);
    }

    /**
     * Sets the network interface the slave provider will use for network
     * communications.
     *
     * @param networkInterface
     *      A string which contains the IP address or hostname associated with
     *      a local network interface, or the special value {@code "*"}
     *      (asterisk) which means <em>all</em> available interfaces.
     *      Use {@code null} to reset this option to its default value.
     */
    public void setNetworkInterface(String networkInterface)
    {
        networkInterface_ = networkInterface;
    }

    /**
     * Sets the UDP port number the slave provider will use to broadcast
     * information about itself on the network.
     * <p>
     * The master must listen on the same port, which can be specified with
     * the {@code discoveryEndpoint} parameter to the
     * {@link no.viproma.coral.master.ProviderCluster#ProviderCluster ProviderCluster constructor}.
     *
     * @param port
     *      A valid UDP port number, which must be in the range from 1
     *      through 65535, or 0 to reset this option to its default value.
     *
     * @throws IllegalArgumentException
     *      If {@code port} is outside the valid range.
     */
    public void setPort(int port) throws IllegalArgumentException
    {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException(
                "Invalid UDP port number: " + String.valueOf(port));
        }
        port_ = port;
    }

    /**
     * Sets the path to the slave executable.
     * <p>
     * If this is a relative path, it will be interpreted relative to the
     * working directory of the slave provider.
     *
     * @param file
     *      The path to an executable file.
     *      Use <code>null</code> to reset this option to its default value.
     *
     * @throws FileNotFoundException
     *      If <code>file</code> does not refer to an executable file.
     */
    public void setSlaveExecutable(File file) throws FileNotFoundException
    {
        if (file != null) {
            requireExecutable(relativeToWorkingDirectory(file));
        }
        slaveExecutable_ = file;
    }

    /**
     * Sets the directory to which output files will be written.
     * <p>
     * If this is a relative path, it will be interpreted relative to the
     * working directory of the slave provider.
     *
     * @param dir
     *      The path to a directory.
     *      Use <code>null</code> to reset this option to its default value.
     *
     * @throws FileNotFoundException
     *      If <code>dir</code> does not refer to an existing directory.
     */
    public void setOutputDirectory(File dir) throws FileNotFoundException
    {
        if (dir != null) {
            requireDirectory(relativeToWorkingDirectory(dir));
        }
        outputDirectory_ = dir;
    }

    /**
     * Sets the communications timeout (in seconds) for all slaves.
     * <p>
     * This is the number of seconds a newly spawned slave will wait for
     * an incoming connection from a master before giving up and shutting
     * down.
     *
     * @param seconds
     *      If positive, this is the timeout period in seconds.
     *      Use the special value 0 to reset the option to its default value,
     *      and -1 to disable the timeout altogether. Note that the latter
     *      could make the slave wait forever for incoming connections, perhaps
     *      using unnecessary system resources, and is therefore mainly
     *      intended for debugging.
     */
    public void setTimeout_s(int seconds)
    {
        timeout_s_ = seconds < 0 ? -1 : seconds;
    }

    /**
     * Starts a slave provider process.
     * <p>
     * This constructs a command line for the slave provider executable
     * based on the options which have been set previously, and starts
     * the program.
     * <p>
     * At least one FMU path must have been added with
     * {@link #addFMUPath addFMUPath()} before this method is called.
     *
     * @return
     *      An object which refers to the running process.
     *
     * @throws IOException
     *      If an I/O error occurs.
     * @throws IllegalStateException
     *      If this method is called before any FMU paths have been added.
     */
    public Process start() throws IOException
    {
        if (fmus_.isEmpty()) {
            throw new IllegalStateException("No FMU paths added");
        }
        ArrayList<String> args = new ArrayList<String>();
        args.add(executable_.getPath());
        for (File fmu : fmus_) {
            args.add(fmu.getPath());
        }
        if (networkInterface_ != null) {
            args.add("--interface");
            args.add(networkInterface_);
        }
        if (port_ != 0) {
            args.add("--port");
            args.add(String.valueOf(port_));
        }
        if (slaveExecutable_ != null) {
            args.add("--slave-exe");
            args.add(slaveExecutable_.getPath());
        }
        if (outputDirectory_ != null) {
            args.add("--output-dir");
            args.add(outputDirectory_.getPath());
        }
        if (timeout_s_ != 0) {
            args.add("--timeout");
            args.add(String.valueOf(timeout_s_));
        }
        ProcessBuilder pb = new ProcessBuilder(args);
        if (workingDirectory_ != null) {
            pb.directory(workingDirectory_);
        }
        return pb.start();
    }

    /**
     * Returns the path to the output file for a particular slave in a
     * particular execution.
     * <p>
     * This is a convenience method which constructs a path based on:
     * <ul>
     *  <li>The slave provider's working directory, and/or
     *  <li>The slave provider's output directory (if set), and
     *  <li>The name of the execution and the slave.
     * </ul>
     * <p>
     * The last two pieces of information must be supplied.  This requires that
     * they were set explicitly when the execution was started and the slave was
     * added &ndash; that is, by
     * {@link no.viproma.coral.master.Execution#Execution} and
     * {@link no.viproma.coral.master.Execution#addSlaves addSlaves()},
     * respectively.  If either of these are incorrect, the returned path
     * will also be incorrect.
     *
     * @param executionName
     *      The execution name.
     * @param slaveName
     *      The slave name.
     *
     * @throws IllegalArgumentException
     *      If either argument is an empty string.
     */
    public File getSlaveOutputFile(String executionName, String slaveName)
    {
        if (executionName.isEmpty()) {
            throw new IllegalArgumentException("Execution name is empty");
        }
        if (slaveName.isEmpty()) {
            throw new IllegalArgumentException("Slave name is empty");
        }
        return new File(
            relativeToWorkingDirectory(outputDirectory_),
            executionName + '_' + slaveName + ".csv");
    }


    // =========================================================================

    private File relativeToWorkingDirectory(File dir)
    {
        if (dir == null) {
            return workingDirectory_;
        } else {
            if (dir.isAbsolute()) {
                return dir;
            } else {
                return new File(workingDirectory_, dir.toString());
            }
        }
    }

    private static void requireExecutable(File file)
        throws FileNotFoundException
    {
        if (!file.canExecute()) {
            throw new FileNotFoundException("Not an executable file: " + file.toString());
        }
    }

    private static void requireDirectory(File file)
        throws FileNotFoundException
    {
        if (!file.isDirectory()) {
            throw new FileNotFoundException("Not a directory: " + file.toString());
        }
    }

    private File executable_;
    private File workingDirectory_;

    // Command-line arguments
    private ArrayList<File> fmus_;
    private String networkInterface_;
    private int port_ = 0;
    private File slaveExecutable_;
    private File outputDirectory_;
    private int timeout_s_ = 0;
}
