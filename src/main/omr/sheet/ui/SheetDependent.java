//----------------------------------------------------------------------------//
//                                                                            //
//                        S h e e t D e p e n d e n t                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.selection.MouseMovement;
import omr.selection.SheetEvent;

import omr.sheet.Sheet;

import org.bushe.swing.event.EventSubscriber;

import org.jdesktop.application.AbstractBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code SheetDependent} handles the dependency on sheet
 * availability.
 *
 * @author Hervé Bitteur
 */
public abstract class SheetDependent
        extends AbstractBean
        implements EventSubscriber<SheetEvent>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            SheetDependent.class);

    /** Name of property linked to sheet availability */
    public static final String SHEET_AVAILABLE = "sheetAvailable";

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
        // Stay informed on sheet status, in order to enable or disable all
        // sheet-dependent actions accordingly
        SheetsController.getInstance()
                .subscribe(this);
    }

    //~ Methods ----------------------------------------------------------------
    //------------------//
    // isSheetAvailable //
    //------------------//
    /**
     * Getter for sheetAvailable property
     *
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
     * Notification of sheet selection.
     *
     * @param sheetEvent the notified sheet event
     */
    @Override
    public void onEvent (SheetEvent sheetEvent)
    {
        try {
            // Ignore RELEASING
            if (sheetEvent.movement == MouseMovement.RELEASING) {
                return;
            }

            Sheet sheet = sheetEvent.getData();
            setSheetAvailable(sheet != null);
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //-------------------//
    // setSheetAvailable //
    //-------------------//
    /**
     * Setter for sheetAvailable property.
     *
     * @param sheetAvailable the new property value
     */
    public void setSheetAvailable (boolean sheetAvailable)
    {
        boolean oldValue = this.sheetAvailable;
        this.sheetAvailable = sheetAvailable;
        firePropertyChange(SHEET_AVAILABLE, oldValue, this.sheetAvailable);
    }
}
