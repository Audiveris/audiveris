//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S e l e c t i o n H i n t                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

/**
 * Enum {@code SelectionHint} gives a hint about what observers should
 * do with the published selection.
 *
 * @author Hervé Bitteur
 */
public enum SelectionHint
{

    /**
     * Designation is by location pointing, so we keep the original location
     * information, and try to lookup for designated Run, Section and Glyph
     * <br>[MouseLeft]
     */
    LOCATION_INIT,
    /**
     * Designation is by location pointing while adding to the existing selection(s), so we keep the
     * original location information, and try to lookup for designated Run, Section and Glyph
     * <br>[CTRL + MouseLeft]
     */
    LOCATION_ADD,
    /**
     * Designation is by context pointing, discarding any previous selection
     * <br>[MouseRight]
     */
    CONTEXT_INIT,
    /**
     * Designation is by context pointing while keeping the existing selection(s) if any
     * <br>[CTRL + MouseRight]
     */
    CONTEXT_ADD,
    /**
     * Designation is at Section level, so we display the pixel contour of the Section, Run
     * information is not available, and related Glyph information is displayed
     */
    SECTION_INIT,
    /**
     * Designation is at Glyph level, for which a characteristic (typically the shape) has just been
     * modified
     */
    GLYPH_MODIFIED,
    /**
     * Glyph information is for temporary display / evaluation only, with no impact on other
     * structures such as glyph set
     */
    GLYPH_TRANSIENT,
    /**
     * Designation is at entity list level
     */
    ENTITY_INIT,
    /**
     * Designation is at entity list level, while adding to the existing selection(s)
     */
    ENTITY_ADD;
    //

    //------------//
    // isLocation //
    //------------//
    /**
     * Predicate for LOCATION_XXX.
     *
     * @return true for location-related hints
     */
    public boolean isLocation ()
    {
        switch (this) {
        case LOCATION_INIT:
        case LOCATION_ADD:
            return true;
        }

        return false;
    }

    //-----------//
    // isContext //
    //-----------//
    /**
     * Predicate for CONTEXT_XXX.
     *
     * @return true for context-related hints
     */
    public boolean isContext ()
    {
        switch (this) {
        case CONTEXT_INIT:
        case CONTEXT_ADD:
            return true;
        }

        return false;
    }
//
//    //-----------//
//    // isSection //
//    //-----------//
//    /**
//     * Predicate for SECTION_XXX.
//     *
//     * @return true for section-related hints
//     */
//    public boolean isSection ()
//    {
//        switch (this) {
//        case SECTION_INIT:
//            return true;
//        }
//
//        return false;
//    }
//
//    //---------//
//    // isGlyph //
//    //---------//
//    /**
//     * Predicate for GLYPH_XXX.
//     *
//     * @return true for glyph-related hints
//     */
//    public boolean isGlyph ()
//    {
//        switch (this) {
//        case GLYPH_INIT:
//        case GLYPH_MODIFIED:
//        case GLYPH_TRANSIENT:
//            return true;
//        }
//
//        return false;
//    }
}
