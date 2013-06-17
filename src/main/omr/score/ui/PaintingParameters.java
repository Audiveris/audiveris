//----------------------------------------------------------------------------//
//                                                                            //
//                    P a i n t i n g P a r a m e t e r s                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.Main;

import omr.action.ActionManager;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import org.jdesktop.application.AbstractBean;
import org.jdesktop.application.Action;
import org.jdesktop.application.ApplicationAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Class {@code PaintingParameters} handles the dynamic parameters
 * related to the painting of any score (layers, slots, voices, ...)
 *
 * @author Hervé Bitteur
 */
public class PaintingParameters
        extends AbstractBean
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            PaintingParameters.class);

    /** Should the annotations be painted */
    public static final String ANNOTATION_PAINTING = "annotationPainting";

    /** Should the marks be painted */
    public static final String MARK_PAINTING = "markPainting";

    /** Should the slots be painted */
    public static final String SLOT_PAINTING = "slotPainting";

    /** A global property name for layers */
    public static final String LAYER_PAINTING = "layerPainting";

    /** Should the voices be painted */
    public static final String VOICE_PAINTING = "voicePainting";

    //~ Instance fields --------------------------------------------------------
    //
    /** Icons for layers combinations. (Must be lazily computed) */
    private Map<PaintingLayer, Icon> layerIcons;

    /** Action for switching layers. (Must be lazily computed) */
    private ApplicationAction layerAction;

    /** Voice painting is chosen to be not persistent. */
    private boolean voicePainting = false;

    /** Layer painting is chosen to be not persistent. */
    private PaintingLayer paintingLayer = PaintingLayer.INPUT;

    //~ Methods ----------------------------------------------------------------
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
        return (getPaintingLayer() == PaintingLayer.INPUT)
               || (getPaintingLayer() == PaintingLayer.INPUT_OUTPUT);
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
        return (getPaintingLayer() == PaintingLayer.OUTPUT)
               || (getPaintingLayer() == PaintingLayer.INPUT_OUTPUT);
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
        // return constants.voicePainting.getValue();
        return voicePainting;
    }

    //-----------------------//
    // setAnnotationPainting //
    //-----------------------//
    public void setAnnotationPainting (boolean value)
    {
        boolean oldValue = constants.annotationPainting.getValue();
        constants.annotationPainting.setValue(value);
        firePropertyChange(
                ANNOTATION_PAINTING,
                oldValue,
                constants.annotationPainting.getValue());
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

    //------------------//
    // setPaintingLayer //
    //------------------//
    public void setPaintingLayer (PaintingLayer value)
    {
        PaintingLayer oldValue = getPaintingLayer();
        /// constants.paintingLayer.setValue(value); // For persistency
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
        firePropertyChange(
                SLOT_PAINTING,
                oldValue,
                constants.slotPainting.getValue());
    }

    //------------------//
    // setVoicePainting //
    //------------------//
    public void setVoicePainting (boolean value)
    {
        //        boolean oldValue = constants.voicePainting.getValue();
        //        constants.voicePainting.setValue(value);
        //        firePropertyChange(
        //            VOICE_PAINTING,
        //            oldValue,
        //            constants.voicePainting.getValue());
        boolean oldValue = voicePainting;
        voicePainting = value;
        firePropertyChange(VOICE_PAINTING, oldValue, voicePainting);
    }

    //--------------//
    // switchLayers //
    //--------------//
    /**
     * Action that switches among layer combinations
     *
     * @param e the event that triggered this action
     */
    @Action
    public void switchLayers (ActionEvent e)
    {
        // Compute new layer
        int oldOrd = getPaintingLayer()
                .ordinal();
        int ord = (oldOrd + 1) % PaintingLayer.values().length;
        PaintingLayer layer = PaintingLayer.values()[ord];

        // Update toolbar/menu icon
        Icon icon = getLayerIcons()
                .get(layer);
        ApplicationAction action = getLayerAction();
        action.putValue(AbstractAction.LARGE_ICON_KEY, icon); // toolbar
        action.putValue(AbstractAction.SMALL_ICON, icon); // menu

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

    //----------------//
    // getLayerAction //
    //----------------//
    /**
     * Lazily retrieve which action is mapped to the "switchLayers"
     * method.
     *
     * @return the mapped action
     */
    private ApplicationAction getLayerAction ()
    {
        if (layerAction == null) {
            layerAction = ActionManager.getInstance()
                    .getActionInstance(this, "switchLayers");
        }

        return layerAction;
    }

    //---------------//
    // getLayerIcons //
    //---------------//
    /**
     * Build the map of icons, based on painting layer.
     *
     * @return the map (layer -> icon)
     */
    private Map<PaintingLayer, Icon> getLayerIcons ()
    {
        if (layerIcons == null) {
            layerIcons = new HashMap<PaintingLayer, Icon>();

            final String root = Main.getGui()
                    .getIconsRoot();

            for (PaintingLayer layer : PaintingLayer.values()) {
                layerIcons.put(
                        layer,
                        new ImageIcon(
                        getClass().getResource(
                        root + "/apps/" + layer.getImageName())));
            }
        }

        return layerIcons;
    }

    //~ Inner Interfaces -------------------------------------------------------
    //--------//
    // Holder //
    //--------//
    private static interface Holder
    {
        //~ Static fields/initializers -----------------------------------------

        public static final PaintingParameters INSTANCE = new PaintingParameters();

    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Should the annotations be painted */
        final Constant.Boolean annotationPainting = new Constant.Boolean(
                true,
                "Should the annotations be painted");

        /** Should the slots be painted */
        final Constant.Boolean slotPainting = new Constant.Boolean(
                true,
                "Should the slots be painted");

        /** Should the marks be painted */
        final Constant.Boolean markPainting = new Constant.Boolean(
                true,
                "Should the marks be painted");

        //        /** Which layers should be painted */
        //        final PaintingLayer.Constant paintingLayer = new PaintingLayer.Constant(
        //            PaintingLayer.INPUT_OUTPUT,
        //            "Which layers should be painted");
        //        /** Should the voices be painted */
        //        final Constant.Boolean voicePainting = new Constant.Boolean(
        //            false,
        //            "Should the voices be painted");
    }
}
