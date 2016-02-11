package com.sfh.dsb;


/**
 * Exception type thrown if some named entity (typically a slave type, a slave,
 * or a variable) was not found.
 */
public class EntityNotFoundException extends Exception
{
    /// Constructs an exception with the given message.
    public EntityNotFoundException(String message)
    {
        super(message);
    }
}

