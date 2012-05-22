//----------------------------------------------------------------------------//
//                                                                            //
//                       F o r m a t t e d R e c o r d                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.log;

import java.util.logging.Level;

/**
 * Class {@code FormattedRecord} is a formatted log record.
 *
 * @author Hervé Bitteur
 */
public class FormattedRecord
{
    //~ Instance fields --------------------------------------------------------

    /** Logging level */
    public final Level level;

    /** Formatted content */
    public final String formattedMessage;

    //~ Constructors -----------------------------------------------------------
    //
    //-----------------//
    // FormattedRecord //
    //-----------------//
    /**
     * Create a FormattedRecord.
     *
     * @param level            logging level
     * @param formattedMessage message already formatted
     */
    public FormattedRecord (Level level,
                            String formattedMessage)
    {
        this.level = level;
        this.formattedMessage = formattedMessage;
    }
}
