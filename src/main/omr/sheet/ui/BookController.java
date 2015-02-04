//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  B o o k C o n t r o l l e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.sheet.Book;
import omr.sheet.Sheet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code BookController} is only a convenient way to retrieve the current book
 * (which contains the sheet currently selected by the user).
 *
 * @author Hervé Bitteur
 */
public class BookController
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BookController.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * No meant to be instantiated
     */
    private BookController ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // getCurrentBook //
    //----------------//
    /**
     * Convenient method to get the current book instance.
     *
     * @return the current book instance, or null
     */
    public static Book getCurrentBook ()
    {
        Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet != null) {
            return sheet.getBook();
        }

        return null;
    }
}
