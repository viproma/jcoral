package com.sfh.dsb;


/** Exception type thrown on model construction errors. */
public class ModelConstructionException extends Exception
{
    /** Constructs an exception with the given message. */
    public ModelConstructionException(String message)
    {
        super(message);
    }

    // Strongly recommended for Serializable classes, see:
    // https://docs.oracle.com/javase/7/docs/api/java/io/Serializable.html
    private static final long serialVersionUID = 0L;
}
