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
 * Enum <code>Tag</code> allows a convenient handling of selection
 * objects.
 */
public enum SelectionTag
{
    /** Current Sheet. Entity type is a Sheet */
    SHEET,

        /** Rectangle in Score display. Entity type is a Rectangle. */
        SCORE,

        /** Rectangle in Sheet display. Entity type is a Rectangle. */
        PIXEL,

        /** Pixel grey level. Entity type is an Integer. */
        LEVEL,

        /** Run of initial skewed lag. Entity type is a Run. */
        SKEW_RUN,

        /** Section of initial skewed lag. Entity type is a Section. */
        SKEW_SECTION,

        /** Section Id of initial skewed lag. Entity type is an Integer. */
        SKEW_SECTION_ID,

        /** Run of horizontal lag. Entity type is a Run. */
        HORIZONTAL_RUN,

        /** Section of horizontal lag. Entity type is a Section. */
        HORIZONTAL_SECTION,

        /** Section Id of horizontal lag. Entity type is an Integer. */
        HORIZONTAL_SECTION_ID,

        /** A horizontal glyph. Entity type is a Glyph. */
        HORIZONTAL_GLYPH,

        /** A horizontal glyph id. Entity type is an Integer. */
        HORIZONTAL_GLYPH_ID,

        /** Run of vertical lag. Entity type is a Run. */
        VERTICAL_RUN,

        /** Section of vertical lag. Entity type is a Section. */
        VERTICAL_SECTION,

        /** Section Id of vertical lag. Entity type is an Integer. */
        VERTICAL_SECTION_ID,

        /** A vertical glyph. Entity type is a Glyph. */
        VERTICAL_GLYPH,

        /** A vertical glyph id. Entity type is an Integer. */
        VERTICAL_GLYPH_ID,

        /** Set of (vertical) glyphs. Entity type is a Collection of
            Glyphs. */
        GLYPH_SET;
}

