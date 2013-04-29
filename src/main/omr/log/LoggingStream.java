//----------------------------------------------------------------------------//
//                                                                            //
//                          L o g g i n g S t r e a m                         //
//                                                                            //
//----------------------------------------------------------------------------//
package omr.log;

import omr.WellKnowns;

import ch.qos.logback.classic.Level;

import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Class {@code LoggingStream} defines an OutputStream that writes
 * contents to a Logger upon each call to flush().
 * <br/>
 * <a
 * href="http://blogs.oracle.com/nickstephen/entry/java_redirecting_system_out_and">See
 * blog of Nick Stephen</a>
 *
 * @author Nick Stephen
 */
public class LoggingStream
        extends ByteArrayOutputStream
{
    //~ Instance fields --------------------------------------------------------

    private final Logger logger;

    private final Level level;

    //~ Constructors -----------------------------------------------------------
    /**
     * Constructor
     *
     * @param logger Logger to write to
     * @param level  Level at which to write the log message
     */
    public LoggingStream (Logger logger,
                          Level level)
    {
        super();
        this.logger = logger;
        this.level = level;
    }

    //~ Methods ----------------------------------------------------------------
    /**
     * Upon flush(), write the existing contents of the OutputStream to
     * the logger as a log record.
     *
     * @throws java.io.IOException in case of error
     */
    @Override
    public void flush ()
            throws IOException
    {
        String record;

        synchronized (this) {
            super.flush();
            record = this.toString(WellKnowns.FILE_ENCODING);
            super.reset();
        }

        // Avoid empty records
        if ((record.length() == 0) || record.equals(WellKnowns.LINE_SEPARATOR)) {
            return;
        }

        // Write to the actual logger
        ch.qos.logback.classic.Logger theLogger = (ch.qos.logback.classic.Logger) logger;
        theLogger.log(null, null, level.toInt(), record, null, null);
    }
}
