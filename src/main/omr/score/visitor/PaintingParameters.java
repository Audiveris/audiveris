//----------------------------------------------------------------------------//
//                                                                            //
//                    P a i n t i n g P a r a m e t e r s                     //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.score.ui.ScoreOrientation;

import omr.sheet.SheetsManager;

import omr.util.Worker;

import org.jdesktop.application.AbstractBean;
import org.jdesktop.application.Action;

import java.awt.event.ActionEvent;

/**
 * Class <code>PaintingParameters</code> handles the dynamic parameters related
 * to the painting of any score (slots, voices and marks)
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class PaintingParameters
    extends AbstractBean
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    public static PaintingParameters getInstance ()
    {
        return Holder.INSTANCE;
    }

    //-----------------//
    // setMarkPainting //
    //-----------------//
    public void setMarkPainting (boolean value)
    {
        boolean oldValue = constants.markPainting.getValue();
        constants.markPainting.setValue(value);
        firePropertyChange(
            "markPainting",
            oldValue,
            constants.markPainting.getValue());
    }

    //----------------//
    // isMarkPainting //
    //----------------//
    public boolean isMarkPainting ()
    {
        return constants.markPainting.getValue();
    }

    //---------------------//
    // getScoreOrientation //
    //---------------------//
    public ScoreOrientation getScoreOrientation ()
    {
        return constants.verticalLayout.getValue() ? ScoreOrientation.VERTICAL
               : ScoreOrientation.HORIZONTAL;
    }

    //-----------------//
    // setSlotPainting //
    //-----------------//
    public void setSlotPainting (boolean value)
    {
        boolean oldValue = constants.slotPainting.getValue();
        constants.slotPainting.setValue(value);
        firePropertyChange(
            "slotPainting",
            oldValue,
            constants.slotPainting.getValue());
    }

    //----------------//
    // isSlotPainting //
    //----------------//
    public boolean isSlotPainting ()
    {
        return constants.slotPainting.getValue();
    }

    //-------------------//
    // setVerticalLayout //
    //-------------------//
    public void setVerticalLayout (boolean value)
    {
        boolean oldValue = constants.verticalLayout.getValue();
        constants.verticalLayout.setValue(value);
        firePropertyChange(
            "verticalLayout",
            oldValue,
            constants.verticalLayout.getValue());
    }

    //------------------//
    // isVerticalLayout //
    //------------------//
    public boolean isVerticalLayout ()
    {
        return constants.verticalLayout.getValue();
    }

    //------------------//
    // setVoicePainting //
    //------------------//
    public void setVoicePainting (boolean value)
    {
        boolean oldValue = constants.voicePainting.getValue();
        constants.voicePainting.setValue(value);
        firePropertyChange(
            "voicePainting",
            oldValue,
            constants.voicePainting.getValue());
    }

    //-----------------//
    // isVoicePainting //
    //-----------------//
    public boolean isVoicePainting ()
    {
        return constants.voicePainting.getValue();
    }

    //--------------//
    // toggleLayout //
    //--------------//
    /**
     * Action that toggles the layout of the systems
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = "verticalLayout")
    public void toggleLayout (ActionEvent e)
    {
        new Worker<Void>() {
                @Override
                public Void construct ()
                {
                    SheetsManager.getInstance()
                                 .setScoreOrientation(getScoreOrientation());

                    return null;
                }
            }.start();
    }

    //-------------//
    // toggleMarks //
    //-------------//
    /**
     * Action that toggles the display of computed marks in the score
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = "markPainting")
    public void toggleMarks (ActionEvent e)
    {
        ScorePainter.repaintDisplay();
    }

    //-------------//
    // toggleSlots //
    //-------------//
    /**
     * Action that toggles the display of vertical time slots
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = "slotPainting")
    public void toggleSlots (ActionEvent e)
    {
        ScorePainter.repaintDisplay();
    }

    //--------------//
    // toggleVoices //
    //--------------//
    /**
     * Action that toggles the display of voices with specific colors
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = "voicePainting")
    public void toggleVoices (ActionEvent e)
    {
        ScorePainter.repaintDisplay();
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Should the systems be painted vertically */
        final Constant.Boolean verticalLayout = new Constant.Boolean(
            true,
            "Should the systems be painted vertically");

        /** Should the slots be painted */
        final Constant.Boolean slotPainting = new Constant.Boolean(
            true,
            "Should the slots be painted");

        /** Should the voices be painted */
        final Constant.Boolean voicePainting = new Constant.Boolean(
            false,
            "Should the voices be painted");

        /** Should the marks be painted */
        final Constant.Boolean markPainting = new Constant.Boolean(
            true,
            "Should the marks be painted");
    }

    //--------//
    // Holder //
    //--------//
    private static class Holder
    {
        //~ Static fields/initializers -----------------------------------------

        public static final PaintingParameters INSTANCE = new PaintingParameters();
    }
}
