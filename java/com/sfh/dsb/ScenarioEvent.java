package com.sfh.dsb;

import com.sfh.dsb.ScalarValue;
import com.sfh.dsb.SlaveID;
import com.sfh.dsb.Variable;
import com.sfh.dsb.VariableSetting;


/**
 * An object which represents an event in a simulation scenario.
 * <p>
 * Currently, the only scenario events supported are simple modifications of
 * variable values.  In the future, other event types may be supported, like
 * making/breaking connections, adding/removing slaves, etc.
 */
public class ScenarioEvent
{
    /**
     * Creates an event where a variable value is modified.
     *
     * @param timePoint
     *      The point in time at which this event will be triggered.
     * @param variable
     *      The variable whose value to modify.
     * @param value
     *      A new value for the variable.
     */
    public ScenarioEvent(double timePoint, Variable variable, ScalarValue value)
    {
        timePoint_ = timePoint;
        slaveID_ = variable.getSlaveID();
        variableSetting_ = new VariableSetting(variable.getVariableID(), value);
    }

    /**
     * Returns the point in time at which this event will be triggered.
     */
    public double getTimePoint()
    {
        return timePoint_;
    }

    SlaveID getSlaveID()
    {
        return slaveID_;
    }

    VariableSetting getVariableSetting()
    {
        return variableSetting_;
    }

    private double timePoint_;
    private SlaveID slaveID_;
    private VariableSetting variableSetting_;
}
