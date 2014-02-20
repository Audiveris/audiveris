//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      I n t e r A c t i o n                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.ui;

import omr.glyph.Shape;

import omr.selection.InterListEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;

import omr.sig.Inter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.util.Arrays;

import javax.swing.AbstractAction;
import static javax.swing.Action.SHORT_DESCRIPTION;

/**
 * Class {@code InterAction} is the base for Inter actions, with or without relations.
 *
 * @author Hervé Bitteur
 */
public class InterAction
        extends AbstractAction
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(InterAction.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The underlying interpretation. */
    private final Inter inter;

    //~ Constructors -------------------------------------------------------------------------------
    //-------------//
    // InterAction //
    //-------------//
    /**
     * Creates a new InterAction object.
     *
     * @param inter the underlying inter
     */
    public InterAction (Inter inter)
    {
        this(inter, null);
    }

    //-------------//
    // InterAction //
    //-------------//
    /**
     * Creates a new InterAction object.
     *
     * @param inter the underlying inter
     * @param text  specific item text, if any
     */
    public InterAction (Inter inter,
                        String text)
    {
        this.inter = inter;

        Shape shape = inter.getShape();
        putValue(NAME, (text != null) ? text : inter.toString());
        putValue(SMALL_ICON, shape.getDecoratedSymbol());

        final String details = inter.getDetails();

        if (!details.isEmpty()) {
            putValue(SHORT_DESCRIPTION, details);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // actionPerformed //
    //-----------------//
    @Override
    public void actionPerformed (ActionEvent e)
    {
        logger.info(inter.toString());

        publish();
    }

    //---------//
    // publish //
    //---------//
    public void publish ()
    {
        inter.getSig().publish(
                new InterListEvent(
                        this,
                        SelectionHint.INTER_INIT,
                        MouseMovement.PRESSING,
                        Arrays.asList(inter)));
    }
}
