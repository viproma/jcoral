package no.viproma.coral.master;

import java.util.HashMap;
import java.util.Map;

import no.viproma.coral.master.EntityNotFoundException;
import no.viproma.coral.master.ModelBuilder;
import no.viproma.coral.model.SlaveID;
import no.viproma.coral.model.Variable;
import no.viproma.coral.model.VariableDescription;


/**
 * A mapping between slave names and slave IDs for an execution created by
 * {@link ModelBuilder}.
 * <p>
 * Objects of this type are created and returned by {@link ModelBuilder#apply},
 * and contains information about how the names specified to the
 * <code>ModelBuilder</code> are mapped to the numeric IDs used by
 * {@link Execution}.
 */
public class ModelSlaveMap
{
    // TODO: Make this class independent of ModelSlaveType, which is supposed
    //       to be a private class in ModelBuilder.  (It used to be, but then
    //       it was made package private for use here, solely because I was
    //       too lazy to do it properly.)
    ModelSlaveMap(
        Map<String, SlaveID> slaveIDs,
        Map<String, ModelBuilder.ModelSlaveType> slaveInfo)
    {
        slaveIDs_ = slaveIDs;

        // Do a deep copy of the non-immutable objects in slaveInfo, to make
        // this object independent of changes in the ModelBuilder.
        slaveInfo_ = new HashMap<String, ModelBuilder.ModelSlaveType>(slaveInfo);
        for (ModelBuilder.ModelSlaveType e : slaveInfo_.values()) {
            e.variables = new HashMap<String, VariableDescription>(e.variables);
        }
    }

    /**
     * Returns the ID of a slave.
     *
     * @param slaveName
     *      The name of a slave in the model.
     * @return
     *      The slave ID.
     * @throws EntityNotFoundException
     *      If there was no slave with the given name.
     */
    public SlaveID getSlaveID(String slaveName) throws EntityNotFoundException
    {
        SlaveID slaveID = slaveIDs_.get(slaveName);
        if (slaveID == null) {
            throw new EntityNotFoundException("Unknown slave: " + slaveName);
        }
        return slaveID;
    }

    /**
     * Returns a {@link Variable} object for a variable.
     *
     * @param slaveName
     *      The name of a slave in the model.
     * @param variableName
     *      The name of one of the slave's variables.
     * @return
     *      A <code>Variable</code> object that refers to the specified
     *      variable.
     * @throws EntityNotFoundException
     *      If the slave or the variable was not found in the model.
     */
    public Variable getVariable(String slaveName, String variableName)
        throws EntityNotFoundException
    {
        SlaveID slaveID = getSlaveID(slaveName);
        VariableDescription varDesc =
            slaveInfo_.get(slaveName).variables.get(variableName);
        if (varDesc == null) {
            throw new EntityNotFoundException(
                "Unknown variable: " + slaveName + "." + variableName);
        }
        return new Variable(slaveID, varDesc.getID());
    }

    // =========================================================================

    private Map<String, SlaveID> slaveIDs_;
    private Map<String, ModelBuilder.ModelSlaveType> slaveInfo_;
}
