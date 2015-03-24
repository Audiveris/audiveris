//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            L o c a t i o n D e p e n d e n t M e n u                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.view;

import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JMenu;

/**
 * Class {@code LocationDependentMenu}
 *
 * @author Hervé Bitteur
 */
public class LocationDependentMenu
        extends JMenu
        implements LocationDependent
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new LocationDependentMenu object.
     */
    public LocationDependentMenu ()
    {
        super();
    }

    /**
     * Creates a new LocationDependentMenu object.
     *
     * @param s menu text
     */
    public LocationDependentMenu (String s)
    {
        super(s);
    }

    /**
     * Creates a new LocationDependentMenu object.
     *
     * @param action an action
     */
    public LocationDependentMenu (Action action)
    {
        this();
        setAction(action);
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void updateUserLocation (Rectangle rect)
    {
        for (Component comp : getMenuComponents()) {
            if (comp instanceof AbstractButton) {
                Action action = ((AbstractButton) comp).getAction();

                if (action instanceof LocationDependent) {
                    ((LocationDependent) action).updateUserLocation(rect);
                }
            }
        }
    }
}
