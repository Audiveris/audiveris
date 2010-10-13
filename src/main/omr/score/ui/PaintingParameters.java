//----------------------------------------------------------------------------//
//                                                                            //
//                    P a i n t i n g P a r a m e t e r s                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import org.jdesktop.application.AbstractBean;
import org.jdesktop.application.Action;

import java.awt.event.ActionEvent;

/**
 * Class <code>PaintingParameters</code> handles the dynamic parameters related
 * to the painting of any score (slots, voices and marks)
 *
 * @author Hervé Bitteur
 */
public class PaintingParameters
    extends AbstractBean
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Should the marks be painted */
    public static final String MARK_PAINTING = "markPainting";

    /** Should the slots be painted */
    public static final String SLOT_PAINTING = "slotPainting";

    /** Should the voices be painted */
    public static final String VOICE_PAINTING = "voicePainting";

    /** Should the dummy parts be painted */
    public static final String DUMMY_PAINTING = "dummyPainting";

    /** Should the score entities be painted */
    public static final String ENTITY_PAINTING = "entityPainting";

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    public static PaintingParameters getInstance ()
    {
        return Holder.INSTANCE;
    }

    //------------------//
    // setDummyPainting //
    //------------------//
    public void setDummyPainting (boolean value)
    {
        boolean oldValue = constants.dummyPainting.getValue();
        constants.dummyPainting.setValue(value);
        firePropertyChange(
            DUMMY_PAINTING,
            oldValue,
            constants.dummyPainting.getValue());
    }

    //-----------------//
    // isDummyPainting //
    //-----------------//
    public boolean isDummyPainting ()
    {
        return constants.dummyPainting.getValue();
    }

    //-------------------//
    // setEntityPainting //
    //-------------------//
    public void setEntityPainting (boolean value)
    {
        boolean oldValue = constants.entityPainting.getValue();
        constants.entityPainting.setValue(value);
        firePropertyChange(
            ENTITY_PAINTING,
            oldValue,
            constants.entityPainting.getValue());
    }

    //------------------//
    // isEntityPainting //
    //------------------//
    public boolean isEntityPainting ()
    {
        return constants.entityPainting.getValue();
    }

    //-----------------//
    // setMarkPainting //
    //-----------------//
    public void setMarkPainting (boolean value)
    {
        boolean oldValue = constants.markPainting.getValue();
        constants.markPainting.setValue(value);
        firePropertyChange(
            MARK_PAINTING,
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

    //-----------------//
    // setSlotPainting //
    //-----------------//
    public void setSlotPainting (boolean value)
    {
        boolean oldValue = constants.slotPainting.getValue();
        constants.slotPainting.setValue(value);
        firePropertyChange(
            SLOT_PAINTING,
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

    //------------------//
    // setVoicePainting //
    //------------------//
    public void setVoicePainting (boolean value)
    {
        boolean oldValue = constants.voicePainting.getValue();
        constants.voicePainting.setValue(value);
        firePropertyChange(
            VOICE_PAINTING,
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

    //---------------//
    // toggleDummies //
    //---------------//
    /**
     * Action that toggles the display of dummy system parts
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = DUMMY_PAINTING)
    public void toggleDummies (ActionEvent e)
    {
    }

    //----------------//
    // toggleEntities //
    //----------------//
    /**
     * Action that toggles the display of score entities
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = ENTITY_PAINTING)
    public void toggleEntities (ActionEvent e)
    {
    }

    //-------------//
    // toggleMarks //
    //-------------//
    /**
     * Action that toggles the display of computed marks in the score
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = MARK_PAINTING)
    public void toggleMarks (ActionEvent e)
    {
    }

    //-------------//
    // toggleSlots //
    //-------------//
    /**
     * Action that toggles the display of vertical time slots
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = SLOT_PAINTING)
    public void toggleSlots (ActionEvent e)
    {
    }

    //--------------//
    // toggleVoices //
    //--------------//
    /**
     * Action that toggles the display of voices with specific colors
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = VOICE_PAINTING)
    public void toggleVoices (ActionEvent e)
    {
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

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

        /** Should the dummy parts be painted */
        final Constant.Boolean dummyPainting = new Constant.Boolean(
            true,
            "Should the dummy parts be painted");

        /** Should the score entities be painted */
        final Constant.Boolean entityPainting = new Constant.Boolean(
            true,
            "Should the score entities be painted");
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
