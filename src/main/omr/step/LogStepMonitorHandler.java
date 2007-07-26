//----------------------------------------------------------------------------//
//                                                                            //
//                 L o g S t e p M o n i t o r H a n d l e r                  //
//                                                                            //
//  Copyright (C) Brenton Partridge 2007. All rights reserved.                //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.step;

import java.util.logging.*;

/**
 * Class <code>LogStepMonitorHandler</code> provides a connection between
 * the default step monitor's progress bar and any INFO messages
 * logged. Info messages will cause the bar to increase its value by a
 * small proportion.
 *
 * As a <code>Handler</code>, it should be added to the highest-level
 * logger instance, either programmatically or in a properties file.
 *
 * @author Brenton Partridge
 */
public class LogStepMonitorHandler
    extends Handler
{
	public static final String FORCE = "Force StepMonitor Increment";
	
    //~ Methods ----------------------------------------------------------------

    //-------//
    // close //
    //-------//
    @Override
	public void close ()
        throws SecurityException
    {
    }

    //-------//
    // flush //
    //-------//
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
        if (record.getLevel().equals(Level.INFO) ||
        	record.getMessage().equals(FORCE)) {
            StepMonitor monitor = Step.getMonitor();
            if (monitor != null) {
                monitor.animate();
            }
        }
    }
}
