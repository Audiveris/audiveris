//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S c a l e S t e p                                        //
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

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.ScaleBuilder;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.ui.DeltaView;
import org.audiveris.omr.sheet.ui.PixelBoard;
import org.audiveris.omr.sheet.ui.SheetTab;
import org.audiveris.omr.ui.BoardsPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JEditorPane;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;

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

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ScaleStep.class);

    /**
     * Creates a new ScaleStep object.
     */
    public ScaleStep ()
    {
    }

    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Step step,
                           Sheet sheet)
    {
        if (constants.displayDelta.isSet()) {
            // Display delta view
            sheet.getStub().getAssembly().addViewTab(
                    SheetTab.DELTA_TAB,
                    new DeltaView(sheet),
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
        final Scale scale = new ScaleBuilder(sheet).retrieveScale();
        logger.info("{}", scale);
        sheet.setScale(scale);

        // Warn user if beam scale could not be measured precisely
        final Scale.BeamScale beamScale = sheet.getScale().getBeamScale();

        if (beamScale.isExtrapolated()) {
            String title = "Sheet" + sheet.getStub().getNum() + " - Unsufficient beam scaling data";
            logger.info("{}: {}", title, beamScale);

            if (OMR.gui != null) {
                String message
                        = "Raw results of SCALE step give: <b>" + beamScale + "</b>"
                                  + "<p>There is no sufficient data to define beam thickness."
                                  + "<br>Value of <b>" + beamScale.getMain()
                                  + "</b> pixels is just an extrapolated guess."
                                  + "<p>If this sheet does contain beams, you should:<ol>"
                                  + "<li>Measure typical beam thickness value,"
                                  + "<li>Enter a new value via Sheet menu \"Set Scaling Data\""
                                  + " if so needed,"
                                  + "<li>Resume processing via the Step menu."
                                  + "</ol>"
                                  + "<p>OK for pausing sheet transcription here?";
                JEditorPane htmlPane = new JEditorPane("text/html", message);
                htmlPane.setEditable(false);

                if (OMR.gui.displayConfirmation(htmlPane, title, YES_NO_OPTION, WARNING_MESSAGE)) {
                    throw new StepPause("Beam scaling to be checked");
                }
            }
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

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean displayDelta = new Constant.Boolean(
                false,
                "Should we display the Delta view?");
    }
}
