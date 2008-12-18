
package omr.log;

import java.util.logging.LogRecord;

/**
 * Filters out log messages that have
 * null or empty message strings.
 * Should be set as the filter for
 * <code>Handler</code>'s that
 * display messages on screen.
 */
public class LogEmptyMessageFilter
    implements java.util.logging.Filter
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Checks if the record is loggable.
     * In this implementation, returns true
     * if and only if the record's message
     * is non-null and not an empty string.
     */
    public boolean isLoggable (LogRecord record)
    {
        String message = record.getMessage();

        return ((message != null) && (message.length() > 0));
    }
}
