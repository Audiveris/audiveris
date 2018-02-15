//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          C o l o r s                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.ui;

import java.awt.Color;

/**
 * Class {@code Colors} gathers alphabetically in one place the various colors used by
 * Audiveris displays, in order to ensure consistency and compatibility.
 *
 * @author Hervé Bitteur
 */
public class Colors
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Global alpha transparency (0..255) */
    private static final int alpha = 180;

    /** Annotations */
    public static final Color ANNOTATION = Color.LIGHT_GRAY;

    /** Attachments */
    public static final Color ATTACHMENT = Color.PINK;

    /** Filament combs of main interline */
    public static final Color COMB = new Color(220, 200, 150, alpha);

    /** Filament combs of second interline */
    public static final Color COMB_MINOR = new Color(200, 200, 0, alpha);

    /** Color for standard distance */
    public static final Color DISTANCE_STANDARD = Color.BLUE;

    /** Color for zero distance (on foreground target) */
    public static final Color DISTANCE_TARGET = Color.PINK;

    /** Color for unknown distance (neutralized locations) */
    public static final Color DISTANCE_UNKNOWN = Color.GREEN;

    /** Successful entities */
    public static final Color ENTITY = Color.BLACK;

    /** Past successful entities (such as staff lines) */
    public static final Color ENTITY_MINOR = Color.GRAY;

    /** Virtual entities */
    public static final Color ENTITY_VIRTUAL = Color.LIGHT_GRAY;

    /** Barred evaluation */
    public static final Color EVALUATION_BARRED = Color.PINK;

    /** Glyph area */
    public static final Color GLYPH_AREA = Color.BLACK;

    /** Glyph area (for XORMode, no longer used) */
    public static final Color GLYPH_AREA_XOR = Color.DARK_GRAY;

    /** Current glyph. */
    public static final Color GLYPH_CURRENT = new Color(255, 0, 255, alpha);

    /** Horizontal stuff */
    public static final Color GRID_HORIZONTAL = new Color(255, 200, 200);

    /** Vertical stuff */
    public static final Color GRID_VERTICAL = new Color(200, 200, 255);

    /** Barline-shape glyphs */
    public static final Color GRID_VERTICAL_SHAPED = new Color(150, 150, 255);

    /** Barline glyphs */
    public static final Color GRID_BARLINE = new Color(0, 0, 255, 100);

    /** Hidden entity */
    public static final Color HIDDEN = Color.white;

    /** Vertical filament lines and points */
    public static final Color LINE_VERTICAL = new Color(0, 255, 255, alpha);

    /** Music for picture view */
    public static final Color MUSIC_ALONE = Color.BLACK;

    /** Music for mixed picture view */
    public static final Color MUSIC_PICTURE = new Color(80, 255, 80, alpha);

    /** Music for mixed symbols view */
    public static final Color MUSIC_SYMBOLS = new Color(170, 255, 170, alpha);

    /** Function values in charts. */
    public static final Color CHART_VALUE = Color.RED;

    /** Function derivatives in charts. */
    public static final Color CHART_DERIVATIVE = Color.GREEN;

    /** Derivative hilos in charts. */
    public static final Color CHART_HILO = Color.CYAN;

    /** Peaks in charts. */
    public static final Color CHART_PEAK = Color.YELLOW;

    /** Zero line in charts. */
    public static final Color CHART_ZERO = Color.WHITE;

    /** Color for a progress bar */
    public static final Color PROGRESS_BAR = Color.ORANGE;

    /** Rubber rectangle */
    public static final Color RUBBER_RECT = Color.RED;

    /** Rubber rule / cross */
    public static final Color RUBBER_RULE = new Color(255, 0, 0, 100);

    /** Sentence baseline */
    public static final Color SENTENCE_BASELINE = Color.RED;

    /** Sentence gaps among words */
    public static final Color SENTENCE_GAPS = new Color(100, 100, 255, 50);

    /** Inter in abnormal state. */
    public static final Color INTER_ABNORMAL = Color.RED;

    /** Unknown shape */
    public static final Color SHAPE_UNKNOWN = Color.LIGHT_GRAY;

    /** Known shape */
    public static final Color SHAPE_KNOWN = Color.GREEN;

    /** Color for busy sheet tab */
    public static final Color SHEET_BUSY = Color.ORANGE;

    /** Color for invalid sheet tab */
    public static final Color SHEET_INVALID = Color.PINK;

    /** Color for not loaded sheet tab */
    public static final Color SHEET_NOT_LOADED = Color.LIGHT_GRAY;

    /** Color for not OK sheet tab */
    public static final Color SHEET_NOT_OK = Color.RED;

    /** Color for OK sheet tab */
    public static final Color SHEET_OK = Color.BLACK;

    /** Time slot */
    public static final Color SLOT = new Color(192, 192, 192, alpha);

    /** Current time slot */
    public static final Color SLOT_CURRENT = new Color(255, 0, 255, alpha);

    /** Stack detected as abnormal. */
    public static final Color STACK_ABNORMAL = new Color(255, 225, 225);

    /** Brace staff peak */
    public static final Color STAFF_PEAK_BRACE = new Color(0, 255, 255, 100);

    /** Bracket staff peak */
    public static final Color STAFF_PEAK_BRACKET = new Color(255, 255, 0, 180);

    /** Standard staff peak */
    public static final Color STAFF_PEAK = new Color(255, 200, 0, 100);

    /** Tangents */
    public static final Color TANGENT = new Color(0, 0, 0, 100);

    /** Translation links */
    public static final Color TRANSLATION_LINK = Color.RED;

    /** System kind of bracket */
    public static final Color SYSTEM_BRACKET = new Color(255, 255, 0, alpha);

    /** Warping points */
    public static final Color WARP_POINT = Color.RED;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Not meant to be instantiated.
     */
    private Colors ()
    {
    }
}
