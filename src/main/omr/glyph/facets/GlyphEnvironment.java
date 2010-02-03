//----------------------------------------------------------------------------//
//                                                                            //
//                      G l y p h E n v i r o n m e n t                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.util.Predicate;

import java.util.Set;

/**
 * Interface {@code GlyphEnvironment} defines the facet in charge of the
 * surrounding environment of a glyph, in terms of staff-based pitch position,
 * of presence of stem or ledgers, etc.
 *
 * @author Hervé Bitteur
 */
interface GlyphEnvironment
    extends GlyphFacet
{
    //~ Methods ----------------------------------------------------------------

    //-------------//
    // setLeftStem //
    //-------------//
    /**
     * Assign the stem on left
     * @param leftStem stem glyph
     */
    void setLeftStem (Glyph leftStem);

    //-------------//
    // getLeftStem //
    //-------------//
    /**
     * Report the stem attached on left side, if any
     * @return stem on left, or null
     */
    Glyph getLeftStem ();

    //------------------//
    // setPitchPosition //
    //------------------//
    /**
     * Setter for the pitch position, with respect to the containing staff
     * @param pitchPosition the pitch position wrt the staff
     */
    void setPitchPosition (double pitchPosition);

    //------------------//
    // getPitchPosition //
    //------------------//
    /**
     * Report the pitchPosition feature (position relative to the staff)
     * @return the pitchPosition value
     */
    double getPitchPosition ();

    //--------------//
    // setRightStem //
    //--------------//
    /**
     * Assign the stem on right
     * @param rightStem stem glyph
     */
    void setRightStem (Glyph rightStem);

    //--------------//
    // getRightStem //
    //--------------//
    /**
     * Report the stem attached on right side, if any
     * @return stem on right, or null
     */
    Glyph getRightStem ();

    //---------------//
    // setStemNumber //
    //---------------//
    /**
     * Remember the number of stems near by
     * @param stemNumber the number of stems
     */
    void setStemNumber (int stemNumber);

    //---------------//
    // getStemNumber //
    //---------------//
    /**
     * Report the number of stems the glyph is close to
     * @return the number of stems near by, typically 0, 1 or 2.
     */
    int getStemNumber ();

    //-----------------//
    // getSymbolsAfter //
    //-----------------//
    /**
     * Return the known glyphs stuck on last side of the stick (this is relevant
     * mainly for a stem glyph)
     * @param predicate the predicate to apply on each glyph
     * @param goods the set of correct glyphs (perhaps empty)
     * @param bads the set of non-correct glyphs (perhaps empty)
     */
    void getSymbolsAfter (Predicate<Glyph> predicate,
                          Set<Glyph>       goods,
                          Set<Glyph>       bads);

    //------------------//
    // getSymbolsBefore //
    //------------------//
    /**
     * Return the known glyphs stuck on first side of the stick (this is
     * relevant mainly for a stem glyph)
     * @param predicate the predicate to apply on each glyph
     * @param goods the set of correct glyphs (perhaps empty)
     * @param bads the set of non-correct glyphs (perhaps empty)
     */
    void getSymbolsBefore (Predicate<Glyph> predicate,
                           Set<Glyph>       goods,
                           Set<Glyph>       bads);

    //---------------//
    // setWithLedger //
    //---------------//
    /**
     * Remember info about ledger nearby
     * @param withLedger true is there is such ledger
     */
    void setWithLedger (boolean withLedger);

    //--------------//
    // isWithLedger //
    //--------------//
    /**
     * Report whether the glyph touches a ledger
     * @return true if there is a close ledger
     */
    boolean isWithLedger ();

    //---------------------//
    // copyStemInformation //
    //---------------------//
    /**
     * Forward stem-related information from the provided glyph
     * @param glyph the glyph whose stem information has to be used
     */
    void copyStemInformation (Glyph glyph);
}
