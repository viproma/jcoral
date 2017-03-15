package no.viproma.util;


/** Program output captured by {@link ProcessCapture}. */
public class ProcessOutput
{
    ProcessOutput(String o, String e)
    {
        output_ = o;
        errors_ = e;
    }

    /** Returns whatever the program wrote to its standard output stream. */
    public String getOutput() { return output_; }

    /** Returns whatever the program wrote to its standard error stream. */
    public String getErrors() { return errors_; }

    private String output_;
    private String errors_;
}
