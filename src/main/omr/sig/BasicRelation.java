//----------------------------------------------------------------------------//
//                                                                            //
//                         B a s i c R e l a t i o n                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

/**
 * Class {@code BasicRelation}
 *
 * @author Hervé Bitteur
 */
public class BasicRelation
        implements Relation
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BasicRelation object.
     */
    public BasicRelation ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    @Override
    public String getDetails ()
    {
        return internals();
    }

    @Override
    public String getName ()
    {
        return "Relation";
    }

    @Override
    public String seenFrom (Inter inter)
    {
        final StringBuilder sb = new StringBuilder(toString());
        final SIGraph sig = inter.getSig();

        if (sig != null) {
            final Inter source = sig.getEdgeSource(this);

            if (source != inter) {
                sb.append("<-")
                        .append(source);
            } else {
                final Inter target = sig.getEdgeTarget(this);

                if (target != inter) {
                    sb.append("->")
                            .append(target);
                }
            }
        }

        return sb.toString();
    }

    @Override
    public String toLongString (SIGraph sig)
    {
        final Inter source = sig.getEdgeSource(this);
        final Inter target = sig.getEdgeTarget(this);
        final StringBuilder sb = new StringBuilder();
        
        sb.append(source);
        sb.append("-");
        sb.append(getName());
        sb.append(":");
        sb.append(getDetails());
        sb.append("-");
        sb.append(target);

        return sb.toString();
    }

    @Override
    public String toString ()
    {
        return getName();
    }

    protected String internals ()
    {
        return "";
    }
}
