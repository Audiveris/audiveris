//-----------------------------------------------------------------------//
//                                                                       //
//                        S e l e c t i o n T a g                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.selection;

/**
 * Enum <code>SelectionTag</code> allows a convenient handling of selection
 * objects.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public enum SelectionTag
{
    /** Current Sheet, entity type is a Sheet. */
    SHEET,

        /** Rectangle in Score display, entity type is a Rectangle. */
        SCORE,

        /** Rectangle in Sheet display, entity type is a Rectangle. */
        PIXEL,

        /** Pixel grey level, entity type is an Integer. */
        LEVEL,

        /** Run of initial skewed lag, entity type is a Run. */
        SKEW_RUN,

        /** Section of initial skewed lag, entity type is a Section. */
        SKEW_SECTION,

        /** Section Id of initial skewed lag, entity type is an Integer. */
        SKEW_SECTION_ID,

        /** Run of horizontal lag, entity type is a Run. */
        HORIZONTAL_RUN,

        /** Section of horizontal lag, entity type is a Section. */
        HORIZONTAL_SECTION,

        /** Section Id of horizontal lag, entity type is an Integer. */
        HORIZONTAL_SECTION_ID,

        /** A horizontal glyph, entity type is a Glyph. */
        HORIZONTAL_GLYPH,

        /** A horizontal glyph id, entity type is an Integer. */
        HORIZONTAL_GLYPH_ID,

        /** Run of vertical lag, entity type is a Run. */
        VERTICAL_RUN,

        /** Section of vertical lag, entity type is a Section. */
        VERTICAL_SECTION,

        /** Section Id of vertical lag, entity type is an Integer. */
        VERTICAL_SECTION_ID,

        /** A vertical glyph, entity type is a Glyph. */
        VERTICAL_GLYPH,

        /** A vertical glyph id, entity type is an Integer. */
        VERTICAL_GLYPH_ID,

        /** Set of (vertical) glyphs, entity type is a Collection of
            Glyphs. */
        GLYPH_SET;
}
