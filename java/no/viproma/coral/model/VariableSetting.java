/* Copyright 2014-2017, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral.model;

import no.viproma.coral.model.ScalarValue;
import no.viproma.coral.model.Variable;


/**
 * An object which represents the action of assigning an initial value to a
 * variable, or of connecting it to another variable.
 */
public class VariableSetting
{
    /**
     * Indicates a variable which should be given a specific value.
     * <p>
     * It is up to the caller to ensure that the data type of the value matches
     * the one expected for the variable (see {@link VariableDescription}).
     */
    public VariableSetting(int variableID, ScalarValue value)
    {
        variableID_ = variableID;
        value_ = value;
        isConnectionChange_ = false;
        connectedOutput_ = null;
    }

    /**
     * Indicates an input variable which should be connected to, or
     * disconnected from, an output variable.
     * <p>
     * Use the special value {@link #NO_CONNECTION} for {@code outputVariable}
     * to indicate that an existing connection should be broken.  (If there was
     * no connection to the variable in the first place, this has no effect.)
     */
    public VariableSetting(int inputVariableID, Variable outputVariable)
    {
        variableID_ = inputVariableID;
        value_ = null;
        isConnectionChange_ = true;
        connectedOutput_ = outputVariable;
    }

    /**
     * A special value used with {@link #VariableSetting(int, Variable)}
     * to indicate that a variable should be disconnected.
     */
    public static final Variable NO_CONNECTION = null;

    /** Returns the ID of the variable which is to be initialised/connected. */
    public int getVariableID() { return variableID_; }

    /** Returns whether the variable is to be given a value. */
    public boolean hasValue() { return value_ != null; }

    /** Returns the variable value. */
    public ScalarValue getValue() { return value_; }

    /** Returns whether the variable is to be connected. */
    public boolean isConnectionChange() { return isConnectionChange_; }

    /**
     * Returns the output variable to which the input variable should be
     * connected, or {@code null} if none.
     */
    public Variable getConnectedOutput()
    {
        return connectedOutput_;
    }


    private int variableID_;
    private ScalarValue value_;
    private boolean isConnectionChange_;
    private Variable connectedOutput_;
}
