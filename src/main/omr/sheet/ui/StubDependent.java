//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S t u b D e p e n d e n t                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.ui.selection.MouseMovement;
import omr.ui.selection.StubEvent;

import omr.sheet.Book;
import omr.sheet.SheetStub;

import org.bushe.swing.event.EventSubscriber;

import org.jdesktop.application.AbstractBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code StubDependent} handles the dependency on current sheet stub availability
 * and current sheet stub activity.
 *
 * @author Hervé Bitteur
 */
public abstract class StubDependent
        extends AbstractBean
        implements EventSubscriber<StubEvent>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(StubDependent.class);

    /** Name of property linked to sheet availability. */
    public static final String STUB_AVAILABLE = "stubAvailable";

    /** Name of property linked to sheet validity. */
    public static final String STUB_VALID = "stubValid";

    /** Name of property linked to sheet lack of activity. */
    public static final String STUB_IDLE = "stubIdle";

    /** Name of property linked to book lack of activity. */
    public static final String BOOK_IDLE = "bookIdle";

    /** Name of property linked to book modified. */
    public static final String BOOK_MODIFIED = "bookModified";

    //~ Instance fields ----------------------------------------------------------------------------
    /** Indicates whether there is a current sheet stub. */
    protected boolean stubAvailable = false;

    /** Indicates whether the current sheet stub is valid. */
    protected boolean stubValid = true;

    /** Indicates whether current sheet is idle. */
    protected boolean stubIdle = false;

    /** Indicates whether current book is idle (all its sheets are idle). */
    protected boolean bookIdle = false;

    /** Indicates whether current book has been modified. */
    protected boolean bookModified = false;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code StubDependent} object.
     */
    protected StubDependent ()
    {
        // Stay informed on stub status, in order to enable or disable all dependent actions
        StubsController.getInstance().subscribe(this);
    }

    //~ Methods ------------------------------------------------------------------------------------
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

    //----------------//
    // isBookModified //
    //----------------//
    /**
     * Getter for bookModified property
     *
     * @return the current property value
     */
    public boolean isBookModified ()
    {
        return bookModified;
    }

    //-----------------//
    // isStubAvailable //
    //-----------------//
    /**
     * Getter for stubAvailable property
     *
     * @return the current property value
     */
    public boolean isStubAvailable ()
    {
        return stubAvailable;
    }

    //-------------//
    // isStubValid //
    //-------------//
    /**
     * Getter for stubValid property
     *
     * @return the current property value
     */
    public boolean isStubValid ()
    {
        return stubValid;
    }

    //------------//
    // isStubIdle //
    //------------//
    /**
     * Getter for stubIdle property
     *
     * @return the current property value
     */
    public boolean isStubIdle ()
    {
        return stubIdle;
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Process received notification of sheet stub selection.
     *
     * @param stubEvent the notified sheet stub event
     */
    @Override
    public void onEvent (StubEvent stubEvent)
    {
        try {
            // Ignore RELEASING
            if (stubEvent.movement == MouseMovement.RELEASING) {
                return;
            }

            SheetStub stub = stubEvent.getData();

            // Update stubAvailable
            setStubAvailable(stub != null);

            // Update stubValid
            setStubValid(stub != null && stub.isValid());

            // Update stubIdle
            if (stub != null) {
                setStubIdle(stub.getCurrentStep() == null);
            } else {
                setStubIdle(false);
            }

            // Update bookIdle
            if (stub != null) {
                setBookIdle(isBookIdle(stub.getBook()));
            } else {
                setBookIdle(false);
            }

            // Update bookModified
            if (stub != null) {
                setBookModified(stub.isModified() || stub.getBook().isModified());
            } else {
                setBookModified(false);
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
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

    //-----------------//
    // setBookModified //
    //-----------------//
    /**
     * Setter for bookModified property
     *
     * @param bookModified the new property value
     */
    public void setBookModified (boolean bookModified)
    {
        boolean oldValue = this.bookModified;
        this.bookModified = bookModified;

        if (bookModified != oldValue) {
            firePropertyChange(BOOK_MODIFIED, oldValue, this.bookModified);
        }
    }

    //------------------//
    // setStubAvailable //
    //------------------//
    /**
     * Setter for stubAvailable property.
     *
     * @param stubAvailable the new property value
     */
    public void setStubAvailable (boolean stubAvailable)
    {
        boolean oldValue = this.stubAvailable;
        this.stubAvailable = stubAvailable;

        if (stubAvailable != oldValue) {
            firePropertyChange(STUB_AVAILABLE, oldValue, this.stubAvailable);
        }
    }

    //--------------//
    // setStubValid //
    //--------------//
    /**
     * Setter for stubValid property.
     *
     * @param stubValid the new property value
     */
    public void setStubValid (boolean stubValid)
    {
        boolean oldValue = this.stubValid;
        this.stubValid = stubValid;

        if (stubValid != oldValue) {
            firePropertyChange(STUB_VALID, oldValue, this.stubValid);
        }
    }

    //-------------//
    // setStubIdle //
    //-------------//
    /**
     * Setter for stubIdle property
     *
     * @param stubIdle the new property value
     */
    public void setStubIdle (boolean stubIdle)
    {
        boolean oldValue = this.stubIdle;
        this.stubIdle = stubIdle;

        if (stubIdle != oldValue) {
            firePropertyChange(STUB_IDLE, oldValue, this.stubIdle);
        }
    }

    //------------//
    // isBookIdle //
    //------------//
    private boolean isBookIdle (Book book)
    {
        for (SheetStub stub : book.getStubs()) {
            if (stub.getCurrentStep() != null) {
                return false;
            }
        }

        return true;
    }
}
