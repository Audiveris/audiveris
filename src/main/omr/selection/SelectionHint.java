//----------------------------------------------------------------------------//
//                                                                            //
//                         S e l e c t i o n H i n t                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;


/**
 * Enum <code>SelectionHint</code> gives a hint about what observers should do
 * with the updated selection
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public enum SelectionHint {
    /**
     * Designation is by location pointing (either SCORE or SHEET), so we keep
     * the original location information, and try to lookup for designated Run,
     * Section & Glyph
     */
    LOCATION_INIT,
    /**
     * Designation is by location pointing (either SCORE or SHEET) while adding
     * to the existing selection(s), so we keep the original location
     * information, and try to lookup for designated Run, Section & Glyph
     */
    LOCATION_ADD, 
    /**
     * Designation is at Section level, so we display the pixel contour of the
     * Section, Run information is not available, and related Glyph information
     * is displayed
     */
    SECTION_INIT, 
    /**
     * Designation is at Glyph level, so we display the pixel contour of the
     * Glyph, as well as Glyph information, but Run & Section informations are
     * not available
     */
    GLYPH_INIT, 
    /**
     * Designation is at Glyph level, for which a characteristic (typically the
     * shape) has just been modified
     */
    GLYPH_MODIFIED, 
    /**
     * Glyph information is for temporary display / evaluation only, with no
     * impact on other structures such as glyph set.
     */
    GLYPH_TRANSIENT;
}
