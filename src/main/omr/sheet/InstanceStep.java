//-----------------------------------------------------------------------//
//                                                                       //
//                        I n s t a n c e S t e p                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.sheet;

import omr.ProcessingException;
import java.io.Serializable;

/**
 * Class <code>InstanceStep</code> encapsulates a processing step performed
 * on a sheet instance.
 *
 * @param <R> the type produced by the step as result
 * @see omr.Step The various steps defined
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class InstanceStep <R>
    implements java.io.Serializable
{
    //~ Instance variables ------------------------------------------------

    // A readable description for this step
    private String description;

    /**
     * Cache the result of the step
     */
    protected R result;

    //~ Constructors ------------------------------------------------------

    //--------------//
    // InstanceStep //
    //--------------//
    /**
     * Create a new InstanceStep
     *
     * @param description More verbose description
     */
    InstanceStep (String description)
    {
        this.description = description;
    }

    //~ Methods -----------------------------------------------------------

    //------//
    // doit //
    //------//
    /**
     * Actually perform the step
     *
     * @throws ProcessingException raised if processing failed
     */
    protected abstract void doit ()
            throws ProcessingException;

    //-----------//
    // displayUI //
    //-----------//
    /**
     * Make the related user interface visible for this step
     */
    public void displayUI ()
    {
    }

    //----------------//
    // getDescription //
    //----------------//
    /**
     * Report the step description
     *
     * @return the description of this step
     */
    public String getDescription ()
    {
        return description;
    }

    //-----------//
    // getResult //
    //-----------//
    /**
     * Return the result of the step
     *
     * @return the result
     * @throws ProcessingException raised if processing failed
     */
    public R getResult ()
            throws ProcessingException
    {
        if (!isDone()) {
            doit();
        }

        return result;
    }

    //--------//
    // isDone //
    //--------//
    /**
     * Is step processing already done?
     *
     * @return true if done, false otherwise
     */
    public boolean isDone ()
    {
        return result != null;
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a quick description and result of this step
     *
     * @return the descriptive string
     */
    public String toString ()
    {
        return description + " -> " + isDone();
    }

    //------//
    // undo //
    //------//
    /**
     * Erase the result of the step
     */
    public void undo ()
    {
        result = null;
    }

//     private void writeObject(java.io.ObjectOutputStream os)
//         throws java.io.IOException
//     {
//         System.err.println("wo " + getClass().getName() + " " + this);
//         os.defaultWriteObject();
//     }

//     private void readObject(java.io.ObjectInputStream is)
//         throws java.io.IOException,
//                ClassNotFoundException
//     {
//         is.defaultReadObject();
//         System.err.println("ro " + getClass().getName() + " " + this);
//     }
}
