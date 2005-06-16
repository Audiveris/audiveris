//-----------------------------------------------------------------------//
//                                                                       //
//                             M a i l B o x                             //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.util;

/**
 * Class <code>MailBox</code> is a basic implementation of a mail box, with
 * blocking read and write and non-blocking read (polling)
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class MailBox
{
    //~ Instance variables ------------------------------------------------

    private final int max; // Max number of items in the mailbox
    private final Object[] data; // The contained items
    private int nextIn = 0; // Current number of items in the mbx
    private int nextOut = 0; // Current number of items in the mbx
    private int count = 0; // Current number of items in the mbx

    //~ Constructors ------------------------------------------------------

    //---------//
    // MailBox //
    //---------//

    /**
     * Creates a new <code>MailBox</code> instance, with a maximum size.
     *
     * @param max the maximum number of objects the mailbox can contain
     */
    public MailBox (int max)
    {
        this.max = max;
        data = new Object[max];
    }

    //~ Methods -----------------------------------------------------------

    //----------//
    // getCount //
    //----------//

    /**
     * Report the current number of objects in the box.
     *
     * @return this number
     */
    public int getCount ()
    {
        return count;
    }

    //-----//
    // get //
    //-----//

    /**
     * Get the next object out of the mailbox, and blocks till this next
     * object is actually available.
     *
     * @return the next object
     * @throws InterruptedException if an error occurs
     */
    public synchronized Object get ()
            throws InterruptedException
    {
        while (count == 0) {
            wait();
        }

        Object result = data[nextOut];
        data[nextOut] = null; // To allow garbage collection
        nextOut = (nextOut + 1) % max;
        count--;
        notify();

        return result;
    }

    //------//
    // poll //
    //------//

    /**
     * Try to get the next object out of the mail box, in a non-blocking
     * manner
     *
     * @return the next object, or null if none is available
     * @throws InterruptedException if an error occurs
     */
    public synchronized Object poll ()
            throws InterruptedException
    {
        Object result;

        if (count > 0) {
            result = data[nextOut];
            data[nextOut] = null; // To allow garbage collection
            nextOut = (nextOut + 1) % max;
            count--;
        } else {
            result = null;
        }

        notify();

        return result;
    }

    //-----//
    // put //
    //-----//

    /**
     * Store a new object into the mail box. The method blocks until there
     * is enough room in the mail box.
     *
     * @param item the object to store
     *
     * @throws InterruptedException if an error occurs
     */
    public synchronized void put (Object item)
            throws InterruptedException
    {
        while (count == max) {
            wait();
        }

        data[nextIn] = item;
        nextIn = (nextIn + 1) % max;
        count++;
        notify();
    }
}
