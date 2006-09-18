//----------------------------------------------------------------------------//
//                                                                            //
//                            C h e c k B o a r d                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.check;

import omr.selection.Selection;
import omr.selection.SelectionHint;

import omr.ui.Board;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.util.Collections;

/**
 * Class <code>CheckBoard</code> defines a board dedicated to the display of
 * check result information.
 *
 * <dl>
 * <dt><b>Selection Inputs:</b></dt><ul>
 * <li>*_GLYPH
 * </ul>
 * </dl>
 *
 * @param C the specific {@link Checkable} type of object to be checked
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class CheckBoard<C extends Checkable>
    extends Board
{
    //~ Instance fields --------------------------------------------------------

    // For display of check suite results
    private final CheckPanel<C> checkPanel;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // CheckBoard //
    //------------//
    /**
     * Create a Check Board
     *
     * @param name           the name of the check
     * @param suite          the check suite to be used
     * @param inputSelection the input to run check upon
     */
    public CheckBoard (String        name,
                       CheckSuite<C> suite,
                       Selection     inputSelection)
    {
        super(Board.Tag.CHECK, name + "-CheckBoard");
        checkPanel = new CheckPanel<C>(suite);
        defineLayout(suite.getName());

        setInputSelectionList(Collections.singletonList(inputSelection));

        // define default content
        tellObject(null);
    }

    //~ Methods ----------------------------------------------------------------

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
        checkPanel.setSuite(suite);
    }

    //--------//
    // update //
    //--------//
    /**
     * Call-back triggered when C Selection has been modified.
     *
     * @param selection the Selection to perform check upon
     * @param hint potential notification hint
     */
    public void update (Selection     selection,
                        SelectionHint hint)
    {
        tellObject((C) selection.getEntity());
    }

    //------------//
    // tellObject //
    //------------//
    /**
     * Render the result of checking the given object
     *
     * @param object the object whose check result is to be displayed
     */
    protected void tellObject (C object)
    {
        if (object == null) {
            getComponent()
                .setVisible(false);
        } else {
            getComponent()
                .setVisible(true);
            checkPanel.passForm(object);
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout (String name)
    {
        FormLayout   layout = new FormLayout("pref", "pref,pref,pref");
        PanelBuilder builder = new PanelBuilder(layout, getComponent());
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int             r = 1; // --------------------------------
        builder.addSeparator(name + " check", cst.xy(1, r));

        r += 2; // --------------------------------
        builder.add(checkPanel.getComponent(), cst.xy(1, r));
    }
}
