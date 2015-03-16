//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S c a l e S t e p                                        //
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

import omr.sheet.Scale;
import omr.sheet.ScaleBuilder;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.ui.DeltaView;
import omr.sheet.ui.PixelBoard;

import omr.ui.BoardsPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Class {@code ScaleStep} implements <b>SCALE</b> step, which determines the general
 * scaling informations of a sheet, based essentially on the mean distance between staff
 * lines.
 *
 * @author Hervé Bitteur
 */
public class ScaleStep
        extends AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ScaleStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ScaleStep object.
     */
    public ScaleStep ()
    {
        super(
                Steps.SCALE,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                BINARY_TAB,
                "Compute sheet scale");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Sheet sheet)
    {
        if (constants.displayDelta.isSet()) {
            // Display delta view
            sheet.getAssembly().addViewTab(
                    Step.DELTA_TAB,
                    new DeltaView(sheet),
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
        Scale scale = new ScaleBuilder(sheet).retrieveScale();

        logger.info("{}{}", sheet.getLogPrefix(), scale);
        sheet.setScale(scale);
        sheet.getBench().recordScale(scale);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Boolean displayDelta = new Constant.Boolean(
                false,
                "Should we display the Delta view?");
    }
}
