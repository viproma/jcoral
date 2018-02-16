/* Copyright 2018-2018, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral.slave;

import no.viproma.coral.Logging;
import no.viproma.coral.model.SlaveTypeDescription;
import no.viproma.coral.slave.Instance;


public class OpaqueInstance implements Instance
{
    static
    {
        System.loadLibrary("jcoral");
    }

    OpaqueInstance(long nativePtr)
    {
        nativePtr_ = nativePtr;
    }

    @Override
    protected void finalize()
    {
        if (nativePtr_ != 0) Logging.logNotClosedOnFinalization(getClass());
    }

    @Override
    public void close()
    {
        if (nativePtr_ != 0) {
            destroyNative(nativePtr_);
            nativePtr_ = 0;
        }
    }

    @Override
    public SlaveTypeDescription getTypeDescription() throws Exception
    {
        return getTypeDescriptionNative(nativePtr_);
    }

    @Override
    public long getNativeInstancePtr() { return nativePtr_; }

    private static native void destroyNative(long selfPtr);
    private static native SlaveTypeDescription getTypeDescriptionNative(long selfPtr)
        throws Exception;

    private long nativePtr_ = 0;
}
