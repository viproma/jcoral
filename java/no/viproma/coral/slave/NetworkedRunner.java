/* Copyright 2018-2018, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral.slave;

import java.net.InetAddress;
import no.viproma.coral.Logging;
import no.viproma.coral.net.SlaveLocator;
import no.viproma.coral.slave.Instance;


/**
 *  A class for running a slave instance, when the slave will communicate with
 *  the master over TCP/IP.
 *  <p>
 *  This class is responsible for setting up the slave's part of the
 *  communication channel.  To obtain a slave locator that can be passed to
 *  the {@link no.viproma.coral.master.Execution} class, i.e. the master, use
 *  {@link #getLocator}.
 *  <p>
 *  Objects of this class should always be disposed of with {@link #close} when
 *  they are no longer needed, to avoid resource leaks in the underlying native
 *  code. (A nice, automated way to do this is to use
 *  <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">the
 *  try-with-resources statement</a>.)
 */
public class NetworkedRunner implements AutoCloseable
{
    static
    {
        System.loadLibrary("jcoral");
    }

    /**
     *  Constructor.
     *
     *  @param slaveInstance
     *      The slave instance to run.
     *  @param bindAddress
     *      The local network interface to use for network communication.
     *      The special address "0.0.0.0" may be used to listen on all
     *      interfaces.
     *  @param controlPort
     *      The port number to use for master-slave communication. Use zero
     *      to let the operating system choose an available port number.
     *  @param dataPubPort
     *      The port number to use for slave-slave communication. Use zero
     *      to let the operating system choose an available port number.
     *  @param commTimeout_s
     *      A number of seconds after which the slave will shut itself down if
     *      no master has yet connected.  The special value -1 means "never".
     */
    public NetworkedRunner(
        Instance slaveInstance,
        InetAddress bindAddress,
        int controlPort,
        int dataPubPort,
        int commTimeout_s)
        throws Exception
    {
        nativePtr_ = createNative(
            slaveInstance.getNativeInstancePtr(),
            bindAddress.getHostAddress(),
            controlPort,
            dataPubPort,
            commTimeout_s);
    }

    /**
     *  Convenience constructor.
     *
     *  Forwards to the main constructor with both port numbers set to zero.
     */
    public NetworkedRunner(
        Instance slaveInstance,
        InetAddress bindAddress,
        int commTimeout_s)
        throws Exception
    {
        this(slaveInstance, bindAddress, 0, 0, commTimeout_s);
    }

    @Override
    protected void finalize()
    {
        if (nativePtr_ != 0) Logging.logNotClosedOnFinalization(getClass());
    }

    /**
     *  Releases native resources (such as memory) associated with this object.
     *  <p>
     *  After this function has been called, any attempt to use the object
     *  will fail.
     */
    @Override
    public void close()
    {
        if (nativePtr_ != 0) {
            destroyNative(nativePtr_);
            nativePtr_ = 0;
        }
    }

    /**
     *  Returns the slave locator.
     *  <p>
     *  The returned object describes how the master should connect to the
     *  slave, and can be passed to {@link no.viproma.coral.master.AddedSlave#AddedSlave}.
     */
    public SlaveLocator getLocator() throws Exception
    {
        return getLocatorNative(nativePtr_);
    }

    /**
     *  Runs the slave instance.
     *  <p>
     *  This function does not return before the execution has completed.
     */
    public void run() throws Exception
    {
        runNative(nativePtr_);
    }

    private static native long createNative(
        long nativeInstancePtr, String bindAddress,
        int controlPort, int dataPubPort, int commTimeout_s)
        throws Exception;
    private static native void destroyNative(long selfPtr);
    private static native SlaveLocator getLocatorNative(
        long selfPtr)
        throws Exception;
    private static native void runNative(long selfPtr) throws Exception;

    private long nativePtr_ = 0;
}
