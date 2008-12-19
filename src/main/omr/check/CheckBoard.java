//----------------------------------------------------------------------------//
//                                                                            //
//                            C h e c k B o a r d                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.check;

import omr.selection.UserEvent;

import omr.ui.Board;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import org.bushe.swing.event.EventService;

import java.util.Collection;

/**
 * Class <code>CheckBoard</code> defines a board dedicated to the display of
 * check result information.
 *
 * @param <C> The {@link Checkable} entity type to be checked
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class CheckBoard<C extends Checkable>
    extends Board
{
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
     * @param eventService which event service to use
     * @param eventList    which even classes to expect
     */
    public CheckBoard (String                                name,
                       CheckSuite<C>                         suite,
                       EventService                          eventService,
                       Collection<Class<?extends UserEvent>> eventList)
    {
        super(name + "-CheckBoard", eventService, eventList);
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
    public void setSuite (CheckSuite<C> suite)
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
        tellObject((C) event.getData()); // Compiler warning
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
