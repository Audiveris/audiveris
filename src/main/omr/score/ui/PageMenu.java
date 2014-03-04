//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        P a g e M e n u                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;


import omr.ui.view.LocationDependent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import omr.sheet.Sheet;

/**
 * Class {@code PageMenu} is a general page pop-up menu meant to host sub-menus.
 *
 * @author Hervé Bitteur
 */
public class PageMenu
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PageMenu.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The related sheet. */
    protected final Sheet sheet;

    /** Concrete pop-up menu. */
    protected final JPopupMenu popup = new JPopupMenu();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BasicPageMenu object.
     *
     * @param sheet the related sheet
     */
    public PageMenu (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // addMenu //
    //---------//
    /**
     * Add a menu to the page popup
     *
     * @param menu the menu to add
     */
    public void addMenu (JMenu menu)
    {
        popup.add(menu);
    }

    //----------//
    // getPopup //
    //----------//
    /**
     * Report the concrete pop-up menu.
     *
     * @return the pop-up menu
     */
    public JPopupMenu getPopup ()
    {
        return popup;
    }

    //------------//
    // updateMenu //
    //------------//
    /**
     * Update the pop-up menu according to the currently selected items.
     *
     * @param rect the selected rectangle, perhaps degenerated to a point
     * @return true if pop-up is not empty, and thus is worth being shown
     */
    public boolean updateMenu (Rectangle rect)
    {
        // Update interested components
        for (Component component : popup.getComponents()) {
            if (component instanceof LocationDependent) {
                ((LocationDependent) component).updateUserLocation(rect);
            }
        }

        // Check if popup is worth being displayed
        for (Component component : popup.getComponents()) {
            if (component.isVisible()) {
                return true;
            }
        }

        return false;
    }
}
