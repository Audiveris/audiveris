//----------------------------------------------------------------------------//
//                                                                            //
//                   L o g g i n g O u t p u t S t r e a m                    //
//                                                                            //
//----------------------------------------------------------------------------//
package omr.log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Class {@code LoggingOutputStream} defines an OutputStream that writes
 * contents to a Logger upon each call to flush().
 * <br/>
 * <a href="http://blogs.oracle.com/nickstephen/entry/java_redirecting_system_out_and">See blog of Nick Stephen</a>
 * 
 * @author Nick Stephen 
 */
public class LoggingOutputStream
    extends ByteArrayOutputStream
{
    //~ Instance fields --------------------------------------------------------

    private String lineSeparator;
    private Logger logger;
    private Level  level;

    //~ Constructors -----------------------------------------------------------

    /**
     * Constructor
     * @param logger Logger to write to
     * @param level Level at which to write the log message
     */
    public LoggingOutputStream (Logger logger,
                                Level  level)
    {
        super();
        this.logger = logger;
        this.level = level;
        lineSeparator = System.getProperty("line.separator");
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * upon flush(), write the existing contents of the OutputStream to the
     * logger as a log record.
     * @throws java.io.IOException in case of error
     */
    @Override
    public void flush ()
        throws IOException
    {
        String record;

        synchronized (this) {
            super.flush();
            record = this.toString();
            super.reset();
        }

        if ((record.length() == 0) || record.equals(lineSeparator)) {
            // avoid empty records
            return;
        }

        logger.logp(level, "", "", record);
    }
}
