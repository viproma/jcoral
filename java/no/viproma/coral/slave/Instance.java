/* Copyright 2018-2018, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral.slave;

import no.viproma.coral.model.SlaveTypeDescription;


/**
 *  An interface for classes that represent slave instances.
 *  <p>
 *  Objects of this class should always be disposed of with {@link #close} when
 *  they are no longer needed, to avoid resource leaks in the underlying native
 *  code. (A nice, automated way to do this is to use
 *  <a href="https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">the
 *  try-with-resources statement</a>.)
 */
public interface Instance extends AutoCloseable
{
    /** Returns an object that describes the slave type. */
    SlaveTypeDescription getTypeDescription() throws Exception;

    /** Returns a pointer to the underlying native object.
     *
     *  This method is primarily designed for internal use by this library,
     *  but can also be used by authors who wish to create their own
     *  native instance wrappers.
     *
     *  @return
     *      The numeric value of a pointer to a C++ object of type
     *      {@code std::shared_ptr<coral::slave::Instance>} (yes, a pointer
     *      to a shared pointer).
     */
    long getNativeInstancePtr();
}
