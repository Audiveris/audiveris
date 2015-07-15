//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       B i n a r y S t e p                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphFactory;

import omr.image.FilterDescriptor;
import omr.image.PixelFilter;

import omr.run.Orientation;
import omr.run.RunTable;
import omr.run.RunTableFactory;

import omr.sheet.Picture;
import omr.sheet.Picture.SourceKey;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.ui.SheetAssembly;
import omr.sheet.ui.SheetTab;

import omr.util.StopWatch;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Class {@code BinaryStep} implements <b>BINARY</b> step, which binarizes the initial
 * sheet image, using proper filter, to come up with a black & white image.
 *
 * @author Hervé Bitteur
 */
public class BinaryStep
        extends AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BinaryStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BinaryStep object.
     */
    public BinaryStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Step step,
                           Sheet sheet)
    {
        // Switch from Picture to Binary display
        SheetAssembly assembly = sheet.getAssembly();
        assembly.renameTab(SheetTab.PICTURE_TAB.label, SheetTab.BINARY_TAB.label);
    }

    //------//
    // doit //
    //------//
    @Override
    public void doit (Collection<SystemInfo> unused,
                      Sheet sheet)
            throws StepException
    {
        StopWatch watch = new StopWatch("Binary step for " + sheet.getId());
        watch.start("Getting initial source");

        Picture picture = sheet.getPicture();
        ByteProcessor initial = picture.getSource(SourceKey.INITIAL);
        FilterDescriptor desc = sheet.getFilterParam().getTarget();
        logger.debug("{}{}", sheet.getLogPrefix(), "Binarization");
        sheet.getFilterParam().setActual(desc);

        PixelFilter filter = desc.getFilter(initial);
        watch.start("Binarize source");

        ByteProcessor binary = filter.filteredImage();

        watch.start("Create binary RunTable");

        RunTableFactory vertFactory = new RunTableFactory(Orientation.VERTICAL);
        RunTable wholeVertTable = vertFactory.createTable(binary);
        picture.setTable(Picture.TableKey.BINARY, wholeVertTable);

        // To discard image
        picture.disposeSource(SourceKey.INITIAL);

        watch.start("marked");
        GlyphFactory.createGlyphs(wholeVertTable, null);

        if (constants.printWatch.isSet()) {
            watch.print();
        }
    }

    //-------------//
    // getSheetTab //
    //-------------//
    @Override
    public SheetTab getSheetTab ()
    {
        return SheetTab.BINARY_TAB;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");
    }
}
