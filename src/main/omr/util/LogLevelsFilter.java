package omr.util;

import java.util.logging.LogRecord;

/**
 * Filters out log messages that are
 * of different types than the ones
 * specified in the constructor.
 */
public class LogLevelsFilter
    implements java.util.logging.Filter
{
    private final java.util.logging.Level[] levels;

    public LogLevelsFilter (java.util.logging.Level... levels)
    {
        this.levels = levels;
    }

    /**
     * Checks if the record is loggable.
     * In this implementation, returns true
     * if and only if the record's message
     * is non-null and not an empty string.
     */
    public boolean isLoggable (LogRecord record)
    {
        for (java.util.logging.Level level : levels) {
            if (record.getLevel()
                      .equals(level)) {
                return true;
            }
        }

        return false;
    }
}