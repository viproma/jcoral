/* Copyright 2014-2017, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral.model;

import no.viproma.coral.model.SlaveID;


/**
 * A class that refers to a particular variable of a particular slave in
 * an execution.
 * <p>
 * An object of this class holds a combination of a slave ID and a variable ID.
 */
public class Variable
{
    /** Constructor. */
    public Variable(SlaveID slaveID, int variableID)
    {
        if (slaveID == null) throw new IllegalArgumentException("slaveID is null");
        if (variableID < 0) throw new IllegalArgumentException("variableID is negative");
        slaveID_ = slaveID;
        variableID_ = variableID;
    }

    /** Returns the slave ID. */
    public SlaveID getSlaveID() { return slaveID_; }

    /** Returns the variable ID. */
    public int getVariableID() { return variableID_; }


    private SlaveID slaveID_;
    private int variableID_;
}
