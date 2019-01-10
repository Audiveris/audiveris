//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        L o a d S t e p                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.ui.SheetTab;
import org.audiveris.omr.util.Memory;

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

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(LoadStep.class);

    /**
     * Creates a new LoadStep object.
     */
    public LoadStep ()
    {
    }

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
                                           + " pixels (vs "
                                           + String.format("%,d", max)
                                           + " max)";
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
        return SheetTab.INITIAL_TAB;
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Integer maxPixelCount = new Constant.Integer(
                "Pixels",
                20_000_000,
                "Maximum image size, specified in pixel count");
    }
}
