//-----------------------------------------------------------------------//
//                                                                       //
//                          C h e c k B o a r d                          //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.check;

import omr.ui.Board;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import javax.swing.JEditorPane;
import javax.swing.JLabel;

/**
 * Class <code>CheckBoard</code> defines a board dedicated to the display
 * of check result information.
 *
 * @param C the specific {@link Checkable} type of object to be checked
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class CheckBoard <C extends Checkable>
    extends Board
    implements CheckMonitor<C>
{
    //~ Instance variables ------------------------------------------------

    // For display of check suite results
    private final CheckPanel<C> checkPane;

    //~ Constructors ------------------------------------------------------

    //------------//
    // CheckBoard //
    //------------//
    /**
     * Create a Check Board
     *
     * @param suite the check suite to be used
     */
    public CheckBoard (CheckSuite<C> suite)
    {
        super(Board.Tag.CHECK);
        checkPane = new CheckPanel<C>(suite);
        defineLayout(suite.getName());

        // define default content
        tellObject(null);
    }

    //~ Methods -----------------------------------------------------------

    //----------//
    // setSuite //
    //----------//
    /**
     * Assign a (new) suite to the check board
     *
     * @param suite the (new) check suite to be used
     */
    public void setSuite (CheckSuite<C> suite)
    {
        checkPane.setSuite(suite);
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout (String name)
    {
        FormLayout layout = new FormLayout("pref", "pref,pref,pref");
        PanelBuilder builder = new PanelBuilder(layout, getComponent());
        builder.setDefaultDialogBorder();
        CellConstraints cst = new CellConstraints();

        int r = 1;                      // --------------------------------
        builder.addSeparator(name + " check",   cst.xy(1,  r));

        r += 2;                         // --------------------------------
        builder.add(checkPane.getComponent(),   cst.xy (1,  r));
    }

    //------------//
    // tellObject //
    //------------//
    /**
     * Render the result of checking the given object
     *
     * @param object the object whose check result is to be displayed
     */
    public void tellObject (C object)
    {
        if (object == null) {
            getComponent().setVisible(false);
        } else {
            getComponent().setVisible(true);
            checkPane.passForm(object);
        }
    }
}
