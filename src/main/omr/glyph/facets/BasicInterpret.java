//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      B a s i c I n t e r p r e t                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.sig.SIGraph;
import omr.sig.inter.Inter;
import omr.sig.relation.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Class {@code BasicInterpret} is a basic implementation for GlyphInterpret.
 *
 * @author Hervé Bitteur
 */
public class BasicInterpret
        extends BasicFacet
        implements GlyphInterpret
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BasicInterpret.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Set of interpretation(s) for this glyph . */
    private final Set<Inter> interpretations = new HashSet<Inter>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BasicInterpret object.
     *
     * @param glyph the underlying glyph
     */
    public BasicInterpret (Glyph glyph)
    {
        super(glyph);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------------//
    // addInterpretation //
    //-------------------//
    @Override
    public void addInterpretation (Inter inter)
    {
        interpretations.add(inter);
    }

    //--------//
    // dumpOf //
    //--------//
    @Override
    public String dumpOf ()
    {
        StringBuilder sb = new StringBuilder();

        if (!interpretations.isEmpty()) {
            for (Inter inter : interpretations) {
                sb.append("   interpretation=").append(inter);

                SIGraph sig = inter.getSig();

                if (sig != null) {
                    for (Relation relation : sig.edgesOf(inter)) {
                        Inter source = sig.getEdgeSource(relation);
                        Inter target = sig.getEdgeTarget(relation);
                        sb.append(" {").append(relation);

                        if (source != inter) {
                            sb.append(" <- ").append(source);
                        }

                        if (target != inter) {
                            sb.append(" -> ").append(target);
                        }

                        sb.append("}");
                    }
                } else {
                    sb.append(" NO_SIG");
                }

                sb.append("\n");
            }
        }

        return sb.toString();
    }

    //--------------------//
    // getInterpretations //
    //--------------------//
    @Override
    public Set<Inter> getInterpretations ()
    {
        return interpretations;
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        interpretations.clear();
    }
}
