//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        G r i d S t e p                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sheet.grid;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.sheet.Picture;
import omr.sheet.Sheet;
import omr.sheet.ui.ImageView;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.ScrollImageView;
import omr.sheet.ui.SheetTab;

import omr.step.AbstractStep;
import omr.step.Step;
import omr.step.StepException;

import omr.ui.BoardsPane;

import omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code GridStep} implements <b>GRID</b> step, which retrieves all staves and
 * systems of a sheet.
 *
 * @author Hervé Bitteur
 */
public class GridStep
        extends AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(GridStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new GridStep object.
     */
    public GridStep ()
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
        sheet.getSymbolsEditor().refresh();

        if (constants.displayNoStaff.isSet()) {
            sheet.getStub().getAssembly().addViewTab(
                    SheetTab.NO_STAFF_TAB,
                    new ScrollImageView(
                            sheet,
                            new ImageView(
                                    sheet.getPicture().getSource(Picture.SourceKey.NO_STAFF).getBufferedImage())),
                    new BoardsPane(new PixelBoard(sheet)));
        }
    }

    //------//
    // doit //
    //------//
    @Override
    public void doit (Sheet sheet)
            throws StepException
    {
        StopWatch watch = new StopWatch("GridStep");
        watch.start("GridBuilder");
        new GridBuilder(sheet).buildInfo();

        watch.start("StaffLineCleaner");
        new StaffLineCleaner(sheet).process();

        sheet.getStub().getBook().updateScores(sheet.getStub());

        ///watch.print();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean displayNoStaff = new Constant.Boolean(
                false,
                "Should we display the staff-free image?");
    }
}
