//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        L o a d S t e p                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.Book;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.Collection;

/**
 * Class {@code LoadStep} loads the image for a sheet, from a provided image file.
 *
 * @author Hervé Bitteur
 */
public class LoadStep
        extends AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(LoadStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new LoadStep object.
     */
    public LoadStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // doit //
    //------//
    @Override
    public void doit (Collection<SystemInfo> unused,
                      Sheet sheet)
            throws StepException
    {
        final Book book = sheet.getBook();
        final int index = sheet.getIndex();

        BufferedImage image = book.readImage(index);

        if (image != null) {
            sheet.setImage(image);
        }
    }
}
