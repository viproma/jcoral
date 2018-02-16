/* Copyright 2018-2018, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral.fmi;

import java.io.File;
import no.viproma.coral.Logging;
import no.viproma.coral.fmi.FMU;


/**
 *  Imports and caches FMUs.
 *  <p>
 *  The main purpose of this class is to read FMU files and create {@link FMU}
 *  objects to represent them.  This is done with the {@link #importFMU} and
 *  {@link #importUnpackedFMU} methods.
 *  <p>
 *  An Importer object uses an on-disk cache that holds the unpacked contents
 *  of previously imported FMUs, so that they don't need to be unpacked anew every
 *  time they are imported.  This is a huge time-saver when large and/or many FMUs
 *  are loaded.  The path to this cache may be supplied by the user, in which case
 *  it is not automatically emptied when {@link #close} is called.  Thus, if the
 *  same path is supplied each time, the cache becomes persistent between program
 *  runs.  It may be cleared manually by calling {@link #cleanCache}.
 *  <p>
 *  <strong>Warning:</strong>
 *  Currently there are no synchronisation mechanisms to protect the cache from
 *  concurrent use, so accessing the same cache from multiple
 *  instances/processes will likely cause problems.
 *  <p>
 *  Objects of this class should always be disposed of with {@link #close} when
 *  they are no longer needed, to avoid resource leaks in the underlying native
 *  code. (A nice, automated way to do this is to use
 *  <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">the
 *  try-with-resources statement</a>.)
 */
public class Importer implements AutoCloseable
{
    static
    {
        System.loadLibrary("jcoral");
    }

    /**
     *  Creates a new FMU importer that uses a specific cache directory.
     *  <p>
     *  The given directory will not be removed nor emptied when {@link #close}
     *  is called.
     *
     *  @param cachePath
     *      The path to the directory which will hold the FMU cache.  If it does
     *      not exist already, it will be created.
     */
    public Importer(File cachePath) throws Exception
    {
        nativePtr_ = createNativeP(cachePath.toString());
    }

    /**
     *  Creates a new FMU importer that uses a temporary cache directory.
     *  <p>
     *  A new cache directory will be created in a location suitable for temporary
     *  files under the conventions of the operating system.  It will be completely
     *  removed again when {@link #close} is called.
    */
    public Importer() throws Exception
    {
        nativePtr_ = createNative();
    }

    @Override
    protected void finalize()
    {
        if (nativePtr_ != 0) Logging.logNotClosedOnFinalization(getClass());
    }

    /**
     *  Releases native resources (such as memory) associated with this object.
     *  <p>
     *  If the object was constructed using {@link #Importer()}, the temporary
     *  cache directory will be deleted.
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
     *  Imports an FMU.
     *
     *  @param fmuPath
     *      The path to the FMU file.
     *  @return
     *      An object which represents the imported FMU.
    */
    public FMU importFMU(File fmuPath) throws Exception
    {
        return importFMUNative(nativePtr_, fmuPath.toString());
    }

    /**
     *  Imports an FMU that has already been unpacked.
     *  <p>
     *  This is more or less equivalent to {@link #importFMU}, but since the
     *  FMU is already unpacked its contents will be read from the specified
     *  directory rather than the cache.
     *
     *  @param unpackedFMUPath
     *      The path to a directory that holds the unpacked contents of an FMU.
     *  @return
     *      An object which represents the imported FMU.
    */
    public FMU importUnpackedFMU(File unpackedFMUPath) throws Exception
    {
        return importUnpackedFMUNative(nativePtr_, unpackedFMUPath.toString());
    }

    /**
     *  Removes unused files and directories from the FMU cache.
     *  <p>
     *  This will remove all FMU contents from the cache, except the ones for
     *  which there are unclosed FMU objects.
     */
    public void cleanCache() throws Exception
    {
        cleanCacheNative(nativePtr_);
    }


    private static native long createNativeP(String cachePath)
        throws Exception;
    private static native long createNative()
        throws Exception;
    private static native void destroyNative(long selfPtr);
    private static native FMU importFMUNative(
        long selfPtr, String fmuPath)
        throws Exception;
    private static native FMU importUnpackedFMUNative(
        long selfPtr, String unpackedFMUPath)
        throws Exception;
    private static native void cleanCacheNative(long selfPtr)
        throws Exception;

    private long nativePtr_;
}
