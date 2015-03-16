//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        L i v e P a r a m                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

/**
 * Class {@code LiveParam} is a {@link Param} for which we handle the
 * value actually used, to detect when re-processing must be done.
 *
 * @param <E> type of parameter handled
 *
 * @author Hervé Bitteur
 */
public class LiveParam<E>
        extends Param<E>
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Actually used parameter, if any. */
    private E actual;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a LiveParam object.
     *
     * @param parent parent context, or null
     */
    public LiveParam (Param<E> parent)
    {
        super(parent);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // getActual //
    //-----------//
    /**
     * @return the actual
     */
    public E getActual ()
    {
        return actual;
    }

    //
    //-------------//
    // needsUpdate //
    //-------------//
    public boolean needsUpdate ()
    {
        return !getTarget().equals(actual);
    }

    //-----------//
    // setActual //
    //-----------//
    public void setActual (E actual)
    {
        this.actual = actual;
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());

        if (actual != null) {
            sb.append(" actual:").append(actual);
        }

        return sb.toString();
    }
}
