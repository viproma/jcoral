/* Copyright 2014-2017, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral.master;

import no.viproma.coral.model.SlaveID;
import no.viproma.coral.net.SlaveLocator;


/**
 *  Specifies a slave which is to be added to an execution.
 *  <p>
 *  This class is used in calls to {@link Execution#addSlaves} to supply
 *  information about the slave which is to be added, and to obtain new
 *  information about the slave after it has been added (including any
 *  errors that may have occurred in the process).
 *  <p>
 *  After <code>addSlaves</code> has completed, the {@link #getID} and
 *  {@link #getError} functions can be queried for information about the
 *  slave.
 */
public class AddedSlave
{
    /**
     *  Constructor.
     *
     *  @param locator
     *      Information about the slave's network location.
     *  @param name
     *      A name for the slave, unique in the execution.
     *      Slave names may only consist of letters (a-z, A-Z), digits (0-9)
     *      and underscores (_). The first character must be a letter.
     *      If the argument is null, a unique name will be generated for the
     *      slave.
     */
    public AddedSlave(SlaveLocator locator, String name)
    {
        if (locator == null) {
            throw new IllegalArgumentException("Slave locator is null");
        }
        locator_ = locator;
        name_ = name;
    }

    /** Forwards to {@link #AddedSlave} with <code>name = null</code>. */
    public AddedSlave(SlaveLocator locator)
    {
        this(locator, null);
    }

    /** Returns the slave locator that was passed to the constructor. */
    public SlaveLocator getLocator() { return locator_; }

    /** Returns the name that was passed to the constructor. */
    public String getName() { return name_; }

    /**
     *  Returns the slave's unique ID.
     *  <p>
     *  If this function is called before the object has been passed to
     *  {@link Execution#addSlaves}, or if the slave could not be added,
     *  it will return null.  In the latter case, <code>addSlaves</code>
     *  will have thrown an exception, and a textual description of the
     *  error can be obtained by calling {@link #getError}.
     */
    public SlaveID getID() { return id_; }

    void setID(SlaveID value) { id_ = value; }

    /**
     *  Returns a textual description of any error that may have occurred.
     *  <p>
     *  If this function is called before the object has been passed to
     *  {@link Execution#addSlaves}, or if there were no errors adding
     *  this particular slave, it will return null.
     */
    public String getError() { return error_; }

    void setError(String value) { error_ = value; }

    private SlaveLocator locator_;
    private String name_;
    private SlaveID id_;
    private String error_;
}
