/* Copyright 2014-2017, SINTEF Ocean.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package no.viproma.coral.master;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import no.viproma.coral.master.EntityNotFoundException;
import no.viproma.coral.master.Execution;
import no.viproma.coral.master.ModelConstructionException;
import no.viproma.coral.master.ModelSlaveMap;
import no.viproma.coral.master.ProviderCluster;
import no.viproma.coral.model.Causality;
import no.viproma.coral.model.ScalarValue;
import no.viproma.coral.model.SlaveID;
import no.viproma.coral.model.VariableDescription;
import no.viproma.coral.model.VariableSetting;


/**
 * A convenience class for "offline" simulation setup.
 * <p>
 * This class offers a convenient pre-stage to setting up an online simulation
 * using {@link Execution}.  <code>ModelBuilder</code> allows one to
 * add slaves, set initial variable values, and connect variables, all without
 * having instantiated the slaves first.  This makes for quick and easy model
 * construction and simpler debugging.  A {@link ProviderCluster} must be
 * queried for a list of available slave types on the network, but apart from
 * that, no other network communication takes place before
 * {@link #apply apply()} is called.
 * <p>
 * Most model construction errors (such as connecting incompatible variables)
 * are caught by this class, but there are some problems that can only be
 * detected when the slaves are instantiated.  Examples include network issues,
 * slaves that can only be instantiated a limited number of times, etc.
 */
public class ModelBuilder
{
    /** The name of a slave and a description of one of its variables. */
    public static class Variable
    {
        Variable(String slaveName, VariableDescription variable)
        {
            slaveName_ = slaveName;
            variable_ = variable;
        }

        /** Returns the slave name. */
        public String getSlaveName() { return slaveName_; }

        /** Returns the variable description. */
        public VariableDescription getVariable() { return variable_; }

        private String slaveName_;
        private VariableDescription variable_;
    }

    /** An object which represents a connection between two variables. */
    public static class Connection
    {
        public Connection(Variable output, Variable input)
        {
            output_ = output;
            input_ = input;
        }

        /** Returns the output variable. */
        public Variable getOutput() { return output_; }

        /** Returns the input variable. */
        public Variable getInput() { return input_; }

        private Variable output_;
        private Variable input_;
    }

    /**
     * Constructor.
     *
     * @param cluster
     *      A communication interface for the slave providers which will
     *      be used to instantiate slaves.
     * @param commTimeout_ms
     *      The communications timeout used for requests to slave providers.
     */
    public ModelBuilder(ProviderCluster cluster, int commTimeout_ms)
    {
        if (cluster == null) {
            throw new IllegalArgumentException("cluster is null");
        }
        if (commTimeout_ms < 0) {
            throw new IllegalArgumentException("commTimeout_ms is negative");
        }
        cluster_               = cluster;
        clusterCommTimeout_ms_ = commTimeout_ms;
        clusterSlaveTypes_     = new HashMap<String, ProviderCluster.SlaveType>();
        modelSlaveTypes_       = new HashMap<String, ModelSlaveType>();
        slaves_                = new HashMap<String, ModelSlaveType>();
        initialValues_         = new HashMap<String, Map<VariableDescription, ScalarValue>>();
        connections_           = new HashMap<String, Map<VariableDescription, Variable>>();
    }

    /**
     * Adds a slave to the model.
     *
     * @param slaveName
     *      A unique name which will be associated with the slave. This can
     *      only contain alphanumeric characters and underscores, and the
     *      first character must be a letter. May not be empty.
     * @param typeName
     *      The name of the slave type.  This must correspond to a slave
     *      type that exists in the cluster.
     *
     * @throws IllegalArgumentException
     *      If <code>slaveName</code> is not a valid name.
     * @throws EntityNotFoundException
     *      If the given slave type was not found in the cluster.
     * @throws ModelConstructionException
     *      If a slave with the given name already exists in the model.
     * @throws Exception
     *      If the cluster needs to be queried for information about slave types,
     *      and this fails for some reason (e.g. due to network issues).
     */
    public void addSlave(String slaveName, String typeName)
        throws EntityNotFoundException, ModelConstructionException, Exception
    {
        if (!isValidSlaveName(slaveName)) {
            throw new IllegalArgumentException("Invalid slave name: " + slaveName);
        }
        if (slaves_.containsKey(slaveName)) {
            throw new ModelConstructionException(
                "A slave with the given name already exists: " + slaveName);
        }
        ModelSlaveType type = getModelSlaveType(typeName);
        slaves_.put(slaveName, type);
    }

