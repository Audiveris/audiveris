//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          C o l o r s                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

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
    public static final Color ENTITY_MINOR = new Color(192, 192, 192);

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

    /** Vertical filament lines & points */
    public static final Color LINE_VERTICAL = new Color(0, 255, 255, alpha);

    /** Music for picture view */
    public static final Color MUSIC_ALONE = Color.BLACK;

    /** Music for mixed picture view */
    public static final Color MUSIC_PICTURE = new Color(80, 255, 80, alpha);

    /** Music for mixed symbols view */
    public static final Color MUSIC_SYMBOLS = new Color(170, 255, 170, alpha);

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

    /** Unknown shape */
    public static final Color SHAPE_UNKNOWN = Color.RED;

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
