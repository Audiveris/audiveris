//----------------------------------------------------------------------------//
//                                                                            //
//                 L o g S t e p M o n i t o r H a n d l e r                  //
//                                                                            //
//  Copyright (C) Brenton Partridge 2007. All rights reserved.                //
//  This software is released under the GNU General Public License.           //
//  Contact herve.bitteur@laposte.net to report bugs & suggestions.           //
//----------------------------------------------------------------------------//
//
package omr.step;

import java.util.logging.*;

import omr.util.LogLevelsFilter;

/**
 * <p>Class <code>LogStepMonitorHandler</code> provides a connection between
 * the default step monitor's progress bar and any INFO messages
 * logged. Info messages will cause the bar to increase its value by a
 * small proportion.</p>
 *
 * <p>Note that, while zero-length messages are not shown by the
 * GUI log pane, they increment the step monitor, so empty-string
 * messages can be used to force an increment. Also note that
 * these animations are only shown when a Step is ongoing.</p>
 *
 * <p>As a <code>Handler</code>, it should be added to the highest-level
 * logger instance, either programmatically or in a properties file.</p>
 *
 * @author Brenton Partridge
 * @version $Id$
 */
public class LogStepMonitorHandler
    extends Handler
{
    //~ Constructors -----------------------------------------------------------

    public LogStepMonitorHandler ()
    {
        setFilter(new LogLevelsFilter(Level.INFO));
    }

    //~ Methods ----------------------------------------------------------------

    //-------//
    // close //
    //-------//
    /**
     * Called when the handler must be closed. It's a void routine for the time
     * being.
     */
    @Override
    public void close ()
    {
    }

    //-------//
    // close //
    //-------//
    /**
     * Flush any buffered output. It's a void routine for the time being.
     */
    @Override
    public void flush ()
    {
    }

    //---------//
    // publish //
    //---------//
    @Override
    public void publish (final LogRecord record)
    {
        if (isLoggable(record)) {
            StepMonitor monitor = Step.getMonitor();

            if (monitor != null) {
                monitor.animate();
            }
        }
    }
}
