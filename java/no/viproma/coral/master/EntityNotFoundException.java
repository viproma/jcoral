package no.viproma.coral.master;


/**
 * Exception type thrown if some named entity (typically a slave type, a slave,
 * or a variable) was not found.
 */
public class EntityNotFoundException extends Exception
{
    /** Constructs an exception with the given message. */
    public EntityNotFoundException(String message)
    {
        super(message);
    }

    // Strongly recommended for Serializable classes, see:
    // https://docs.oracle.com/javase/7/docs/api/java/io/Serializable.html
    private static final long serialVersionUID = 0L;
}

