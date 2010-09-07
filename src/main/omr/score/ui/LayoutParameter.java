//----------------------------------------------------------------------------//
//                                                                            //
//                       L a y o u t P a r a m e t e r                        //
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
 * Class <code>LayoutParameter</code> handles the user choice on score layout
 *
 * @author Hervé Bitteur
 */
public class LayoutParameter
    extends AbstractBean
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Should the systems be painted vertically */
    public static final String VERTICAL_LAYOUT = "verticalLayout";

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    public static LayoutParameter getInstance ()
    {
        return Holder.INSTANCE;
    }

    //---------------------//
    // getScoreOrientation //
    //---------------------//
    public ScoreOrientation getScoreOrientation ()
    {
        return constants.verticalLayout.getValue() ? ScoreOrientation.VERTICAL
               : ScoreOrientation.HORIZONTAL;
    }

    //-------------------//
    // setVerticalLayout //
    //-------------------//
    public void setVerticalLayout (boolean value)
    {
        boolean oldValue = constants.verticalLayout.getValue();
        constants.verticalLayout.setValue(value);
        firePropertyChange(
            VERTICAL_LAYOUT,
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

    //--------------//
    // toggleLayout //
    //--------------//
    /**
     * Action that toggles the layout of the systems
     * @param e the event that triggered this action
     */
    @Action(selectedProperty = VERTICAL_LAYOUT)
    public void toggleLayout (ActionEvent e)
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

        /** Should the systems be painted vertically */
        final Constant.Boolean verticalLayout = new Constant.Boolean(
            true,
            "Should the systems be painted vertically");
    }

    //--------//
    // Holder //
    //--------//
    private static class Holder
    {
        //~ Static fields/initializers -----------------------------------------

        public static final LayoutParameter INSTANCE = new LayoutParameter();
    }
}
