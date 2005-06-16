//-----------------------------------------------------------------------//
//                                                                       //
//                       F i l t e r M o n i t o r                       //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.stick;

/**
 * Interface <code>FilterMonitor</code> defines how stick filtering
 * information can be told to a dedicated monitor. This interface is used
 * to pass check results to the board where such info is displayed.
 *
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public interface FilterMonitor
{
    /**
     * Pass the filtering information
     *
     * @param html an HTML-formatted result
     */
    void tellHtml (String html);
}
