//----------------------------------------------------------------------------//
//                                                                            //
//                        S h e e t D e p e n d e n t                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.log.Logger;

import omr.selection.MouseMovement;
import omr.selection.SheetEvent;

import omr.sheet.Sheet;

import omr.util.Implement;

import org.bushe.swing.event.EventSubscriber;

import org.jdesktop.application.AbstractBean;

/**
 * Class <code>SheetDependent</code> handles the dependency on sheet
 * availability
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class SheetDependent
    extends AbstractBean
    implements EventSubscriber<SheetEvent>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SheetDependent.class);

    //~ Instance fields --------------------------------------------------------

    /** Indicates whether there is a current sheet */
    protected boolean sheetAvailable = false;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // SheetDependent //
    //----------------//
    /**
     * Creates a new SheetDependent object.
     */
    protected SheetDependent ()
    {
        // Stay informed on sheet status, in order to enable sheet-dependent
        // actions accordingly
        SheetsController.getInstance()
                        .subscribe(this);
    }

    //~ Methods ----------------------------------------------------------------

    //-------------------//
    // setSheetAvailable //
    //-------------------//
    /**
     * Setter for sheetAvailable property
     * @param sheetAvailable the new property value
     */
    public void setSheetAvailable (boolean sheetAvailable)
    {
        boolean oldValue = this.sheetAvailable;
        this.sheetAvailable = sheetAvailable;
        firePropertyChange("sheetAvailable", oldValue, this.sheetAvailable);
    }

    //------------------//
    // isSheetAvailable //
    //------------------//
    /**
     * Getter for sheetAvailable property
     * @return the current property value
     */
    public boolean isSheetAvailable ()
    {
        return sheetAvailable;
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Notification of sheet selection
     *
     * @param event the notified sheet event
     */
    @Implement(EventSubscriber.class)
    public void onEvent (SheetEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            Sheet sheet = event.getData();
            setSheetAvailable(sheet != null);
        } catch (Exception ex) {
            logger.warning(getClass().getName() + " onEvent error", ex);
        }
    }
}
