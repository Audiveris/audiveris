//----------------------------------------------------------------------------//
//                                                                            //
//                        V i e w P a r a m e t e r s                         //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import org.jdesktop.application.AbstractBean;
import org.jdesktop.application.Action;

import java.awt.event.ActionEvent;

/**
 * Class <code>ViewParameters</code> handles parameters for GlyphLagView
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ViewParameters
    extends AbstractBean
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

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
        firePropertyChange("circlePainting", oldValue, value);
    }

    //------------------//
    // isCirclePainting //
    //------------------//
    public boolean isCirclePainting ()
    {
        return constants.circlePainting.getValue();
    }

    //-----------------//
    // setLinePainting //
    //-----------------//
    public void setLinePainting (boolean value)
    {
        boolean oldValue = constants.linePainting.getValue();
        constants.linePainting.setValue(value);
        firePropertyChange("linePainting", oldValue, value);
    }

    //----------------//
    // isLinePainting //
    //----------------//
    public boolean isLinePainting ()
    {
        return constants.linePainting.getValue();
    }

    //---------------//
    // toggleCircles //
    //---------------//
    /**
     * Action that toggles the display of approximating circles in selected
     * slur-shaped glyphs
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = "circlePainting")
    public void toggleCircles (ActionEvent e)
    {
    }

    //-------------//
    // toggleLines //
    //-------------//
    /**
     * Action that toggles the display of mean line in selected sticks
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = "linePainting")
    public void toggleLines (ActionEvent e)
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

        /** Should the lines be painted */
        final Constant.Boolean linePainting = new Constant.Boolean(
            false,
            "Should the stick lines be painted");

        /** Should the circles be painted */
        final Constant.Boolean circlePainting = new Constant.Boolean(
            true,
            "Should the slur circles be painted");
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
