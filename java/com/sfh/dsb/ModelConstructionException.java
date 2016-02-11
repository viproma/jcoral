package com.sfh.dsb;


/// Exception type thrown on model construction errors.
public class ModelConstructionException extends Exception
{
    /// Constructs an exception with the given message.
    public ModelConstructionException(String message)
    {
        super(message);
    }
}
