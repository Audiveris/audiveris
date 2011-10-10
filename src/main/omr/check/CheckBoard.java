//----------------------------------------------------------------------------//
//                                                                            //
//                            C h e c k B o a r d                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.check;

import omr.log.Logger;

import omr.selection.MouseMovement;
import omr.selection.SelectionService;
import omr.selection.UserEvent;

import omr.ui.Board;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Class <code>CheckBoard</code> defines a board dedicated to the display of
 * check result information.
 *
 * @param <C> The {@link Checkable} entity type to be checked
 *
 * @author Hervé Bitteur
 */
public class CheckBoard<C extends Checkable>
    extends Board
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(CheckBoard.class);

    //~ Instance fields --------------------------------------------------------

    /** For display of check suite results */
    private final CheckPanel<C> checkPanel;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // CheckBoard //
    //------------//
    /**
     * Create a Check Board
     *
     * @param name         the name of the check
     * @param suite        the check suite to be used
     * @param selectionService which selection service to use
     * @param eventList    which even classes to expect
     */
    public CheckBoard (String           name,
                       CheckSuite<C>    suite,
                       SelectionService selectionService,
                       Class[]          eventList)
    {
        super(name + "-CheckBoard", "Check", selectionService, eventList, true);
        checkPanel = new CheckPanel<C>(suite);

        if (suite != null) {
            defineLayout(suite.getName());
        }

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
    public synchronized void setSuite (CheckSuite<C> suite)
    {
        final boolean toBuild = checkPanel.getComponent() == null;
        checkPanel.setSuite(suite);

        if (toBuild) {
            defineLayout(suite.getName());
        }
    }

    //--------//
    // update //
    //--------//
    /**
     * Call-back triggered when C Selection has been modified.
     *
     * @param event the Event to perform check upon its data
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            tellObject((C) event.getData()); // Compiler warning
        } catch (Exception ex) {
            logger.warning(getClass().getName() + " onEvent error", ex);
        }
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
        PanelBuilder builder = new PanelBuilder(layout, getBody());
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int             r = 1; // --------------------------------
        builder.addSeparator(name + " check", cst.xy(1, r));

        r += 2; // --------------------------------
        builder.add(checkPanel.getComponent(), cst.xy(1, r));
    }
}