    private static boolean isValidSlaveName(String s)
    {
        if (s.isEmpty()) return false;
        if (!isAsciiLetter(s.charAt(0))) return false;
        for (int i = 1; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (!isAsciiLetter(c) && !isAsciiDigit(c) && c != '_') return false;
        }
        return true;
    }

    private static boolean isAsciiLetter(char c)
    {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private static boolean isAsciiDigit(char c)
    {
        return c >= '0' && c <= '9';
    }

    /**
     * Returns a list of the names of all slaves that have been added to the
     * simulation.
     */
    public Iterable<String> getSlaveNames()
    {
        return slaves_.keySet();
    }

    /**
     * Returns a description of the type of the given slave.
     *
     * @param slaveName
     *      The name of a slave which has previously been added to the
     *      simulation.
     * @return
     *      A slave type description.
     * @throws EntityNotFoundException
     *      If <code>slaveName</code> does not refer to a slave in the
     *      simulation.
     */
    public ProviderCluster.SlaveType getSlaveTypeOf(String slaveName)
        throws EntityNotFoundException
    {
        ModelSlaveType st = slaves_.get(slaveName);
        if (st == null) {
            throw new EntityNotFoundException("Unknown slave: " + slaveName);
        }
        return st.clusterSlaveType;
    }

    /**
     * Sets the initial value of a variable, replacing any previous value.
     *
     * @param slaveName
     *      The name of the slave whose variable to initialise.
     * @param variableName
     *      The name of the variable to initialise.
     * @param value
     *      The variable value.
     *
     * @throws EntityNotFoundException
     *      If either <code>slaveName</code> or <code>variableName</code>
     *      are unknown.
     * @throws ModelConstructionException
     *      If the value is not appropriate for the variable, e.g. if their
     *      data types don't match.
     */
    public void setInitialVariableValue(
        String slaveName,
        String variableName,
        ScalarValue value)
        throws EntityNotFoundException, ModelConstructionException
    {
        VariableDescription varDesc = getVariableDescription(slaveName, variableName);
        if (varDesc.getDataType() != value.getDataType()) {
            throw new ModelConstructionException(
                "Attempted to initialize variable " + slaveName + "." + variableName
                + ", which is of type '" + varDesc.getDataType().name().toLowerCase()
                + "', with a value of type '" + value.getDataType().name().toLowerCase()
                + "'");
        }
        Map<VariableDescription, ScalarValue> slaveInits = initialValues_.get(slaveName);
        if (slaveInits == null) {
            slaveInits = new HashMap<VariableDescription, ScalarValue>();
            initialValues_.put(slaveName, slaveInits);
        }
        slaveInits.put(varDesc, value);
    }

    /**
     * Returns the initial value of a variable, or null if no initial value
     * has been previously specified with
     * {@link #setInitialVariableValue setInitialvariableValue()}
     *
     * @param slaveName
     *      The name of the slave whose variable to initialise.
     * @param variableName
     *      The name of the variable to initialise.
     *
     * @throws EntityNotFoundException
     *      If either <code>slaveName</code> or <code>variableName</code>
     *      are unknown.
     */
    public ScalarValue getInitialVariableValue(
        String slaveName, String variableName)
        throws EntityNotFoundException
    {
        VariableDescription varDesc = getVariableDescription(slaveName, variableName);
        Map<VariableDescription, ScalarValue> slaveInits = initialValues_.get(slaveName);
        if (slaveInits == null) return null;
        return slaveInits.get(varDesc);
    }

    /**
     * Connects an output variable to an input variable, replacing any
     * previous connections to the input variable.
     * <p>
     * Note that an output variable may be connected to any number of input
     * variables, but an input variable may only be connected to one output
     * variable.
     *
     * @param outputSlaveName
     *      The name of the slave which is to provide the output.
     * @param outputVariableName
     *      The name of the output variable.
     * @param inputSlaveName
     *      The name of the slave which is to receive the input.
     * @param inputVariableName
     *      The name of the input variable.
     *
     * @throws EntityNotFoundException
     *      If either of the slave or variable names are unknown.
     * @throws ModelConstructionException
     *      If the two variables cannot be connected due to incompatible
     *      data type, variability or causality.
     */
    public void connectVariables(
        String outputSlaveName, String outputVariableName,
        String inputSlaveName, String inputVariableName)
        throws EntityNotFoundException, ModelConstructionException
    {
        VariableDescription oVarDesc = getVariableDescription(
            outputSlaveName, outputVariableName);
        VariableDescription iVarDesc = getVariableDescription(
            inputSlaveName, inputVariableName);

        // Causality checks
        if (oVarDesc.getCausality() == Causality.OUTPUT) {
            if (iVarDesc.getCausality() != Causality.INPUT) {
                throw new ModelConstructionException(connectionErrMsg(
                    outputSlaveName, outputVariableName,
                    inputSlaveName, inputVariableName,
                    "An output variable may only be connected to an input variable"));
            }
        } else if (oVarDesc.getCausality() == Causality.CALCULATED_PARAMETER) {
            if (iVarDesc.getCausality() != Causality.PARAMETER
                    && iVarDesc.getCausality() != Causality.INPUT) {
                throw new ModelConstructionException(connectionErrMsg(
                    outputSlaveName, outputVariableName,
                    inputSlaveName, inputVariableName,
                    "A calculated parameter variable may only be connected to a parameter or input variable"));
            }
        } else {
            throw new ModelConstructionException(connectionErrMsg(
                outputSlaveName, outputVariableName,
                inputSlaveName, inputVariableName,
                "Only output variables or calculated parameters may be used as outputs"));
        }

        // Simple variability check
        if (oVarDesc.getVariability().compareTo(iVarDesc.getVariability()) > 0) {
            throw new ModelConstructionException(connectionErrMsg(
                outputSlaveName, outputVariableName,
                inputSlaveName, inputVariableName,
                "Incompatible variability: A "
                + oVarDesc.getVariability().name().toLowerCase()
                + " variable cannot be connected to a "
                + iVarDesc.getVariability().name().toLowerCase()
                + " variable"));
        }

        // Data type check
        if (oVarDesc.getDataType() != iVarDesc.getDataType()) {
            throw new ModelConstructionException(connectionErrMsg(
                outputSlaveName, outputVariableName,
                inputSlaveName, inputVariableName,
                "Incompatible data types: A variable of type '"
                + oVarDesc.getDataType().name().toLowerCase()
                + "' cannot be connected to a variable of type '"
                + iVarDesc.getDataType().name().toLowerCase()
                + "'"));
        }

        // Add connection
        Map<VariableDescription, Variable> iSlaveConns =
            connections_.get(inputSlaveName);
        if (iSlaveConns == null) {
            iSlaveConns = new HashMap<VariableDescription, Variable>();
            connections_.put(inputSlaveName, iSlaveConns);
        }
        iSlaveConns.put(iVarDesc, new Variable(outputSlaveName, oVarDesc));
    }

    // Helper function for connectVariables() which creates a nice error message.
    private static String connectionErrMsg(
        String outputSlaveName, String outputVariableName,
        String inputSlaveName, String inputVariableName,
        String msg)
    {
        return "Error connecting variable " + outputSlaveName + "." + outputVariableName
            + " to " + inputSlaveName + "." + inputVariableName + ": " + msg;
    }

    /**
     * Convenience method which creates a list of all connections going
     * <em>to</em> input variables of the specified slave.
     * <p>
     * Note that the list is rebuilt each time this function is called;
     * it is not a view on some internal data structure.
     */
    public List<Connection> getConnectionsTo(String slaveName)
        throws EntityNotFoundException
    {
        if (!slaves_.containsKey(slaveName)) {
            throw new EntityNotFoundException("Unknown slave: " + slaveName);
        }
        List<Connection> ret = new ArrayList<Connection>();
        addConnectionsTo(ret, slaveName);
        return ret;
    }

    /**
     * Convenience method which creates a list of all connections in the model.
     * <p>
     * Note that the list is rebuilt each time this function is called;
     * it is not a view on some internal data structure.
     */
    public List<Connection> getConnections()
    {
        List<Connection> ret = new ArrayList<Connection>();
        for (String slaveName : getSlaveNames()) {
            addConnectionsTo(ret, slaveName);
        }
        return ret;
    }

    private void addConnectionsTo(List<Connection> list, String slaveName)
    {
        Map<VariableDescription, Variable> slaveConns = connections_.get(slaveName);
        if (slaveConns == null) return;
        for (Map.Entry<VariableDescription, Variable> conn : slaveConns.entrySet()) {
            list.add(new Connection(
                conn.getValue(),                            // output
                new Variable(slaveName, conn.getKey())));   // input
        }
    }

    /**
     * Convenience method which creates a list of all input variables in the
     * model which have <em>not</em> been connected yet.
     * <p>
     * This method is primarily meant as a debugging tool.
     * Note that the list is rebuilt each time this function is called;
     * it is not a view on some internal data structure.
     */
    public List<Variable> getUnconnectedInputs()
    {
        // Make a data structure which contains all connected input variables
        Map<String, Set<VariableDescription>> connected =
            new HashMap<String, Set<VariableDescription>>();
        for (Map.Entry<String, Map<VariableDescription, Variable>> slaveConns :
             connections_.entrySet())
        {
            String slaveName = slaveConns.getKey();
            Set<VariableDescription> slaveConnected = connected.get(slaveName);
            if (slaveConnected == null) {
                slaveConnected = new HashSet<VariableDescription>();
                connected.put(slaveName, slaveConnected);
            }

            for (Map.Entry<VariableDescription, Variable> conn :
                slaveConns.getValue().entrySet())
            {
                slaveConnected.add(conn.getKey());
            }
        }

        // Now, go through *all* variables and make a list of the input
        // variables that are not in 'connected'.
        List<Variable> ret = new ArrayList<Variable>();
        for (Map.Entry<String, ModelSlaveType> slave : slaves_.entrySet()) {
            Set<VariableDescription> connectedVars = connected.get(slave.getKey());
            for (VariableDescription var : slave.getValue().variables.values()) {
                if (var.getCausality() == Causality.INPUT
                    && (connectedVars == null || !connectedVars.contains(var)))
                {
                    ret.add(new Variable(slave.getKey(), var));
                }
            }
        }
        return ret;
    }

    /**
     * Transfers the model structure to an execution controller.
     * <p>
     * This will instantiate all slaves using the {@link ProviderCluster} that
     * was passed to {@linkplain #ModelBuilder the constructor}, add them to the
     * execution controlled by <code>execution</code>, and set initial variable
     * values and make connections.
     * <p>
     * It is strongly recommended that the execution
     * be in a "pristine" state, i.e. with no slaves already added.
     *
     * @param execution
     *      The execution controller to which the model structure should be
     *      transfered.
     * @param instantiationTimeout_ms
     *      The timeout that will be used for all
     *      {@link ProviderCluster#instantiateSlave} calls.
     * @param commandTimeout_ms
     *      The timeout that will be used for all
     *      <code>ExecutionController</code> method calls.
     *
     * @return
     *      An object which contains mappings from slave names to slave IDs.
     */
    public ModelSlaveMap apply(
        Execution execution,
        int instantiationTimeout_ms,
        int commandTimeout_ms)
        throws Exception
    {
        // Instantiate all slaves and add them to the execution
        List<AddedSlave> slavesToAdd = new ArrayList<AddedSlave>();
        for (Map.Entry<String, ModelSlaveType> e : slaves_.entrySet()) {
            slavesToAdd.add(new AddedSlave(
                cluster_.instantiateSlave(
                    e.getValue().clusterSlaveType,
                    instantiationTimeout_ms),
                e.getKey()));
        }
        if (!slavesToAdd.isEmpty()) {
            execution.addSlaves(slavesToAdd, commandTimeout_ms);
        }

        // Extract the slave IDs from the list of AddedSlaves
        Map<String, SlaveID> slaveIDs = new HashMap<String, SlaveID>();
        for (AddedSlave slave : slavesToAdd) {
            slaveIDs.put(slave.getName(), slave.getID());
        }

        // Set initial values and make connections
        List<SlaveConfig> slaveConfigs = new ArrayList<SlaveConfig>();
        for (AddedSlave slave : slavesToAdd) {
            List<VariableSetting> varSettings = new ArrayList<VariableSetting>();

            Map<VariableDescription, ScalarValue> initialValues =
                initialValues_.get(slave.getName());
            if (initialValues != null) {
                for (Map.Entry<VariableDescription, ScalarValue> iv :
                        initialValues.entrySet()) {
                    varSettings.add(new VariableSetting(
                        iv.getKey().getID(), iv.getValue()));
                }
            }

            Map<VariableDescription, ModelBuilder.Variable> connections =
                connections_.get(slave.getName());
            if (connections != null) {
                for (Map.Entry<VariableDescription, ModelBuilder.Variable> cn :
                        connections.entrySet()) {
                    VariableDescription ownVar = cn.getKey();
                    VariableDescription connVar = cn.getValue().getVariable();
                    SlaveID connSlave = slaveIDs.get(cn.getValue().getSlaveName());
                    varSettings.add(new VariableSetting(
                        ownVar.getID(),
                        new no.viproma.coral.model.Variable(connSlave, connVar.getID())));
                }
            }

            if (!varSettings.isEmpty()) {
                slaveConfigs.add(new SlaveConfig(slave.getID(), varSettings));
            }
        }
        if (!slaveConfigs.isEmpty()) {
            execution.reconfigure(slaveConfigs, commandTimeout_ms);
        }

        return new ModelSlaveMap(slaveIDs, slaves_);
    }


    // =========================================================================

    // Our own reference to a ProviderCluster.SlaveType, where we also cache
    // useful information for quick lookup.
    static class ModelSlaveType
    {
        ModelSlaveType(ProviderCluster.SlaveType clusterSlaveType)
        {
            this.clusterSlaveType = clusterSlaveType;
            variables = new HashMap<String, VariableDescription>();
            for (VariableDescription v : clusterSlaveType.getVariables()) {
                variables.put(v.getName(), v);
            }
        }

        // The corresponding cluster slave type
        ProviderCluster.SlaveType clusterSlaveType;

        // A mapping from variable names to variable descriptions, for fast and
        // convenient variable lookup.  This also lets us hold on to the
        // VariableDescription objects, which is important because
        // ProviderCluster creates *new* such objects on every call to
        // getSlaveTypes().  We use them as map keys in several maps here,
        // and it would be disastrous if their address suddenly changed.
        Map<String, VariableDescription> variables;
    }

    // Returns the ProviderCluster.SlaveType object associated with typeName,
    // updating the cluster slave type cache as necessary. Throws if typeName
    // is invalid.
    private ProviderCluster.SlaveType getClusterSlaveType(String typeName)
        throws EntityNotFoundException, Exception
    {
        ProviderCluster.SlaveType ret = clusterSlaveTypes_.get(typeName);
        if (ret == null) {
            for (ProviderCluster.SlaveType st :
                    cluster_.getSlaveTypes(clusterCommTimeout_ms_)) {
                // We only update our map if the slave type isn't there already,
                // so the objects in clusterSlaveTypes_ don't get out of sync
                // with those stored in modelSlaveTypes_.
                if (!clusterSlaveTypes_.containsKey(st.getName())) {
                    clusterSlaveTypes_.put(st.getName(), st);
                    if (st.getName().equals(typeName)) ret = st;
                }
            }
        }
        if (ret == null) {
            throw new EntityNotFoundException("Unknown slave type: " + typeName);
        }
        return ret;
    }

    // Returns the ModelSlaveType object associated with typeName, adding it
    // to the model slave type cache if necessary.  Throws if typeName is
    // invalid.
    private ModelSlaveType getModelSlaveType(String typeName)
        throws EntityNotFoundException, Exception
    {
        ModelSlaveType ret = modelSlaveTypes_.get(typeName);
        if (ret == null) {
            ret = new ModelSlaveType(getClusterSlaveType(typeName));
            modelSlaveTypes_.put(typeName, ret);
        }
        return ret;
    }

    // Returns the VariableDescription object associated with the variable
    // called variableName in the slave called slaveName.  Throws if either
    // name is invalid.
    private VariableDescription getVariableDescription(
        String slaveName, String variableName)
        throws EntityNotFoundException
    {
        ModelSlaveType slaveType = slaves_.get(slaveName);
        if (slaveType == null) {
            throw new EntityNotFoundException("Unknown slave: " + slaveName);
        }
        VariableDescription varDesc = slaveType.variables.get(variableName);
        if (varDesc == null) {
            throw new EntityNotFoundException(
                "Unknown variable: " + slaveName + "." + variableName);
        }
        return varDesc;
    }

    private ProviderCluster cluster_;
    private int clusterCommTimeout_ms_;
    private Map<String, ProviderCluster.SlaveType> clusterSlaveTypes_;
    private Map<String, ModelSlaveType> modelSlaveTypes_;
    private Map<String, ModelSlaveType> slaves_;
    private Map<String, Map<VariableDescription, ScalarValue>> initialValues_;
    private Map<String, Map<VariableDescription, Variable>> connections_;
}
