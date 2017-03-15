/* Copyright 2014-2017, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral.model;

import no.viproma.coral.model.Causality;
import no.viproma.coral.model.DataType;
import no.viproma.coral.model.Variability;


/**
 * Information about one of the variables associated with a slave type.
 */
public class VariableDescription
{
    /**
     * An id_entifier whic uniquely refers to this variable in the context
     * of a single slave type.
     */
    public int getID() { return id_; }

    void setID(int value) { id_ = value; }

    /**
     * A human-readable name_ for the variable.
     */
    public String getName() { return name_; }

    void setName(String value) { name_ = value; }

    /**
     * The variable's data type.
     */
    public DataType getDataType() { return dataType_; }

    void setDataType(DataType value) { dataType_ = value; }

    /**
     * The variable's causality_.
     */
    public Causality getCausality() { return causality_; }

    void setCausality(Causality value) { causality_ = value; }

    /**
     * The variable's variability_.
     */
    public Variability getVariability() { return variability_; }

    void setVariability(Variability value) { variability_ = value; }

    private int id_;
    private String name_;
    private DataType dataType_;
    private Causality causality_;
    private Variability variability_;
}
