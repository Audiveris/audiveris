//----------------------------------------------------------------------------//
//                                                                            //
//                             S e l e c t i o n                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.selection;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.util.Logger;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class <code>Selection</code> is a specific observable subject, meant to
 * encapsulate the result of a given selection made by the user through the User
 * Interface.
 *
 * <p>For a better customization, the original java.util.Observable class has
 * been rewritten here, in a non thread-safe manner, with better handling of
 * observer identities.
 *
 * <p>The collection of instances of {@link Selection} class is constrained by
 * the enumeration {@link SelectionTag}.
 *
 * <p>Notification can be flagged by a {@link SelectionHint} to provide
 * additional information to the observers.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Selection
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants         constants = new Constants();
    private static final Logger            logger = Logger.getLogger(
        Selection.class);

    // Attempt to debug choreography of messages ...
    private static int                     level = 0;

    //~ Instance fields --------------------------------------------------------

    // Collection of (named) selection observers
    private final ArrayList<NamedObserver> observers = new ArrayList<NamedObserver>();

    // The qualifying tag (better than mere class name)
    private final SelectionTag tag;

    // The entity designated by this selection
    private Object  entity;

    // Indicate if entity has changed
    private boolean changed = false;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // Selection //
    //-----------//
    /**
     * Standard constructor where allowed tags are all ones except SHEET.
     *
     * @param tag the selection tag
     */
    public Selection (SelectionTag tag)
    {
        this(tag, false);
    }

    //-----------//
    // Selection //
    //-----------//
    /**
     * Specific constructor to allow allocation of a SHEET selection
     *
     * @param tag the desired tag
     * @param extended true if SHEET is allowed
     */
    private Selection (SelectionTag tag,
                       boolean      extended)
    {
        if (!extended && (tag == SelectionTag.SHEET)) {
            throw new IllegalArgumentException(
                "SelectionTag SHEET is not allowed in sheet selection");
        }

        this.tag = tag;

        if (logger.isFineEnabled()) {
            logger.fine(this + " created");
        }
    }

    //~ Methods ----------------------------------------------------------------

    //--------------------//
    // makeSheetSelection //
    //--------------------//
    /**
     * Factory method to allocate a Sheet Selection
     *
     * @return the allocated SHEET Selection
     */
    public static Selection makeSheetSelection ()
    {
        return new Selection(SelectionTag.SHEET, true);
    }

    //-----------//
    // setEntity //
    //-----------//
    /**
     * Update the selected entity, with a specific hint, and with automatic
     * notification.
     *
     * @param entity the new value for selected entity
     * @param hint   a potential hint for selection observers
     */
    public void setEntity (Object        entity,
                           SelectionHint hint)
    {
        setEntity(entity, hint, true);
    }

    //-----------//
    // setEntity //
    //-----------//
    /**
     * Update the selected entity, with a specific hint, and with the ability to
     * specify whether notification must be done or not.
     *
     * <p>In case of multiple items selected to be viewed in a consistent state,
     * this let the entity modifier decide when it's OK to notify the observers.
     *
     * @param entity the new value for selected entity
     * @param hint   a potential hint for selection observers
     * @param notify false if notification must not be done immediately.  Nota:
     *               In that case it's the caller's responsibility to call the
     *               notifyObservers() method later.
     */
    public void setEntity (Object        entity,
                           SelectionHint hint,
                           boolean       notify)
    {
        this.entity = entity;
        setChanged();

        if (logger.isFineEnabled()) {
            logger.fine(
                indent() + this + hintString(hint) + notifyString(notify) +
                " set to " + entity + " by " +
                new Throwable().getStackTrace()[2]);
        }

        if (notify) {
            notifyObservers(hint);
        }
    }

    //-----------//
    // getEntity //
    //-----------//
    /**
     * Report the current content of selected entity
     *
     * @return current entity value
     */
    public Object getEntity ()
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                indent() + this + " " + entity + " read by " +
                new Throwable().getStackTrace()[1]);
        }

        return entity;
    }

    //--------//
    // getTag //
    //--------//
    /**
     * Report the tag of this selction (useful in switches for example)
     *
     * @return the tag that characterizes this selection
     */
    public SelectionTag getTag ()
    {
        return tag;
    }

    //-------------//
    // addObserver //
    //-------------//
    /**
     * Register an observer to receive future notifications
     *
     * @param observer the observer to be registered
     */
    public void addObserver (SelectionObserver observer)
    {
        String name = observer.getName();

        if (name == null) {
            logger.warning("No name provided for observer " + observer);
        }

        if (logger.isFineEnabled()) {
            logger.fine(
                indent() + this + " adding observer " + name + " by " +
                new Throwable().getStackTrace()[2]);
        }

        // Check if already registered
        for (NamedObserver no : observers) {
            if (no.observer.equals(observer)) {
                logger.warning(this + " already registers " + name);

                return;
            }
        }

        // Register
        observers.add(new NamedObserver(observer, name));
    }

    //----------------//
    // countObservers //
    //----------------//
    /**
     * Report the number of observers registered on this selection object
     *
     * @return the number of observers
     */
    public int countObservers ()
    {
        return observers.size();
    }

    //----------------//
    // deleteObserver //
    //----------------//
    /**
     * Remove a (previously registered) observer
     *
     * @param observer the observer to remove
     */
    public void deleteObserver (SelectionObserver observer)
    {
        for (Iterator<NamedObserver> it = observers.iterator(); it.hasNext();) {
            NamedObserver no = it.next();

            if (no.observer.equals(observer)) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        indent() + this + " deleting observer " + no.name);
                }

                it.remove();

                return;
            }
        }

        // Not found !
        logger.warning(
            this + " cannot delete " + observer.getClass().getName());
    }

    //-----------------//
    // deleteObservers //
    //-----------------//
    /**
     * Remove all observers
     */
    public void deleteObservers ()
    {
        observers.clear();
    }

    //------//
    // dump //
    //------//
    /**
     * Dump to the standard output the list of all observers
     */
    public void dump ()
    {
        System.out.println(this.toLongString());

        if (level != 0) {
            logger.warning(
                "selection notification level is not zero: " + level);
        }

        for (NamedObserver no : observers) {
            System.out.println("    + " + no.name);
        }
    }

    //------------//
    // hasChanged //
    //------------//
    /**
     * Predicate about whether the value has changed since last notification
     *
     * @return true if changed
     */
    public boolean hasChanged ()
    {
        return changed;
    }

    //-----------------//
    // notifyObservers //
    //-----------------//
    /**
     * Notify synchronously all registered observers, providing them with the
     * hint flag (which may be null).
     *
     * @param hint   a potential hint for selection observers
     */
    public void notifyObservers (SelectionHint hint)
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                indent() + "*** " + this + hintString(hint) +
                " notifying list " + getNameList());
        }

        if (!changed) {
            return;
        }

        // A temporary buffer, used as a snapshot of the state of current
        // Observers, to avoid potential modification of this list on the fly
        ArrayList<NamedObserver> copy = new ArrayList<NamedObserver>(observers);

        clearChanged();

        level++;

        // Check we are not in an endless loop
        if (level > constants.maxNotificationLevel.getValue()) {
            level = 0;
            logger.severe(
                "Too many selection levels",
                new Throwable("SelectionStack"));
            throw new RuntimeException("Too many selection levels");
        }

        for (NamedObserver no : copy) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    indent() + "* " + this + hintString(hint) + " notifying " +
                    no.observer.getName());
            }

            no.observer.update(this, hint);
        }

        level--;
    }

    //-------------------//
    // reNotifyObservers //
    //-------------------//
    /**
     * Allows to resend a notification, using current value of the selection
     *
     * @param hint   a potential hint for selection observers
     */
    public void reNotifyObservers (SelectionHint hint)
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                indent() + this + hintString(hint) + " renotified by " +
                new Throwable().getStackTrace()[1]);
        }

        setChanged();
        notifyObservers(hint);
    }

    //--------------//
    // toLongString //
    //--------------//
    /**
     * Report a longer description
     *
     * @return a longer description
     */
    public String toLongString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("{Selection")
          .append(" ")
          .append(tag)
          .append(" entity=")
          .append(entity)
          .append(" observers=")
          .append(countObservers())
          .append("}");

        return sb.toString();
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a short description
     *
     * @return short description
     */
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("{Selection ")
          .append(tag)
          .append("}");

        return sb.toString();
    }

    //------------//
    // setChanged //
    //------------//
    /**
     * Flag the selection as changed
     */
    protected void setChanged ()
    {
        changed = true;
    }

    //--------------//
    // clearChanged //
    //--------------//
    /**
     * Flag the selection as not changed
     */
    protected void clearChanged ()
    {
        changed = false;
    }

    //-------------//
    // getNameList //
    //-------------//
    private String getNameList ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        boolean started = false;

        for (NamedObserver no : observers) {
            if (started) {
                sb.append(";");
            }

            sb.append(no.name);
            started = true;
        }

        sb.append("]");

        return sb.toString();
    }

    //------------//
    // hintString //
    //------------//
    private String hintString (SelectionHint hint)
    {
        if (hint == null) {
            return "";
        } else {
            return " " + hint.toString();
        }
    }

    //--------//
    // indent //
    //--------//
    private String indent ()
    {
        String format = "%" + ((2 * level) + 1) + "s: ";

        return String.format(format, level);
    }

    //--------------//
    // notifyString //
    //--------------//
    private String notifyString (boolean notify)
    {
        if (notify) {
            return "";
        } else {
            return " NO_NOTIFY";
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
        extends ConstantSet
    {
        Constant.Integer maxNotificationLevel = new Constant.Integer(
            10,
            "Reasonable maximum level for stacked selection notifications");

        Constants ()
        {
            initialize();
        }
    }

    //---------------//
    // NamedObserver //
    //---------------//
    /**
     * Class <code>NamedObserver</code> simply encapsulates a pair composed of
     * an SelectionObserver and its readable name
     */
    private static class NamedObserver
    {
        private SelectionObserver observer;
        private String            name;

        public NamedObserver (SelectionObserver observer,
                              String            name)
        {
            this.observer = observer;
            this.name = name;
        }
    }
}
