package com.sfh.dsb;

import com.sfh.dsb.Causality;
import com.sfh.dsb.DataType;
import com.sfh.dsb.Variability;


/**
 * Information about one of the variables in a slave type.
 */
public class VariableDescription
{
    /**
     * An identifier whic uniquely refers to this variable in the context
     * of a single slave type.
     */
    public int getID() { return id; }

    /**
     * A human-readable name for the variable.
     */
    public String getName() { return name; }

    /**
     * The variable's data type.
     */
    public DataType getDataType() { return dataType; }

    /**
     * The variable's causality.
     */
    public Causality getCausality() { return causality; }

    /**
     * The variable's variability.
     */
    public Variability getVariability() { return variability; }


    private int id;
    private String name;
    private DataType dataType;
    private Causality causality;
    private Variability variability;
}
