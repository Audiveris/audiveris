//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S h e e t D e p e n d e n t                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
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
 * Class {@code SheetDependent} handles the dependency on current sheet availability
 * and current sheet activity.
 *
 * @author Hervé Bitteur
 */
public abstract class SheetDependent
        extends AbstractBean
        implements EventSubscriber<SheetEvent>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SheetDependent.class);

    /** Name of property linked to sheet availability. */
    public static final String SHEET_AVAILABLE = "sheetAvailable";

    /** Name of property linked to sheet lack of activity. */
    public static final String SHEET_IDLE = "sheetIdle";

    /** Name of property linked to book lack of activity. */
    public static final String BOOK_IDLE = "bookIdle";

    //~ Instance fields ----------------------------------------------------------------------------
    /** Indicates whether there is a current sheet. */
    protected boolean sheetAvailable = false;

    /** Indicates whether current sheet is idle. */
    protected boolean sheetIdle = false;

    /** Indicates whether current book is idle (all its sheets are idle). */
    protected boolean bookIdle = false;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SheetDependent object.
     */
    protected SheetDependent ()
    {
        // Stay informed on sheet status, in order to enable or disable all dependent actions
        SheetsController.getInstance().subscribe(this);
    }

    //~ Methods ------------------------------------------------------------------------------------
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

    //-------------//
    // isSheetIdle //
    //-------------//
    /**
     * Getter for sheetIdle property
     *
     * @return the current property value
     */
    public boolean isSheetIdle ()
    {
        return sheetIdle;
    }

    //------------//
    // isBookIdle //
    //------------//
    /**
     * Getter for bookIdle property
     *
     * @return the current property value
     */
    public boolean isBookIdle ()
    {
        return bookIdle;
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Process received notification of sheet selection.
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

            // Update sheetAvailable
            setSheetAvailable(sheet != null);

            // Update sheetIdle
            if (sheet != null) {
                setSheetIdle(sheet.getCurrentStep() == null);
            } else {
                setSheetIdle(false);
            }

            // Update bookIdle
            if (sheet != null) {
                setBookIdle(sheet.getBook().isIdle());
            } else {
                setBookIdle(false);
            }

        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //--------------//
    // setSheetIdle //
    //--------------//
    /**
     * Setter for sheetIdle property
     *
     * @param sheetIdle the new property value
     */
    public void setSheetIdle (boolean sheetIdle)
    {
        boolean oldValue = this.sheetIdle;
        this.sheetIdle = sheetIdle;

        if (sheetIdle != oldValue) {
            firePropertyChange(SHEET_IDLE, oldValue, this.sheetIdle);
        }
    }

    //-------------//
    // setBookIdle //
    //-------------//
    /**
     * Setter for bookIdle property
     *
     * @param bookIdle the new property value
     */
    public void setBookIdle (boolean bookIdle)
    {
        boolean oldValue = this.bookIdle;
        this.bookIdle = bookIdle;

        if (bookIdle != oldValue) {
            firePropertyChange(BOOK_IDLE, oldValue, this.bookIdle);
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

        if (sheetAvailable != oldValue) {
            firePropertyChange(SHEET_AVAILABLE, oldValue, this.sheetAvailable);
        }
    }
}
