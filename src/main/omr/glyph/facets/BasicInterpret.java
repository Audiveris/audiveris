//----------------------------------------------------------------------------//
//                                                                            //
//                            B a s i c I n t e r p r e t                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.sheet.SystemInfo;
import omr.sig.Inter;
import omr.sig.Relation;
import omr.sig.SIGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Class {@code BasicInterpret} is a basic implementation for
 * GlyphInterpret.
 *
 * @author Hervé Bitteur
 */
public class BasicInterpret
        extends BasicFacet
        implements GlyphInterpret
{

    private static final Logger logger = LoggerFactory.getLogger(
            BasicInterpret.class);
    //~ Instance fields --------------------------------------------------------

    /** Set of interpretation(s) for this glyph . */
    private Set<Inter> interpretations = new HashSet<>();

    //----------------//
    // BasicInterpret //
    //----------------//
    public BasicInterpret (Glyph glyph)
    {
        super(glyph);
    }

    //~ Methods ----------------------------------------------------------------
    //-------------------//
    // addInterpretation //
    //-------------------//
    @Override
    public void addInterpretation (Inter inter)
    {
        interpretations.add(inter);
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

    //--------//
    // dumpOf //
    //--------//
    @Override
    public String dumpOf ()
    {
        StringBuilder sb = new StringBuilder();

        if (!interpretations.isEmpty()) {
            SystemInfo system = glyph.getSystem();
            if (system != null) {
                SIGraph sig = system.getSig();
                for (Inter inter : interpretations) {
                    sb.append("   interpretation=").append(inter);
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
                    sb.append("\n");
                }
            } else {
                logger.warn("No system for glyph#{}", glyph.getId());
            }
        }

        return sb.toString();
    }
}
