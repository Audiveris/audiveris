//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  G l y p h I n t e r p r e t                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.sig.inter.Inter;

import java.util.Set;

/**
 * Interface {@code GlyphInterpret} defines a facet dealing with the possible
 * interpretations of a glyph as kept in SIG.
 * At most one of the glyph interpretations will be kept in the end.
 *
 * @author Hervé Bitteur
 */
public interface GlyphInterpret
        extends GlyphFacet
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Add a possible interpretation for this glyph.
     *
     * @param inter the possible interpretation
     */
    void addInterpretation (Inter inter);

    /**
     * Report all the current interpretations for this glyph
     *
     * @return the collection of interpretations
     */
    Set<Inter> getInterpretations ();
}
