//-----------------------------------------------------------------------//
//                                                                       //
//                    O m r L o g g e r F a c t o r y                    //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.util;

import org.apache.log4j.spi.LoggerFactory;

/**
 * Class <code>OmrLoggerFactory</code> is used to force creation of
 * OMR-custom Logger entities (from omr.util.Logger) by the log4j utility.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class OmrLoggerFactory
        implements LoggerFactory
{
    //~ Methods -----------------------------------------------------------

    //-----------------------//
    // makeNewLoggerInstance //
    //-----------------------//
    /**
     * Called by log4j to actually create a logger instance
     *
     * @param name
     *
     * @return a proper Logger instance, compatible with omr application
     */
    public org.apache.log4j.Logger makeNewLoggerInstance (String name)
    {
        return new omr.util.Logger(name);
    }
}
