//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   V i e w P a r a m e t e r s                                  //
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
package org.audiveris.omr.ui;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import static org.audiveris.omr.ui.ViewParameters.PaintingLayer.*;
import org.audiveris.omr.ui.action.ActionManager;

import org.jdesktop.application.AbstractBean;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationAction;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Class {@code ViewParameters} handles parameters for various views,
 * using properties referenced through their programmatic name to avoid typos.
 *
 * @author Hervé Bitteur
 */
public class ViewParameters
        extends AbstractBean
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ViewParameters.class);

    /** Should the annotations be painted. */
    public static final String ANNOTATION_PAINTING = "annotationPainting";

    /** Should the marks be painted. */
    public static final String MARK_PAINTING = "markPainting";

    /** Should the slots be painted. */
    public static final String SLOT_PAINTING = "slotPainting";

    /** A global property name for layers. */
    public static final String LAYER_PAINTING = "layerPainting";

    /** A global property name for selection mode. */
    public static final String SELECTION_MODE = "selectionMode";

    /** Should the voices be painted. */
    public static final String VOICE_PAINTING = "voicePainting";

    /** Should the errors be painted. */
    public static final String ERROR_PAINTING = "errorPainting";

    /** Should the invalid sheets be displayed. */
    public static final String INVALID_SHEET_DISPLAY = "invalidSheetDisplay";

    /** Should the letter boxes be painted. */
    public static final String LETTER_BOX_PAINTING = "letterBoxPainting";

    /** Should the staff lines be painted. */
    public static final String STAFF_LINE_PAINTING = "staffLinePainting";

    /** Should the staff peaks be painted. */
    public static final String STAFF_PEAK_PAINTING = "staffPeakPainting";

    /** Should the stick lines be painted. */
    public static final String LINE_PAINTING = "linePainting";

    /** Should the stick attachments be painted. */
    public static final String ATTACHMENT_PAINTING = "attachmentPainting";

    /** Should the translation links be painted. */
    public static final String TRANSLATION_PAINTING = "translationPainting";

    /** Should the sentence links be painted. */
    public static final String SENTENCE_PAINTING = "sentencePainting";

    /** Should the inters be painted with grade-based translucency in input view. */
    public static final String TRANSLUCENT_PAINTING = "translucentPainting";

    /** Action for switching layers. (Must be lazily computed) */
    private ApplicationAction layerAction;

    /** Action for switching entity selection. (Must be lazily computed) */
    private ApplicationAction selectionAction;

    /** Voice painting is chosen to be not persistent. */
    private boolean voicePainting = false;

    /** Error painting is chosen to be not persistent. */
    private boolean errorPainting = true;

    /** Layer painting is chosen to be not persistent. */
    private PaintingLayer paintingLayer = INPUT;

    /** Current selection mode. */
    private SelectionMode selectionMode = SelectionMode.MODE_GLYPH;

    /** Staff line painting is chosen to be not persistent. */
    private boolean staffLinePainting = true;

    /** Staff peak painting is chosen to be not persistent. */
    private boolean staffPeakPainting = false;

    //------------------//
    // getPaintingLayer //
    //------------------//
    public PaintingLayer getPaintingLayer ()
    {
        return paintingLayer;
    }

    //------------------//
    // setPaintingLayer //
    //------------------//
    public void setPaintingLayer (PaintingLayer value)
    {
        PaintingLayer oldValue = getPaintingLayer();
        paintingLayer = value;
        firePropertyChange(LAYER_PAINTING, oldValue, value);
    }

    //------------------//
    // getSelectionMode //
    //------------------//
    public SelectionMode getSelectionMode ()
    {
        return selectionMode;
    }

    //------------------//
    // setSelectionMode //
    //------------------//
    public void setSelectionMode (SelectionMode value)
    {
        SelectionMode oldValue = getSelectionMode();
        selectionMode = value;
        firePropertyChange(SELECTION_MODE, oldValue, value);
    }

    //----------------------//
    // isAnnotationPainting //
    //----------------------//
    public boolean isAnnotationPainting ()
    {
        return constants.annotationPainting.getValue();
    }

    //-----------------------//
    // setAnnotationPainting //
    //-----------------------//
    public void setAnnotationPainting (boolean value)
    {
        boolean oldValue = constants.annotationPainting.getValue();
        constants.annotationPainting.setValue(value);
        firePropertyChange(ANNOTATION_PAINTING, oldValue, value);
    }

    //----------------------//
    // isAttachmentPainting //
    //----------------------//
    public boolean isAttachmentPainting ()
    {
        return constants.attachmentPainting.getValue();
    }

    //-----------------------//
    // setAttachmentPainting //
    //-----------------------//
    public void setAttachmentPainting (boolean value)
    {
        boolean oldValue = constants.attachmentPainting.getValue();
        constants.attachmentPainting.setValue(value);
        firePropertyChange(ATTACHMENT_PAINTING, oldValue, value);
    }

    //-----------------//
    // isErrorPainting //
    //-----------------//
    public boolean isErrorPainting ()
    {
        return errorPainting;
    }

    //------------------//
    // setErrorPainting //
    //------------------//
    public void setErrorPainting (boolean value)
    {
        boolean oldValue = errorPainting;
        errorPainting = value;
        firePropertyChange(ERROR_PAINTING, oldValue, value);
    }

    //-----------------//
    // isInputPainting //
    //-----------------//
    public boolean isInputPainting ()
    {
        return (paintingLayer == INPUT) || (paintingLayer == INPUT_OUTPUT);
    }

    //-----------------------//
    // isInvalidSheetDisplay //
    //-----------------------//
    public boolean isInvalidSheetDisplay ()
    {
        return constants.invalidSheetDisplay.getValue();
    }

    //------------------------//
    // setInvalidSheetDisplay //
    //------------------------//
    public void setInvalidSheetDisplay (boolean value)
    {
        boolean oldValue = constants.invalidSheetDisplay.getValue();
        constants.invalidSheetDisplay.setValue(value);
        firePropertyChange(INVALID_SHEET_DISPLAY, oldValue, value);
    }

    //---------------------//
    // isLetterBoxPainting //
    //---------------------//
    public boolean isLetterBoxPainting ()
    {
        return constants.letterBoxPainting.getValue();
    }

    //----------------------//
    // setLetterBoxPainting //
    //----------------------//
    public void setLetterBoxPainting (boolean value)
    {
        boolean oldValue = constants.letterBoxPainting.getValue();
        constants.letterBoxPainting.setValue(value);
        firePropertyChange(LETTER_BOX_PAINTING, oldValue, value);
    }

    //----------------//
    // isLinePainting //
    //----------------//
    public boolean isLinePainting ()
    {
        return constants.linePainting.getValue();
    }

    //-----------------//
    // setLinePainting //
    //-----------------//
    public void setLinePainting (boolean value)
    {
        boolean oldValue = constants.linePainting.getValue();
        constants.linePainting.setValue(value);
        firePropertyChange(LINE_PAINTING, oldValue, value);
    }

    //----------------//
    // isMarkPainting //
    //----------------//
    public boolean isMarkPainting ()
    {
        return constants.markPainting.getValue();
    }

    //-----------------//
    // setMarkPainting //
    //-----------------//
    public void setMarkPainting (boolean value)
    {
        boolean oldValue = constants.markPainting.getValue();
        constants.markPainting.setValue(value);
        firePropertyChange(MARK_PAINTING, oldValue, value);
    }

    //------------------//
    // isOutputPainting //
    //------------------//
    public boolean isOutputPainting ()
    {
        return (paintingLayer == INPUT_OUTPUT) || (paintingLayer == OUTPUT);
    }

    //--------------------//
    // isSentencePainting //
    //--------------------//
    public boolean isSentencePainting ()
    {
        return constants.sentencePainting.getValue();
    }

    //---------------------//
    // setSentencePainting //
    //---------------------//
    public void setSentencePainting (boolean value)
    {
        boolean oldValue = constants.sentencePainting.getValue();
        constants.sentencePainting.setValue(value);
        firePropertyChange(SENTENCE_PAINTING, oldValue, value);
    }

    //----------------//
    // isSlotPainting //
    //----------------//
    public boolean isSlotPainting ()
    {
        return constants.slotPainting.getValue();
    }

    //-----------------//
    // setSlotPainting //
    //-----------------//
    public void setSlotPainting (boolean value)
    {
        boolean oldValue = constants.slotPainting.getValue();
        constants.slotPainting.setValue(value);
        firePropertyChange(SLOT_PAINTING, oldValue, value);
    }

    //---------------------//
    // isStaffLinePainting //
    //---------------------//
    public boolean isStaffLinePainting ()
    {
        return staffLinePainting;
    }

    //----------------------//
    // setStaffLinePainting //
    //----------------------//
    public void setStaffLinePainting (boolean value)
    {
        boolean oldValue = staffLinePainting;
        staffLinePainting = value;
        firePropertyChange(STAFF_LINE_PAINTING, oldValue, value);
    }

    //---------------------//
    // isStaffPeakPainting //
    //---------------------//
    public boolean isStaffPeakPainting ()
    {
        return staffPeakPainting;
    }

    //----------------------//
    // setStaffPeakPainting //
    //----------------------//
    public void setStaffPeakPainting (boolean value)
    {
        boolean oldValue = staffPeakPainting;
        staffPeakPainting = value;
        firePropertyChange(STAFF_PEAK_PAINTING, oldValue, value);
    }

    //-----------------------//
    // isTranslationPainting //
    //-----------------------//
    public boolean isTranslationPainting ()
    {
        return constants.translationPainting.getValue();
    }

    //------------------------//
    // setTranslationPainting //
    //------------------------//
    public void setTranslationPainting (boolean value)
    {
        boolean oldValue = constants.translationPainting.getValue();
        constants.translationPainting.setValue(value);
        firePropertyChange(TRANSLATION_PAINTING, oldValue, value);
    }

    //-----------------------//
    // isTranslucentPainting //
    //-----------------------//
    public boolean isTranslucentPainting ()
    {
        return constants.translucentPainting.getValue();
    }

    //------------------------//
    // setTranslucentPainting //
    //------------------------//
    public void setTranslucentPainting (boolean value)
    {
        boolean oldValue = constants.translucentPainting.getValue();
        constants.translucentPainting.setValue(value);
        firePropertyChange(TRANSLUCENT_PAINTING, oldValue, value);
    }

    //-----------------//
    // isVoicePainting //
    //-----------------//
    public boolean isVoicePainting ()
    {
        return voicePainting;
    }

    //------------------//
    // setVoicePainting //
    //------------------//
    public void setVoicePainting (boolean value)
    {
        boolean oldValue = voicePainting;
        voicePainting = value;
        firePropertyChange(VOICE_PAINTING, oldValue, value);
    }

    //--------------//
    // switchLayers //
    //--------------//
    /**
     * Action that switches among layer combinations in a circular manner.
     *
     * @param e the event that triggered this action
     */
    @Action
    public void switchLayers (ActionEvent e)
    {
        // Compute next layer
        int oldOrd = getPaintingLayer().ordinal();
        int ord = (oldOrd + 1) % PaintingLayer.values().length;
        PaintingLayer layer = PaintingLayer.values()[ord];

        // Update toolbar/menu icon for this dedicated action
        if (layerAction == null) {
            layerAction = ActionManager.getInstance().getActionInstance(this, "switchLayers");
        }

        Icon icon = layer.getIcon();
        layerAction.putValue(AbstractAction.LARGE_ICON_KEY, icon); // For toolbar
        layerAction.putValue(AbstractAction.SMALL_ICON, icon); // For menu

        // Notify new layer
        setPaintingLayer(layer);
    }

    //------------------//
    // switchSelections //
    //------------------//
    /**
     * Action that switches among selection modes in a circular manner.
     *
     * @param e the event that triggered this action
     */
    @Action
    public void switchSelections (ActionEvent e)
    {
        // Compute next selection
        int oldOrd = getSelectionMode().ordinal();
        int ord = (oldOrd + 1) % SelectionMode.values().length;
        SelectionMode mode = SelectionMode.values()[ord];

        // Update toolbar/menu icon for this dedicated action
        if (selectionAction == null) {
            selectionAction = ActionManager.getInstance().getActionInstance(
                    this,
                    "switchSelections");
        }

        Icon icon = mode.getIcon();
        selectionAction.putValue(AbstractAction.LARGE_ICON_KEY, icon); // For toolbar
        selectionAction.putValue(AbstractAction.SMALL_ICON, icon); // For menu

        // Notify new mode
        setSelectionMode(mode);
    }

    //-------------------//
    // toggleAnnotations //
    //-------------------//
    /**
     * Action that toggles the display of annotations in the score
     *
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = ANNOTATION_PAINTING)
    public void toggleAnnotations (ActionEvent e)
    {
    }

    //-------------------//
    // toggleAttachments //
    //-------------------//
    /**
     * Action that toggles the display of attachments in selected sticks
     *
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = ATTACHMENT_PAINTING)
    public void toggleAttachments (ActionEvent e)
    {
    }

    //--------------//
    // toggleErrors //
    //--------------//
    /**
     * Action that toggles the display of errors in the score
     *
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = ERROR_PAINTING)
    public void toggleErrors (ActionEvent e)
    {
    }

    //---------------------//
    // toggleInvalidSheets //
    //---------------------//
    /**
     * Action that toggles the display of invalid sheets
     *
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = INVALID_SHEET_DISPLAY)
    public void toggleInvalidSheets (ActionEvent e)
    {
    }

    //---------------//
    // toggleLetters //
    //---------------//
    /**
     * Action that toggles the display of letter boxes in selected glyphs
     *
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = LETTER_BOX_PAINTING)
    public void toggleLetters (ActionEvent e)
    {
    }

    //-------------//
    // toggleLines //
    //-------------//
    /**
     * Action that toggles the display of mean line in selected sticks
     *
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = LINE_PAINTING)
    public void toggleLines (ActionEvent e)
    {
    }

    //-------------//
    // toggleMarks //
    //-------------//
    /**
     * Action that toggles the display of computed marks in the score
     *
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = MARK_PAINTING)
    public void toggleMarks (ActionEvent e)
    {
    }

    //-----------------//
    // toggleSentences //
    //-----------------//
    /**
     * Action that toggles the display of sentences in selected glyphs
     *
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = SENTENCE_PAINTING)
    public void toggleSentences (ActionEvent e)
    {
    }

    //-------------//
    // toggleSlots //
    //-------------//
    /**
     * Action that toggles the display of vertical time slots
     *
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = SLOT_PAINTING)
    public void toggleSlots (ActionEvent e)
    {
    }

    //------------------//
    // toggleStaffLines //
    //------------------//
    /**
     * Action that toggles the display of staff lines
     *
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = STAFF_LINE_PAINTING)
    public void toggleStaffLines (ActionEvent e)
    {
    }

    //------------------//
    // toggleStaffPeaks //
    //------------------//
    /**
     * Action that toggles the display of staff peaks
     *
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = STAFF_PEAK_PAINTING)
    public void toggleStaffPeaks (ActionEvent e)
    {
    }

    //--------------------//
    // toggleTranslations //
    //--------------------//
    /**
     * Action that toggles the display of translations in selected glyphs
     *
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = TRANSLATION_PAINTING)
    public void toggleTranslations (ActionEvent e)
    {
    }

    //--------------------//
    // toggleTranslucency //
    //--------------------//
    /**
     * Action that toggles the display of inters with translucency.
     *
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = TRANSLUCENT_PAINTING)
    public void toggleTranslucency (ActionEvent e)
    {
    }

    //--------------//
    // toggleVoices //
    //--------------//
    /**
     * Action that toggles the display of voices with specific colors
     *
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = VOICE_PAINTING)
    public void toggleVoices (ActionEvent e)
    {
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of this class in application.
     *
     * @return the instance
     */
    public static ViewParameters getInstance ()
    {
        return LazySingleton.INSTANCE;
    }

    //---------------//
    // LazySingleton //
    //---------------//
    private static class LazySingleton
    {

        static final ViewParameters INSTANCE = new ViewParameters();
    }

    /**
     * Enum {@code PaintingLayer} defines layers to be painted.
     */
    public static enum PaintingLayer
    {
        /** Input: image or glyphs. */
        INPUT,
        /** Union of input and output. */
        INPUT_OUTPUT,
        /** Output: score entities. */
        OUTPUT;

        /** Icon assigned to layer. */
        private Icon icon;

        /**
         * Lazily building of layer icon.
         *
         * @return the layer icon
         */
        public Icon getIcon ()
        {
            if (icon == null) {
                ResourceMap resource = Application.getInstance().getContext().getResourceMap(
                        ViewParameters.class);

                String key = getClass().getSimpleName() + "." + this + ".icon";
                String resourceName = resource.getString(key);
                icon = new ImageIcon(ViewParameters.class.getResource(resourceName));
            }

            return icon;
        }
    }

    /**
     * Enum {@code SelectionMode} defines type of entities to be selected.
     */
    public static enum SelectionMode
    {
        MODE_GLYPH,
        MODE_INTER,
        MODE_SECTION;

        /** Icon assigned to mode. */
        private Icon icon;

        /**
         * Lazily building of mode icon.
         *
         * @return the mode icon
         */
        public Icon getIcon ()
        {
            if (icon == null) {
                ResourceMap resource = Application.getInstance().getContext().getResourceMap(
                        ViewParameters.class);
                String key = getClass().getSimpleName() + "." + this + ".icon";
                String resourceName = resource.getString(key);
                icon = new ImageIcon(ViewParameters.class.getResource(resourceName));
            }

            return icon;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean annotationPainting = new Constant.Boolean(
                true,
                "Should the annotations be painted");

        private final Constant.Boolean slotPainting = new Constant.Boolean(
                true,
                "Should the slots be painted");

        private final Constant.Boolean markPainting = new Constant.Boolean(
                true,
                "Should the marks be painted");

        private final Constant.Boolean invalidSheetDisplay = new Constant.Boolean(
                true,
                "Should the invalid sheets be displayed");

        private final Constant.Boolean letterBoxPainting = new Constant.Boolean(
                true,
                "Should the letter boxes be painted");

        private final Constant.Boolean linePainting = new Constant.Boolean(
                false,
                "Should the stick lines be painted");

        private final Constant.Boolean attachmentPainting = new Constant.Boolean(
                false,
                "Should the staff & glyph attachments be painted");

        private final Constant.Boolean translationPainting = new Constant.Boolean(
                true,
                "Should the glyph translations links be painted");

        private final Constant.Boolean sentencePainting = new Constant.Boolean(
                true,
                "Should the sentence words links be painted");

        private final Constant.Boolean translucentPainting = new Constant.Boolean(
                true,
                "Should the inters be painted with grade-based translucency");
    }
}
