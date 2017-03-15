package no.viproma.coral.model;

import no.viproma.coral.model.DataType;


/**
 * A scalar value of real, integer, boolean or string type, typically used as
 * a variable value.
 */
public class ScalarValue
{
    /** Constructs a real value. */
    public ScalarValue(double value)
    {
        dataType_ = DataType.REAL;
        realValue_ = value;
    }

    /** Constructs an integer value. */
    public ScalarValue(int value)
    {
        dataType_ = DataType.INTEGER;
        integerValue_ = value;
    }

    /** Constructs a boolean value. */
    public ScalarValue(boolean value)
    {
        dataType_ = DataType.BOOLEAN;
        booleanValue_ = value;
    }

    /** Constructs a string value. */
    public ScalarValue(String value)
    {
        dataType_ = DataType.STRING;
        stringValue_ = value;
    }

    /** Returns the value's data type. */
    public DataType getDataType() { return dataType_; }

    /** Returns the value of the variable. */
    public double getRealValue()
    {
        if (dataType_ != DataType.REAL) {
            throw new IllegalStateException("Wrong data type");
        }
        return realValue_;
    }

    /** Returns the value of the variable. */
    public int getIntegerValue()
    {
        if (dataType_ != DataType.INTEGER) {
            throw new IllegalStateException("Wrong data type");
        }
        return integerValue_;
    }

    /** Returns the value of the variable. */
    public boolean getBooleanValue()
    {
        if (dataType_ != DataType.BOOLEAN) {
            throw new IllegalStateException("Wrong data type");
        }
        return booleanValue_;
    }

    /** Returns the value of the variable. */
    public String getStringValue()
    {
        if (dataType_ != DataType.STRING) {
            throw new IllegalStateException("Wrong data type");
        }
        return stringValue_;
    }


    private DataType dataType_;
    private double realValue_;
    private int integerValue_;
    private boolean booleanValue_;
    private String stringValue_;
}
