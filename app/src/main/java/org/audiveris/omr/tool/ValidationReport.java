//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   V a l i d a t i o n R e p o r t                              //
//------------------------------------------------------------------------------------------------//
package org.audiveris.omr.tool;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates validation errors and warnings for an OMR project.
 */
public class ValidationReport
{
    public static class Entry
    {
        public enum Severity
        {
            ERROR,
            WARNING,
            INFO
        }

        public final Severity severity;

        public final String message;

        public Entry (Severity severity,
                       String message)
        {
            this.severity = severity;
            this.message = message;
        }

        @Override
        public String toString ()
        {
            return severity + ": " + message;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    private int errorCount = 0;

    private int warningCount = 0;

    public void error (String message)
    {
        entries.add(new Entry(Entry.Severity.ERROR, message));
        errorCount++;
    }

    public void warning (String message)
    {
        entries.add(new Entry(Entry.Severity.WARNING, message));
        warningCount++;
    }

    public void info (String message)
    {
        entries.add(new Entry(Entry.Severity.INFO, message));
    }

    public boolean hasErrors ()
    {
        return errorCount > 0;
    }

    public boolean hasWarnings ()
    {
        return warningCount > 0;
    }

    public List<Entry> getEntries ()
    {
        return entries;
    }

    public int getErrorCount ()
    {
        return errorCount;
    }

    public int getWarningCount ()
    {
        return warningCount;
    }

    public boolean isEmpty ()
    {
        return entries.isEmpty();
    }
}
