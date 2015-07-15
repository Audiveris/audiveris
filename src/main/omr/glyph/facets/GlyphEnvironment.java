//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                G l y p h E n v i r o n m e n t                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

/**
 * Interface {@code GlyphEnvironment} defines the facet in charge of the surrounding
 * environment of a glyph, in terms of staff-based pitch position, of presence of stem
 * or ledgers, etc.
 *
 * @author Hervé Bitteur
 */
interface GlyphEnvironment
        extends GlyphFacet
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report the pitchPosition feature (position relative to the staff)
     *
     * @return the pitchPosition value
     */
    double getPitchPosition ();

    /**
     * Report whether this glyph intersects that provided glyph
     *
     * @param that the other glyph
     * @return true if overlap, false otherwise
     */
    boolean intersects (Glyph that);

    /**
     * Report whether this glyph touches (or intersects) that provided glyph
     *
     * @param that the other glyph
     * @return true if touching, false otherwise
     */
    boolean touches (Glyph that);

    /**
     * Setter for the pitch position, with respect to containing staff
     *
     * @param pitchPosition the pitch position wrt the staff
     */
    void setPitchPosition (double pitchPosition);
}
