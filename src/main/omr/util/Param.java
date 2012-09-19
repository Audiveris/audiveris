//----------------------------------------------------------------------------//
//                                                                            //
//                                  P a r a m                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import omr.log.Logger;

/**
 * Class {@code Param} handles the context of operations performed
 * on score and/or pages.
 *
 * @param <E> type of parameter handled
 *
 * @author Hervé Bitteur
 */
public class Param<E>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Param.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** Parent param, if any, to inherit from. */
    protected final Param<E> parent;

    /** Specifically set parameter, if any. */
    protected E specific;

    /** Actually used parameter, if any. */
    protected E actual;

    //~ Constructors -----------------------------------------------------------
    //
    //-------//
    // Param //
    //-------//
    /**
     * Creates a Param object, with no parent.
     */
    public Param ()
    {
        this(null);
    }

    //-------//
    // Param //
    //-------//
    /**
     * Creates a Param object.
     *
     * @param parent parent context, or null
     */
    public Param (Param<E> parent)
    {
        this.parent = parent;
    }

    //~ Methods ----------------------------------------------------------------
    //
    //-------------//
    // setSpecific //
    //-------------//
    /**
     * Defines a (new) specific value
     *
     * @param specific the new specific value
     * @return true if the new value is actually different
     */
    public boolean setSpecific (E specific)
    {
        if (getSpecific() == null || !getSpecific().equals(specific)) {
            this.specific = specific;
            return true;
        } else {
            return false;
        }
    }

    //-------------//
    // getSpecific //
    //-------------//
    public E getSpecific ()
    {
        return specific;
    }

    //-----------//
    // setActual //
    //-----------//
    public void setActual (E actual)
    {
        this.actual = actual;
    }

    //-----------//
    // getActual //
    //-----------//
    public E getActual ()
    {
        return actual;
    }

    //-------------//
    // needsUpdate //
    //-------------//
    public boolean needsUpdate ()
    {
        return !getTarget().equals(actual);
    }

    //-----------//
    // getTarget //
    //-----------//
    public E getTarget ()
    {
        if (getSpecific() != null) {
            return getSpecific();
        } else if (parent != null) {
            return parent.getTarget();
        } else {
            return null;
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Param");

        if (parent != null) {
            sb.append(" parent:").append(parent);
        }

        if (getSpecific() != null) {
            sb.append(" specific:").append(getSpecific());
        }

        if (actual != null) {
            sb.append(" actual:").append(actual);
        }

        sb.append("}");
        return sb.toString();
    }
}
