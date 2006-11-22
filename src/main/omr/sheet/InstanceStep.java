//----------------------------------------------------------------------------//
//                                                                            //
//                          I n s t a n c e S t e p                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.ProcessingException;

import java.io.Serializable;

/**
 * Class <code>InstanceStep</code> encapsulates a processing step performed on a
 * sheet instance.
 *
 * @param <R> the type produced by the step as result
 * @see omr.Step The various steps defined
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public abstract class InstanceStep<R>
    implements java.io.Serializable
{
    //~ Instance fields --------------------------------------------------------

    /** Cache the result of the step */
    protected R result;

    /** A readable description for this step */
    private String description;

    //~ Constructors -----------------------------------------------------------

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

    //~ Methods ----------------------------------------------------------------

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

    //-----------//
    // displayUI //
    //-----------//
    /**
     * Make the related user interface visible for this step
     */
    public void displayUI ()
    {
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a quick description and result of this step
     *
     * @return the descriptive string
     */
    @Override
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

    //------//
    // doit //
    //------//
    /**
     * Actually perform the step
     *
     * @throws ProcessingException raised if processing failed
     */
    public abstract void doit ()
        throws ProcessingException;
}
