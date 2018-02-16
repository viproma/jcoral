/* Copyright 2018-2018, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral.slave;

import no.viproma.coral.slave.Instance;


/**
 *  Factory class for creating slave instances.
 *
 *  @see no.viproma.coral.fmi.FMU#instantiateSlave
 *      FMU.instantiateSlave(), which creates a slave instance based on an FMU.
 */
public class InstanceFactory
{
    static
    {
        System.loadLibrary("jcoral");
    }

    /**
     *  Wraps another slave instance and logs the values of its output
     *  variables to a CSV file for each time step.
     *  <p>
     *  Before returning, this function will call {@code instanceToWrap.close()},
     *  as the original slave instance should no longer be used directly.
     *
     *  @param instanceToWrap
     *      The slave instance to wrap.
     *  @param outputFilePrefix
     *      A directory and prefix for a CSV output file.  An execution- and
     *      slave-specific name as well as a ".csv" extension will be appended
     *      to this name.  If no prefix is required, and the string only
     *      contains a directory name, it should end with a directory separator
     *      (a slash).
     *  @return
     *      A new slave instance to replace the wrapped one.
     */
    public static Instance newCSVLoggingInstance(
        Instance instanceToWrap, String outputFilePrefix)
        throws Exception
    {
        Instance newInstance = newCSVLoggingInstanceNative(
            instanceToWrap.getNativeInstancePtr(),
            outputFilePrefix);
        instanceToWrap.close();
        return newInstance;
    }

    private static native Instance newCSVLoggingInstanceNative(
        long instanceToWrapPtr, String outputFilePrefix)
        throws Exception;
}

