//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          C o l o r s                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
 * Class {@code Colors} gathers in one place the various colors used by
 * Audiveris displays, in order to ensure consistency and compatibility.
 *
 * @author Hervé Bitteur
 */
public abstract class Colors
{

    /** Global alpha transparency (0..255). */
    private static final int alpha = 180;

    // Color names below should better be listed alphabetically
    //
    /** Annotations. */
    public static final Color ANNOTATION = Color.LIGHT_GRAY;

    /** Attachments. */
    public static final Color ATTACHMENT = Color.PINK;

    /** Function values in charts. */
    public static final Color CHART_VALUE = Color.RED;

    /** Function derivatives in charts. */
    public static final Color CHART_DERIVATIVE = Color.GREEN;

    /** Derivative hi-los in charts. */
    public static final Color CHART_HILO = Color.CYAN;

    /** Peaks in charts. */
    public static final Color CHART_PEAK = Color.YELLOW;

    /** Zero line in charts. */
    public static final Color CHART_ZERO = Color.WHITE;

    /** Filament combs of main interline. */
    public static final Color COMB = new Color(220, 200, 150, alpha);

    /** Filament combs of second interline. */
    public static final Color COMB_MINOR = new Color(200, 200, 0, alpha);

    /** Color for standard distance. */
    public static final Color DISTANCE_STANDARD = Color.BLUE;

    /** Color for zero distance (on foreground target). */
    public static final Color DISTANCE_TARGET = Color.PINK;

    /** Color for unknown distance (neutralized locations). */
    public static final Color DISTANCE_UNKNOWN = Color.GREEN;

    /** Successful entities. */
    public static final Color ENTITY = Color.BLACK;

    /** Past successful entities (such as staff lines). */
    public static final Color ENTITY_MINOR = Color.GRAY;

    /** Horizontal stuff. */
    public static final Color GRID_HORIZONTAL = new Color(255, 200, 200);

    /** Vertical stuff. */
    public static final Color GRID_VERTICAL = new Color(200, 200, 255);

    /** Inter in abnormal state. */
    public static final Color INTER_ABNORMAL = Color.RED;

    /** Music for picture view. */
    public static final Color MUSIC_ALONE = Color.BLACK;

    /** Music for mixed picture view. */
    public static final Color MUSIC_PICTURE = new Color(80, 255, 80, alpha);

    /** Music for mixed symbols view. */
    public static final Color MUSIC_SYMBOLS = new Color(170, 255, 170, alpha);

    /** Color for a progress bar. */
    public static final Color PROGRESS_BAR = Color.ORANGE;

    /** Rubber rectangle. */
    public static final Color RUBBER_RECT = Color.RED;

    /** Rubber rule / cross. */
    public static final Color RUBBER_RULE = new Color(255, 0, 0, 100);

    // Colors for physical score view
    /** frame: barlines, brackets, clefs, markers, time signatures. */
    public static final Color SCORE_FRAME = Color.BLUE;

    /** frame: heads, beams, flags, rests, augmentation dots. */
    public static final Color SCORE_NOTES = new Color(0x008844);

    /** frame: accidentals, keys, articulations, attributes, dynamics, etc. */
    public static final Color SCORE_MODIFIERS = new Color(0x992299);

    /** frame: stems, ledgers, normal text. */
    public static final Color SCORE_PHYSICALS = new Color(0x806040);

    /** frame: lyric text. */
    public static final Color SCORE_LYRICS = Color.BLUE;

    /** Unknown shape. */
    public static final Color SHAPE_UNKNOWN = Color.LIGHT_GRAY;

    /** Color for busy sheet tab. */
    public static final Color SHEET_BUSY = Color.ORANGE;

    /** Color for invalid sheet tab. */
    public static final Color SHEET_INVALID = Color.PINK;

    /** Color for not loaded sheet tab. */
    public static final Color SHEET_NOT_LOADED = Color.LIGHT_GRAY;

    /** Color for not OK sheet tab. */
    public static final Color SHEET_NOT_OK = Color.RED;

    /** Color for OK sheet tab. */
    public static final Color SHEET_OK = Color.BLACK;

    /** Time slot. */
    public static final Color SLOT = new Color(192, 192, 192, alpha);

    /** Current time slot. */
    public static final Color SLOT_CURRENT = new Color(255, 0, 255, alpha);

    /** Stack detected as abnormal. */
    public static final Color STACK_ABNORMAL = new Color(255, 225, 225);

    /** Brace staff peak. */
    public static final Color STAFF_PEAK_BRACE = new Color(0, 255, 255, 100);

    /** Bracket staff peak. */
    public static final Color STAFF_PEAK_BRACKET = new Color(255, 255, 0, 180);

    /** Standard staff peak. */
    public static final Color STAFF_PEAK = new Color(255, 200, 0, 100);

    /** Tangents. */
    public static final Color TANGENT = new Color(0, 0, 0, 100);

    /** System kind of bracket. */
    public static final Color SYSTEM_BRACKET = new Color(255, 255, 0, alpha);

    /** Warping points. */
    public static final Color WARP_POINT = Color.RED;

    /**
     * Not meant to be instantiated.
     */
    private Colors ()
    {
    }
}
