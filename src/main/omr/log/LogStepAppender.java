//----------------------------------------------------------------------------//
//                                                                            //
//                        L o g S t e p A p p e n d e r                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.log;

import omr.step.Stepping;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * Class {@code LogStepAppender} uses the flow of logging messages
 * (assumed to be filtered on INFO level at least) to notify a slight
 * progress.
 * Filtering on level is performed in the logging configuration file (if any).
 *
 * @author Hervé Bitteur
 */
public class LogStepAppender
        extends AppenderBase<ILoggingEvent>
{
    //~ Methods ----------------------------------------------------------------

    @Override
    protected void append (ILoggingEvent event)
    {
        if (event.getLevel()
                .toInt() >= Level.INFO_INT) {
            Stepping.notifyProgress();
        }
    }
}
