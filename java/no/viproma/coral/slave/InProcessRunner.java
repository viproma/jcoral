/* Copyright 2018-2018, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral.slave;

import no.viproma.coral.Logging;
import no.viproma.coral.net.SlaveLocator;
import no.viproma.coral.slave.Instance;


/**
 *  A class for running a slave instance, when the slave will run in the
 *  same process as the master.
 *  <p>
 *  If a slave is to run within the same process as the master, they need to
 *  run in separate threads.  This class sets up the communication channel
 *  between them.  To obtain a slave locator that can be passed to the
 *  {@link no.viproma.coral.master.Execution} class, i.e. the master,
 *  use {@link #getLocator}.
 *  <p>
 *  Note that it is up to client code to manage the threads; this does not
 *  happen automatically.  The central issue is that {@link #run} must be
 *  called in a different thread from the master, as it does not return before
 *  the execution is complete.
 *  <p>
 *  <strong>Warning:</strong>
 *  This class is not threadsafe.  Specifically, do not call methods
 *  simultaneously from the "master thread" and the "slave thread".
 *  <p>
 *  Objects of this class should always be disposed of with {@link #close} when
 *  they are no longer needed, to avoid resource leaks in the underlying native
 *  code. (A nice, automated way to do this is to use
 *  <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">the
 *  try-with-resources statement</a>.)
 */
public class InProcessRunner implements AutoCloseable
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
     */
    public InProcessRunner(Instance slaveInstance)
        throws Exception
    {
        nativePtr_ =
            createNative(slaveInstance.getNativeInstancePtr());
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

    private static native long createNative(long nativeInstancePtr)
        throws Exception;
    private static native void destroyNative(long selfPtr);
    private static native SlaveLocator getLocatorNative(
        long selfPtr)
        throws Exception;
    private static native void runNative(long selfPtr) throws Exception;

    private long nativePtr_ = 0;
}
