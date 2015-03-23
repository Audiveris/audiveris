//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        G r i d S t e p                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.ui.SymbolsEditor;

import omr.grid.GridBuilder;
import omr.grid.LagWeaver;

import omr.sheet.Picture;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.ui.ImageView;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.ScrollImageView;
import omr.sheet.ui.SheetTab;

import omr.ui.BoardsPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

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
        SymbolsEditor editor = sheet.getSymbolsEditor();

        if (editor != null) {
            editor.refresh();
        }

        if (constants.displayNoStaff.isSet()) {
            sheet.getAssembly().addViewTab(
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
    public void doit (Collection<SystemInfo> unused,
                      Sheet sheet)
            throws StepException
    {
        sheet.createNest();
        new GridBuilder(sheet).buildInfo();

        // Purge sections & runs of staff lines from hLag
        // Cross-connect vertical & remaining horizontal sections
        new LagWeaver(sheet).buildInfo();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Boolean displayNoStaff = new Constant.Boolean(
                false,
                "Should we display the staff-free image?");
    }
}
