//----------------------------------------------------------------------------//
//                                                                            //
//                            V i e w A p p e n d e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code ViewAppender} is a log appender that appends the
 * logging messages to the BundleView.
 *
 * @author Hervé Bitteur
 */
public class ViewAppender
    extends AppenderBase<ILoggingEvent>
{
    //~ Instance fields --------------------------------------------------------

    /** The installer bundle view. */
    private BundleView view;

    /** Needed to store messages until the view gets available. */
    private final List<ILoggingEvent> backlog = new ArrayList<ILoggingEvent>();

    //~ Methods ----------------------------------------------------------------

    //--------//
    // append //
    //--------//
    @Override
    protected void append (ILoggingEvent event)
    {
        if (!isReady()) {
            backlog.add(event);
        } else {
            if (!backlog.isEmpty()) {
                for (ILoggingEvent evt : backlog) {
                    publish(evt);
                }

                backlog.clear();
            }

            publish(event);
        }
    }

    //---------//
    // isReady //
    //---------//
    private boolean isReady ()
    {
        if (Installer.getBundle() == null) {
            return false;
        }

        view = Installer.getBundle()
                        .getView();

        return view != null;
    }

    //---------//
    // publish //
    //---------//
    private void publish (ILoggingEvent event)
    {
        Level level = event.getLevel();
        view.publishMessage(level, event.getFormattedMessage());
    }
}
