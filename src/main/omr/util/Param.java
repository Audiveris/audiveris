//----------------------------------------------------------------------------//
//                                                                            //
//                                  P a r a m                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

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
    //~ Instance fields --------------------------------------------------------

    //
    /** Parent param, if any, to inherit from. */
    protected final Param<E> parent;

    /** Specifically set parameter, if any. */
    protected E specific;

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
    //-------------//
    // getSpecific //
    //-------------//
    public E getSpecific ()
    {
        return specific;
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
        if ((getSpecific() == null) || !getSpecific()
                .equals(specific)) {
            this.specific = specific;

            return true;
        } else {
            return false;
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());
        sb.append(internalsString());
        sb.append("}");

        return sb.toString();
    }

    //-----------------//
    // internalsString //
    //-----------------//
    /**
     * Return the string of the internals of this class, typically for
     * inclusion in a toString.
     *
     * @return the string of internals
     */
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();

        if (parent != null) {
            sb.append(" parent:")
                    .append(parent);
        }

        if (getSpecific() != null) {
            sb.append(" specific:")
                    .append(getSpecific());
        }

        return sb.toString();
    }
}
