package com.sfh.dsb;

import com.sfh.dsb.ScalarValue;
import com.sfh.dsb.Variable;


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
        connectedOutput_ = null;
    }

    /**
     * Indicates an input variable which should be connected to an output
     * variable.
     */
    public VariableSetting(int inputVariableID, Variable outputVariable)
    {
        variableID_ = inputVariableID;
        value_ = null;
        connectedOutput_ = outputVariable;
    }

    /** Returns the ID of the variable which is to be initialised/connected. */
    int getVariableID() { return variableID_; }

    /** Returns whether the variable is to be given a value. */
    boolean hasValue() { return value_ != null; }

    /** Returns the variable value. */
    ScalarValue getValue() { return value_; }

    /** Returns whether the variable is to be connected. */
    boolean hasConnectedOutput() { return connectedOutput_ != null; }

    /**
     * Returns the output variable to which the input variable should be
     * connected.
     */
    Variable getConnectedOutput()
    {
        return connectedOutput_;
    }


    private int variableID_;
    private ScalarValue value_;
    private Variable connectedOutput_;
}
