//----------------------------------------------------------------------------//
//                                                                            //
//                                C o l o r s                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import java.awt.Color;

/**
 * Class {@code Colors} gathers alphabetically in one place the various
 * colors used by Audiveris displays, in order to ensure consistency
 * and compatibility.
 *
 * @author Hervé Bitteur
 */
public class Colors
{
    //~ Static fields/initializers ---------------------------------------------

    /** Global alpha transparency (0..255) */
    private static final int alpha = 180;

    /** Annotations */
    public static final Color ANNOTATION = Color.lightGray;

    /** Attachments */
    public static final Color ATTACHMENT = Color.pink;

    /** Filament combs of main interline */
    public static final Color COMB = new Color(220, 200, 150, alpha);

    /** Filament combs of second interline */
    public static final Color COMB_MINOR = new Color(200, 200, 0, alpha);

    /** Successful entities */
    public static final Color ENTITY = Color.black;

    /** Past successful entities */
    public static final Color ENTITY_MINOR = Color.lightGray;

    /** Virtual entities */
    public static final Color ENTITY_VIRTUAL = Color.lightGray;

    /** Barred evaluation */
    public static final Color EVALUATION_BARRED = Color.pink;

    /** Glyph area */
    public static final Color GLYPH_AREA = Color.black;

    /** Glyph area (for XORMode) */
    public static final Color GLYPH_AREA_XOR = Color.darkGray;

    /** Horizontal stuff */
    public static final Color GRID_HORIZONTAL = new Color(255, 230, 230);

    /** Horizontal active glyphs */
    public static final Color GRID_HORIZONTAL_ACTIVE = new Color(255, 200, 200);

    /** Vertical stuff */
    public static final Color GRID_VERTICAL = new Color(220, 220, 255);

    /** Barline-shape glyphs */
    public static final Color GRID_VERTICAL_SHAPED = new Color(150, 150, 255);

    /** Barline glyphs */
    public static final Color GRID_BARLINE = Color.blue;

    /** Hidden entity */
    public static final Color HIDDEN = Color.white;

    /** Vertical filament lines & points */
    public static final Color LINE_VERTICAL = new Color(0, 255, 255, alpha);

    /** Music for picture view */
    public static final Color MUSIC_ALONE = Color.black;

    /** Music for mixed picture view */
    public static final Color MUSIC_PICTURE = new Color(80, 255, 80, alpha);

    /** Music for mixed symbols view */
    public static final Color MUSIC_SYMBOLS = new Color(170, 255, 170, alpha);

    /** Rubber rectangle */
    public static final Color RUBBER_RECT = Color.black;

    /** Rubber rule / cross */
    public static final Color RUBBER_RULE = new Color(255, 0, 0, 100);

    /** Sentence baseline */
    public static final Color SENTENCE_BASELINE = Color.red;

    /** Sentence gaps among words */
    public static final Color SENTENCE_GAPS = new Color(100, 100, 255, 50);

    /** Unknown shape */
    public static final Color SHAPE_UNKNOWN = Color.red;

    /** Known shape */
    public static final Color SHAPE_KNOWN = Color.green;

    /** Time slot */
    public static final Color SLOT = new Color(192, 192, 192, alpha);

    /** Current time slot */
    public static final Color SLOT_CURRENT = new Color(255, 0, 255, alpha);

    /** Tangents */
    public static final Color TANGENT = new Color(0, 0, 0, 100);

    /** Translation links */
    public static final Color TRANSLATION_LINK = Color.red;

    /** System kind of bracket */
    public static final Color SYSTEM_BRACKET = new Color(255, 255, 0, alpha);

    /** Warping points */
    public static final Color WARP_POINT = Color.red;

    //~ Constructors -----------------------------------------------------------
    /**
     * Not meant to be instantiated.
     */
    private Colors ()
    {
    }
}
