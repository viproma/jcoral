/* Copyright 2014-2017, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral.master;

import no.viproma.coral.model.SlaveID;
import no.viproma.coral.model.VariableSetting;


/**
 *  Specifies variable values and connection changes for a single slave.
 *  <p>
 *  This class is used in calls to {@link Execution#reconfigure} to
 *  specify the changes which are to be effected for one particular
 *  slave, and to obtain information about any failures the slave
 *  might have reported regarding these changes.
 *  <p>
 *  If <code>reconfigure()</code> throws, the {@link #getError}
 *  function may be called to figure out whether this particular
 *  slave contributed to the failure, and if so, why.
 */
public class SlaveConfig
{
    /**
     *  Constructor.
     *
     *  @param slaveID
     *      The ID number of the slave whose variables are to be
     *      configured.
     *  @param variableSettings
     *      The variable value/connection changes.
     */
    public SlaveConfig(
        SlaveID slaveID, Iterable<VariableSetting> variableSettings)
    {
        if (slaveID == null || variableSettings == null) {
            throw new IllegalArgumentException("Method argument is null");
        }
        slaveID_ = slaveID;
        variableSettings_ = variableSettings;
    }

    /** Gets the ID number that was passed to the constructor. */
    public SlaveID getSlaveID() { return slaveID_; }

    /** Gets the variable settings that were passed to the constructor. */
    public Iterable<VariableSetting> getVariableSettings()
    {
        return variableSettings_;
    }

    /**
     *  Returns a textual description of any error that may have occurred.
     *  <p>
     *  If this function is called before the object has been passed to
     *  {@link Execution#reconfigure}, or if there were no errors configuring
     *  this particular slave, it will return null.
     */
    public String getError() { return error_; }

    void setError(String value) { error_ = value; }

    private SlaveID slaveID_;
    private Iterable<VariableSetting> variableSettings_;
    private String error_;
}
