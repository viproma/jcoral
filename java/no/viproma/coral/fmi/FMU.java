/* Copyright 2018-2018, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral.fmi;

import no.viproma.coral.Logging;
import no.viproma.coral.model.SlaveTypeDescription;
import no.viproma.coral.slave.Instance;
import no.viproma.coral.slave.OpaqueInstance;


/**
 *  A class that represents an imported FMU.
 *  <p>
 *  Use {@link Importer#importFMU} or {@link Importer#importUnpackedFMU} to
 *  import FMUs.
 *  <p>
 *  Objects of this class should always be disposed of with {@link #close} when
 *  they are no longer needed, to avoid resource leaks in the underlying native
 *  code. (A nice, automated way to do this is to use
 *  <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">the
 *  try-with-resources statement</a>.)
 */
public class FMU implements AutoCloseable
{
    static
    {
        System.loadLibrary("jcoral");
    }

    FMU(long nativePtr)
    {
        nativePtr_ = nativePtr;
    }

    @Override
    protected void finalize()
    {
        if (nativePtr_ != 0) Logging.logNotClosedOnFinalization(getClass());
    }

    /**
     *  Releases native resources (such as memory) associated with this object.
     *  <p>
     *  Note that it is perfectly safe to call this function while slave
     *  instances created from the FMU are still active.
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
     *  Returns which FMI version is used in this FMU.
     */
    public FMIVersion getFMIVersion() throws Exception
    {
        return getFMIVersionNative(nativePtr_);
    }

    /**
     *  Returns a description of this FMU.
     */
    public SlaveTypeDescription getDescription() throws Exception
    {
        return getDescriptionNative(nativePtr_);
    }

    /**
     *  Creates a co-simulation slave instance of this FMU.
     */
    public Instance instantiateSlave() throws Exception
    {
        return instantiateSlaveNative(nativePtr_);
    }


    private static native void destroyNative(long selfPtr);
    private static native FMIVersion getFMIVersionNative(long selfPtr)
        throws Exception;
    private static native SlaveTypeDescription getDescriptionNative(long selfPtr)
        throws Exception;
    private static native OpaqueInstance instantiateSlaveNative(long selfPtr)
        throws Exception;

    private long nativePtr_ = 0;
}
