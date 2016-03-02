import com.sfh.util.ProcessCapture;
import com.sfh.util.ProcessOutput;


class ProcessCaptureTest
{
    public static void main(String[] args) throws Exception
    {
        Process process = Runtime.getRuntime().exec("dir");
        ProcessCapture capture = new ProcessCapture(process);
        ProcessOutput output = capture.call();
        assert(!output.getOutput().isEmpty());
        assert(output.getErrors().isEmpty());
        //System.out.println("Output:\n" + output.getOutput());
        //System.out.println("Errors:\n" + output.getErrors());
        int exitCode = process.waitFor();
        assert(exitCode == 0);
        try { capture.call(); assert(false); } catch (IllegalStateException e) { }

        process = Runtime.getRuntime().exec("dir thisDirProbablyDoesntExist");
        output = (new ProcessCapture(process)).call();
        assert(output.getOutput().isEmpty());
        assert(!output.getErrors().isEmpty());
        //System.out.println("Output:\n" + output.getOutput());
        //System.out.println("Errors:\n" + output.getErrors());
        exitCode = process.waitFor();
        assert(exitCode != 0);
    }
}
