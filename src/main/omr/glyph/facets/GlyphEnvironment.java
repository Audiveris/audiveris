//----------------------------------------------------------------------------//
//                                                                            //
//                      G l y p h E n v i r o n m e n t                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.lag.Lag;
import omr.lag.Section;

import omr.sheet.SystemInfo;

import omr.util.HorizontalSide;
import omr.util.Predicate;

import java.awt.Rectangle;
import java.util.Set;

/**
 * Interface {@code GlyphEnvironment} defines the facet in charge of
 * the surrounding environment of a glyph, in terms of staff-based
 * pitch position, of presence of stem or ledgers, etc.
 *
 * @author Hervé Bitteur
 */
interface GlyphEnvironment
        extends GlyphFacet
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Forward stem-related information from the provided glyph
     *
     * @param glyph the glyph whose stem information has to be used
     */
    void copyStemInformation (Glyph glyph);

    /**
     * Report the number of alien pixels, from the provided lag, found
     * in the specified absolute roi
     *
     * @param lag       the lag to serach
     * @param absRoi    the absolute region of interest
     * @param predicate optional predicate to further filter these aliens
     * @return the number of alien pixels found
     */
    int getAlienPixelsFrom (Lag lag,
                            Rectangle absRoi,
                            Predicate<Section> predicate);

    /**
     * Report the set of glyphs that are connected to this one
     *
     * @return the set of neighboring glyphs, connected through their sections
     */
    Set<Glyph> getConnectedNeighbors ();

    /**
     * Report the first stem attached (left then right), if any
     *
     * @return first stem found, or null
     */
    Glyph getFirstStem ();

    /**
     * Report the pitchPosition feature (position relative to the staff)
     *
     * @return the pitchPosition value
     */
    double getPitchPosition ();

    /**
     * Report the stem attached on the provided side, if any
     *
     * @return stem on provided side, or null
     */
    Glyph getStem (HorizontalSide side);

    /**
     * Report the number of stems the glyph is close to
     *
     * @return the number of stems near by, typically 0, 1 or 2.
     */
    int getStemNumber ();

    /**
     * Return the known glyphs stuck on last side of the stick.
     * (this is relevant mainly for a stem glyph)
     *
     * @param predicate the predicate to apply on each glyph
     * @param goods     the set of correct glyphs (perhaps empty)
     * @param bads      the set of non-correct glyphs (perhaps empty)
     */
    void getSymbolsAfter (Predicate<Glyph> predicate,
                          Set<Glyph> goods,
                          Set<Glyph> bads);

    /**
     * Return the known glyphs stuck on first side of the stick.
     * (this is relevant mainly for a stem glyph)
     *
     * @param predicate the predicate to apply on each glyph
     * @param goods     the set of correct glyphs (perhaps empty)
     * @param bads      the set of non-correct glyphs (perhaps empty)
     */
    void getSymbolsBefore (Predicate<Glyph> predicate,
                           Set<Glyph> goods,
                           Set<Glyph> bads);

    /**
     * Report the containing system, if any.
     *
     * @return the system containing this glyph
     */
    SystemInfo getSystem ();

    /**
     * Report whether the glyph touches a ledger
     *
     * @return true if there is a close ledger
     */
    boolean isWithLedger ();

    /**
     * Setter for the pitch position, with respect to containing staff
     *
     * @param pitchPosition the pitch position wrt the staff
     */
    void setPitchPosition (double pitchPosition);

    /**
     * Assign the stem on the provided side
     *
     * @param stem stem glyph
     */
    void setStem (Glyph stem,
                  HorizontalSide side);

    /**
     * Remember the number of stems near by
     *
     * @param stemNumber the number of stems
     */
    void setStemNumber (int stemNumber);

    /**
     * Remember info about ledger nearby
     *
     * @param withLedger true is there is such ledger
     */
    void setWithLedger (boolean withLedger);
}
