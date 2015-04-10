//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              P a i n t i n g P a r a m e t e r s                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.action.ActionManager;

import omr.constant.Constant;
import omr.constant.ConstantSet;
import static omr.score.ui.PaintingParameters.PaintingLayer.*;

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
 * Class {@code PaintingParameters} handles the dynamic parameters related to the
 * painting of any score (layers, slots, voices, ...)
 *
 * @author Hervé Bitteur
 */
public class PaintingParameters
        extends AbstractBean
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(PaintingParameters.class);

    /** Should the annotations be painted. */
    public static final String ANNOTATION_PAINTING = "annotationPainting";

    /** Should the marks be painted. */
    public static final String MARK_PAINTING = "markPainting";

    /** Should the slots be painted. */
    public static final String SLOT_PAINTING = "slotPainting";

    /** A global property name for layers. */
    public static final String LAYER_PAINTING = "layerPainting";

    /** Should the voices be painted. */
    public static final String VOICE_PAINTING = "voicePainting";

    //~ Enumerations -------------------------------------------------------------------------------
    /**
     * Enum {@code PaintingLayer} defines layers to be painted.
     */
    public static enum PaintingLayer
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** Input: image or glyphs. */
        INPUT,
        /** Union of input and output. */
        INPUT_OUTPUT,
        /** Output: score entities. */
        OUTPUT;
        //~ Instance fields ------------------------------------------------------------------------

        /** Icon assigned to layer. */
        private Icon icon;

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Lazily building of layer icon.
         *
         * @return the layer icon
         */
        public Icon getIcon ()
        {
            if (icon == null) {
                ResourceMap resource = Application.getInstance().getContext().getResourceMap(
                        PaintingParameters.class);

                String key = getClass().getSimpleName() + "." + this + ".icon";
                String resourceName = resource.getString(key);
                icon = new ImageIcon(PaintingParameters.class.getResource(resourceName));
            }

            return icon;
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Action for switching layers. (Must be lazily computed) */
    private ApplicationAction layerAction;

    /** Voice painting is chosen to be not persistent. */
    private boolean voicePainting = false;

    /** Layer painting is chosen to be not persistent. */
    private PaintingLayer paintingLayer = INPUT;

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // getInstance //
    //-------------//
    public static PaintingParameters getInstance ()
    {
        return Holder.INSTANCE;
    }

    //------------------//
    // getPaintingLayer //
    //------------------//
    public PaintingLayer getPaintingLayer ()
    {
        return paintingLayer;
    }

    //----------------------//
    // isAnnotationPainting //
    //----------------------//
    public boolean isAnnotationPainting ()
    {
        return constants.annotationPainting.getValue();
    }

    //-----------------//
    // isInputPainting //
    //-----------------//
    public boolean isInputPainting ()
    {
        return (paintingLayer == INPUT) || (paintingLayer == INPUT_OUTPUT);
    }

    //----------------//
    // isMarkPainting //
    //----------------//
    public boolean isMarkPainting ()
    {
        return constants.markPainting.getValue();
    }

    //------------------//
    // isOutputPainting //
    //------------------//
    public boolean isOutputPainting ()
    {
        return (paintingLayer == INPUT_OUTPUT) || (paintingLayer == OUTPUT);
    }

    //----------------//
    // isSlotPainting //
    //----------------//
    public boolean isSlotPainting ()
    {
        return constants.slotPainting.getValue();
    }

    //-----------------//
    // isVoicePainting //
    //-----------------//
    public boolean isVoicePainting ()
    {
        return voicePainting;
    }

    //-----------------------//
    // setAnnotationPainting //
    //-----------------------//
    public void setAnnotationPainting (boolean value)
    {
        boolean oldValue = constants.annotationPainting.getValue();
        constants.annotationPainting.setValue(value);
        firePropertyChange(ANNOTATION_PAINTING, oldValue, constants.annotationPainting.getValue());
    }

    //-----------------//
    // setMarkPainting //
    //-----------------//
    public void setMarkPainting (boolean value)
    {
        boolean oldValue = constants.markPainting.getValue();
        constants.markPainting.setValue(value);
        firePropertyChange(MARK_PAINTING, oldValue, constants.markPainting.getValue());
    }

    //------------------//
    // setPaintingLayer //
    //------------------//
    public void setPaintingLayer (PaintingLayer value)
    {
        PaintingLayer oldValue = getPaintingLayer();
        paintingLayer = value;
        firePropertyChange(LAYER_PAINTING, oldValue, getPaintingLayer());
    }

    //-----------------//
    // setSlotPainting //
    //-----------------//
    public void setSlotPainting (boolean value)
    {
        boolean oldValue = constants.slotPainting.getValue();
        constants.slotPainting.setValue(value);
        firePropertyChange(SLOT_PAINTING, oldValue, constants.slotPainting.getValue());
    }

    //------------------//
    // setVoicePainting //
    //------------------//
    public void setVoicePainting (boolean value)
    {
        boolean oldValue = voicePainting;
        voicePainting = value;
        firePropertyChange(VOICE_PAINTING, oldValue, voicePainting);
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

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //--------//
    // Holder //
    //--------//
    private static interface Holder
    {
        //~ Static fields/initializers -------------------------------------------------------------

        public static final PaintingParameters INSTANCE = new PaintingParameters();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Boolean annotationPainting = new Constant.Boolean(
                true,
                "Should the annotations be painted");

        final Constant.Boolean slotPainting = new Constant.Boolean(
                true,
                "Should the slots be painted");

        final Constant.Boolean markPainting = new Constant.Boolean(
                true,
                "Should the marks be painted");
    }
}
