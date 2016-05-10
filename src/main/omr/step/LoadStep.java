//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        L o a d S t e p                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.sheet.Book;
import omr.sheet.Sheet;
import omr.sheet.SheetStub;
import omr.sheet.ui.SheetTab;

import omr.util.Memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

/**
 * Class {@code LoadStep} loads the image for a sheet, from a provided image file.
 *
 * @author Hervé Bitteur
 */
public class LoadStep
        extends AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

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
    public void doit (Sheet sheet)
            throws StepException
    {
        final SheetStub stub = sheet.getStub();
        final Book book = stub.getBook();
        final int number = stub.getNumber();

        BufferedImage image = book.loadSheetImage(number);

        if (image != null) {
            // Threshold on image size
            final int count = image.getWidth() * image.getHeight();
            final int max = constants.maxPixelCount.getValue();

            if (count > max) {
                Memory.gc();

                ///logger.info("Occupied memory: {}", Memory.getValue());
                final String msg = "Too large image: " + String.format("%,d", count)
                                   + " pixels (vs " + String.format("%,d", max) + " max)";
                stub.decideOnRemoval(msg, false); // This may throw StepException
            }

            sheet.setImage(image);
        }
    }

    //-------------//
    // getSheetTab //
    //-------------//
    @Override
    public SheetTab getSheetTab ()
    {
        return SheetTab.PICTURE_TAB;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Integer maxPixelCount = new Constant.Integer(
                "Pixels",
                20000000,
                "Maximum image size, specified in pixel count");
    }
}
