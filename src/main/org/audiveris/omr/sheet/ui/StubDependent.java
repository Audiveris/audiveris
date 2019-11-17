//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S t u b D e p e n d e n t                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.Versions;
import org.audiveris.omr.sig.ui.InterController;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.StubEvent;

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

    private static final Logger logger = LoggerFactory.getLogger(StubDependent.class);

    /** Name of property linked to initial image availability. */
    public static final String INITIAL_AVAILABLE = "initialAvailable";

    /** Name of property linked to sheet transcription ability. */
    public static final String STUB_TRANSCRIBABLE = "stubTranscribable";

    /** Name of property linked to sheet availability. */
    public static final String STUB_AVAILABLE = "stubAvailable";

    /** Name of property linked to sheet validity. */
    public static final String STUB_VALID = "stubValid";

    /** Name of property linked to sheet lack of activity. */
    public static final String STUB_IDLE = "stubIdle";

    /** Name of property linked to book transcription ability. */
    public static final String BOOK_TRANSCRIBABLE = "bookTranscribable";

    /** Name of property linked to book upgrade ability. */
    public static final String BOOK_UPGRADABLE = "bookUpgradable";

    /** Name of property linked to book lack of activity. */
    public static final String BOOK_IDLE = "bookIdle";

    /** Name of property linked to book modified/upgraded. */
    public static final String BOOK_MODIFIED_OR_UPGRADED = "bookModifiedOrUpgraded";

    /** Name of property linked to undoable. */
    public static final String UNDOABLE = "undoable";

    /** Name of property linked to redoable. */
    public static final String REDOABLE = "redoable";

    /** Indicates whether the sheet initial image is available. */
    protected boolean initialAvailable = false;

    /** Indicates whether the current sheet stub can be transcribed. */
    protected boolean stubTranscribable = false;

    /** Indicates whether there is a current sheet stub. */
    protected boolean stubAvailable = false;

    /** Indicates whether the current sheet stub is valid. */
    protected boolean stubValid = false;

    /** Indicates whether current sheet is idle. */
    protected boolean stubIdle = false;

    /** Indicates whether the current book can be transcribed. */
    protected boolean bookTranscribable = false;

    /** Indicates whether the current book can be upgraded. */
    protected boolean bookUpgradable = false;

    /** Indicates whether current book is idle (all its sheets are idle). */
    protected boolean bookIdle = false;

    /** Indicates whether current book has been modified/upgraded. */
    protected boolean bookModifiedOrUpgraded = false;

    /** Indicates whether we can undo user action. */
    protected boolean undoable = false;

    /** Indicates whether we can redo user action. */
    protected boolean redoable = false;

    /**
     * Creates a new {@code StubDependent} object.
     */
    protected StubDependent ()
    {
        // Stay informed on stub status, in order to enable or disable all dependent actions
        StubsController.getInstance().subscribe(this);
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

    //--------------------------//
    // isBookModifiedOrUpgraded //
    //--------------------------//
    /**
     * Getter for bookModifiedOrUpgraded property
     *
     * @return the current property value
     */
    public boolean isBookModifiedOrUpgraded ()
    {
        return bookModifiedOrUpgraded;
    }

    //---------------------------//
    // setBookModifiedOrUpgraded //
    //---------------------------//
    /**
     * Setter for bookModifiedOrUpgraded property
     *
     * @param bookModifiedOrUpgraded the new property value
     */
    public void setBookModifiedOrUpgraded (boolean bookModifiedOrUpgraded)
    {
        boolean oldValue = this.bookModifiedOrUpgraded;
        this.bookModifiedOrUpgraded = bookModifiedOrUpgraded;

        if (bookModifiedOrUpgraded != oldValue) {
            firePropertyChange(BOOK_MODIFIED_OR_UPGRADED, oldValue, this.bookModifiedOrUpgraded);
        }
    }

    //---------------------//
    // isBookTranscribable //
    //---------------------//
    /**
     * Getter for bookTranscribable property
     *
     * @return the current property value
     */
    public boolean isBookTranscribable ()
    {
        return bookTranscribable;
    }

    //----------------------//
    // setBookTranscribable //
    //----------------------//
    /**
     * Setter for bookTranscribable property.
     *
     * @param bookTranscribable the new property value
     */
    public void setBookTranscribable (boolean bookTranscribable)
    {
        boolean oldValue = this.bookTranscribable;
        this.bookTranscribable = bookTranscribable;

        if (bookTranscribable != oldValue) {
            firePropertyChange(BOOK_TRANSCRIBABLE, oldValue, this.bookTranscribable);
        }
    }

    //------------------//
    // isBookUpgradable //
    //------------------//
    /**
     * Getter for bookUpgradable property
     *
     * @return the current property value
     */
    public boolean isBookUpgradable ()
    {
        return bookUpgradable;
    }

    //-------------------//
    // setBookUpgradable //
    //-------------------//
    /**
     * Setter for bookUpgradable property.
     *
     * @param bookUpgradable the new property value
     */
    public void setBookUpgradable (boolean bookUpgradable)
    {
        boolean oldValue = this.bookUpgradable;
        this.bookUpgradable = bookUpgradable;

        if (bookUpgradable != oldValue) {
            firePropertyChange(BOOK_UPGRADABLE, oldValue, this.bookUpgradable);
        }
    }

    //------------//
    // isRedoable //
    //------------//
    /**
     * Getter for redoable property
     *
     * @return the current property value
     */
    public boolean isRedoable ()
    {
        return redoable;
    }

    //-------------//
    // setRedoable //
    //-------------//
    /**
     * Setter for redoable property
     *
     * @param redoable the new property value
     */
    public void setRedoable (boolean redoable)
    {
        boolean oldValue = this.redoable;
        this.redoable = redoable;

        if (redoable != oldValue) {
            firePropertyChange(REDOABLE, oldValue, this.redoable);
        }
    }

    //--------------------//
    // isInitialAvailable //
    //--------------------//
    /**
     * Getter for initialAvailable property
     *
     * @return the current property value
     */
    public boolean isInitialAvailable ()
    {
        return initialAvailable;
    }

    //---------------------//
    // setInitialAvailable //
    //---------------------//
    /**
     * Setter for initialAvailable property.
     *
     * @param initialAvailable the new property value
     */
    public void setInitialAvailable (boolean initialAvailable)
    {
        boolean oldValue = this.initialAvailable;
        this.initialAvailable = initialAvailable;

        if (initialAvailable != oldValue) {
            firePropertyChange(INITIAL_AVAILABLE, oldValue, this.initialAvailable);
        }
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

    //---------------------//
    // isStubTranscribable //
    //---------------------//
    /**
     * Getter for stubTranscribable property
     *
     * @return the current property value
     */
    public boolean isStubTranscribable ()
    {
        return stubTranscribable;
    }

    //----------------------//
    // setStubTranscribable //
    //----------------------//
    /**
     * Setter for stubTranscribable property.
     *
     * @param stubTranscribable the new property value
     */
    public void setStubTranscribable (boolean stubTranscribable)
    {
        boolean oldValue = this.stubTranscribable;
        this.stubTranscribable = stubTranscribable;

        if (stubTranscribable != oldValue) {
            firePropertyChange(STUB_TRANSCRIBABLE, oldValue, this.stubTranscribable);
        }
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

    //------------//
    // isUndoable //
    //------------//
    /**
     * Getter for undoable property
     *
     * @return the current property value
     */
    public boolean isUndoable ()
    {
        return undoable;
    }

    //-------------//
    // setUndoable //
    //-------------//
    /**
     * Setter for undoable property
     *
     * @param undoable the new property value
     */
    public void setUndoable (boolean undoable)
    {
        boolean oldValue = this.undoable;
        this.undoable = undoable;

        if (undoable != oldValue) {
            firePropertyChange(UNDOABLE, oldValue, this.undoable);
        }
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

            //            logger.info(
            //                    "event: {} {}",
            //                    stubEvent,
            //                    (stub != null) ? stub.getId() : "no stub");
            //
            // Update stubAvailable
            setStubAvailable(stub != null);

            // Update stubValid
            setStubValid((stub != null) && stub.isValid());

            // Update stubIdle & stubTranscribable
            if (stub != null) {
                final Step currentStep = stub.getCurrentStep();
                final boolean idle = currentStep == null;
                setStubIdle(idle);
                setStubTranscribable(idle && stub.isValid() && !stub.isDone(Step.last()));
            } else {
                setStubIdle(false);
                setStubTranscribable(false);
            }

            // Update bookIdle & bookTranscribable & bookUpgradable
            if (stub != null) {
                final boolean idle = isBookIdle(stub.getBook());
                setBookIdle(idle);
                setBookTranscribable(idle && isBookTranscribable(stub.getBook()));
                setBookUpgradable(idle && isBookUpgradable(stub.getBook()));
            } else {
                setBookIdle(false);
                setBookTranscribable(false);
                setBookUpgradable(false);
            }

            // Update bookModifiedOrUpgraded
            if (stub != null) {
                final Book book = stub.getBook();
                setBookModifiedOrUpgraded(book.isModified() || book.isUpgraded());
            } else {
                setBookModifiedOrUpgraded(false);
            }

            // Update undoable/redoable
            if ((stub != null) && stub.hasSheet()) {
                InterController ctrl = stub.getSheet().getInterController();
                setUndoable(ctrl.canUndo());
                setRedoable(ctrl.canRedo());
            } else {
                setUndoable(false);
                setRedoable(false);
            }

            // Update initialAvailable
            if ((stub != null) && stub.hasSheet()) {
                final Picture picture = stub.getSheet().getPicture();
                setInitialAvailable(!picture.hasNoInitialImage());
            } else {
                setInitialAvailable(false);
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //------------//
    // isBookIdle //
    //------------//
    private boolean isBookIdle (Book book)
    {
        for (SheetStub stub : book.getValidStubs()) {
            final Step currentStep = stub.getCurrentStep();

            if (currentStep != null) {
                return false;
            }
        }

        return true;
    }

    //---------------------//
    // isBookTranscribable //
    //---------------------//
    private boolean isBookTranscribable (Book book)
    {
        // Book is assumed idle
        for (SheetStub stub : book.getValidStubs()) {
            if (!stub.isDone(Step.last())) {
                return true;
            }
        }

        return book.isDirty();
    }

    //------------------//
    // isBookUpgradable //
    //------------------//
    private boolean isBookUpgradable (Book book)
    {
        // Book is assumed idle
        for (SheetStub stub : book.getValidStubs()) {
            if (stub.getVersion().compareTo(Versions.LATEST_UPGRADE) < 0) {
                return true;
            }
        }

        return false;
    }
}
