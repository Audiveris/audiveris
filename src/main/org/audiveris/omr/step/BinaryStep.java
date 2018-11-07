//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       B i n a r y S t e p                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.step;

import ij.process.ByteProcessor;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.image.FilterDescriptor;
import org.audiveris.omr.image.PixelFilter;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Picture.SourceKey;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.ui.SheetAssembly;
import org.audiveris.omr.sheet.ui.SheetTab;
import org.audiveris.omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code BinaryStep} implements <b>BINARY</b> step, which binarizes the initial
 * sheet image, using proper filter, to come up with a black-and-white image.
 *
 * @author Hervé Bitteur
 */
public class BinaryStep
        extends AbstractStep
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BinaryStep.class);

    /**
     * Creates a new BinaryStep object.
     */
    public BinaryStep ()
    {
    }

    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Step step,
                           Sheet sheet)
    {
        // Switch from Picture to Binary display
        SheetAssembly assembly = sheet.getStub().getAssembly();
        assembly.renameTab(SheetTab.PICTURE_TAB.label, SheetTab.BINARY_TAB.label);
    }

    //------//
    // doit //
    //------//
    @Override
    public void doit (Sheet sheet)
            throws StepException
    {
        StopWatch watch = new StopWatch("Binary step for " + sheet.getId());
        watch.start("Getting initial source");

        Picture picture = sheet.getPicture();
        ByteProcessor initial = picture.getSource(SourceKey.INITIAL);

        //
        //        boolean hasGray = hasGray(initial);
        //        logger.info("hasGray: {}", hasGray);
        //
        FilterDescriptor desc = sheet.getStub().getBinarizationFilter().getValue();
        logger.debug("{}", "Binarization");

        PixelFilter filter = desc.getFilter(initial);
        watch.start("Binarize source");

        ByteProcessor binary = filter.filteredImage();

        watch.start("Create binary RunTable");

        RunTableFactory vertFactory = new RunTableFactory(Orientation.VERTICAL);
        RunTable wholeVertTable = vertFactory.createTable(binary);
        picture.setTable(Picture.TableKey.BINARY, wholeVertTable, true);

        // To discard image
        picture.disposeSource(SourceKey.INITIAL);

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

    //---------//
    // hasGray //
    //---------//
    /**
     * Check whether the provided source has at least a gray pixel.
     *
     * @param source the source to inspect
     * @return true if at least one pixel is neither black nor white
     */
    private boolean hasGray (ByteProcessor source)
    {
        for (int i = source.getPixelCount() - 1; i >= 0; i--) {
            int val = source.get(i);

            if ((val != 0) && (val != 255)) {
                return true;
            }
        }

        return false;
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {

        private final Constant.Boolean printWatch = new Constant.Boolean(false,
                                                                         "Should we print out the stop watch?");
    }
}
