//----------------------------------------------------------------------------//
//                                                                            //
//                        V i e w P a r a m e t e r s                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

import org.jdesktop.application.AbstractBean;
import org.jdesktop.application.Action;

import java.awt.event.ActionEvent;

/**
 * Class <code>ViewParameters</code> handles parameters for GlyphLagView,
 * using properties referenced through their programmatic name to avoid typos.
 *
 * @author Hervé Bitteur
 */
public class ViewParameters
    extends AbstractBean
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ViewParameters.class);

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Should the slur circles be painted  */
    public static final String CIRCLE_PAINTING = "circlePainting";

    /** Should the letter boxes be painted */
    public static final String LETTER_BOX_PAINTING = "letterBoxPainting";

    /** Should the stick lines be painted */
    public static final String LINE_PAINTING = "linePainting";

    /** Should the Sections selection be enabled  */
    public static final String SECTION_SELECTION_ENABLED = "sectionSelectionEnabled";

    //~ Instance fields --------------------------------------------------------

    /** Dynamic flag to remember if section selection is enabled */
    private boolean sectionSelectionEnabled = false;

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    public static ViewParameters getInstance ()
    {
        return Holder.INSTANCE;
    }

    //-------------------//
    // setCirclePainting //
    //-------------------//
    public void setCirclePainting (boolean value)
    {
        boolean oldValue = constants.circlePainting.getValue();
        constants.circlePainting.setValue(value);
        firePropertyChange(CIRCLE_PAINTING, oldValue, value);
    }

    //------------------//
    // isCirclePainting //
    //------------------//
    public boolean isCirclePainting ()
    {
        return constants.circlePainting.getValue();
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

    //---------------------//
    // isLetterBoxPainting //
    //---------------------//
    public boolean isLetterBoxPainting ()
    {
        return constants.letterBoxPainting.getValue();
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
    // isLinePainting //
    //----------------//
    public boolean isLinePainting ()
    {
        return constants.linePainting.getValue();
    }

    //----------------------------//
    // setSectionSelectionEnabled //
    //----------------------------//
    public void setSectionSelectionEnabled (boolean value)
    {
        boolean oldValue = sectionSelectionEnabled;
        sectionSelectionEnabled = value;
        firePropertyChange(SECTION_SELECTION_ENABLED, oldValue, value);
    }

    //---------------------------//
    // isSectionSelectionEnabled //
    //---------------------------//
    public boolean isSectionSelectionEnabled ()
    {
        return sectionSelectionEnabled;
    }

    //---------------//
    // toggleCircles //
    //---------------//
    /**
     * Action that toggles the display of approximating circles in selected
     * slur-shaped glyphs
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = CIRCLE_PAINTING)
    public void toggleCircles (ActionEvent e)
    {
    }

    //---------------//
    // toggleLetters //
    //---------------//
    /**
     * Action that toggles the display of letter boxes in selected glyphs
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
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = LINE_PAINTING)
    public void toggleLines (ActionEvent e)
    {
    }

    //----------------//
    // toggleSections //
    //----------------//
    /**
     * Action that toggles the ability to select Sections (rather than Glyphs)
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = SECTION_SELECTION_ENABLED)
    public void toggleSections (ActionEvent e)
    {
        logger.info("SectionSelection enabled:" + isSectionSelectionEnabled());
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Should the circles be painted */
        final Constant.Boolean circlePainting = new Constant.Boolean(
            true,
            "Should the slur circles be painted");

        /** Should the letter boxes be painted */
        final Constant.Boolean letterBoxPainting = new Constant.Boolean(
            true,
            "Should the letter boxes be painted");

        /** Should the lines be painted */
        final Constant.Boolean linePainting = new Constant.Boolean(
            false,
            "Should the stick lines be painted");
    }

    //--------//
    // Holder //
    //--------//
    private static class Holder
    {
        //~ Static fields/initializers -----------------------------------------

        public static final ViewParameters INSTANCE = new ViewParameters();
    }
}
