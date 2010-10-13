//----------------------------------------------------------------------------//
//                                                                            //
//                                 G l y p h                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.check.Checkable;

import java.awt.Point;
import java.util.Comparator;

/**
 * Interface {@code Glyph} represents any glyph found, such as stem, ledger,
 * accidental, note head, etc...
 *
 * <p>A Glyph is basically a collection of sections. It can be split into
 * smaller glyphs, which may later be re-assembled into another instance of
 * glyph. There is a means, based on a simple signature (weight and bounding
 * box), to detect if the glyph at hand is identical to a previous one, which is
 * then reused.
 *
 * <p>A Glyph can be stored on disk and reloaded in order to train a glyph
 * evaluator.
 *
 * @author Hervé Bitteur
 */
public interface Glyph
    extends 
/** For handling check results     */ Checkable, 
/** For id and related lag         */ GlyphAdministration, 
/** For member sections            */ GlyphComposition, 
/** For display color              */ GlyphDisplay, 
/** For items nearby               */ GlyphEnvironment, 
/** For physical appearance        */ GlyphGeometry, 
/** For shape assignment           */ GlyphRecognition, 
/** For transtation to score items */ GlyphTranslation
{
    //~ Static fields/initializers ---------------------------------------------

    /** For comparing glyphs according to their height */
    public static final Comparator<Glyph> heightComparator = new Comparator<Glyph>() {
        public int compare (Glyph o1,
                            Glyph o2)
        {
            return o1.getContourBox().height - o2.getContourBox().height;
        }
    };

    /** For comparing glyphs according to their decreasing weight */
    public static final Comparator<Glyph> reverseWeightComparator = new Comparator<Glyph>() {
        public int compare (Glyph o1,
                            Glyph o2)
        {
            return o2.getWeight() - o1.getWeight();
        }
    };

    /** For comparing glyphs according to their id */
    public static final Comparator<Glyph> idComparator = new Comparator<Glyph>() {
        public int compare (Glyph o1,
                            Glyph o2)
        {
            return o1.getId() - o2.getId();
        }
    };

    /** For comparing glyphs according to their abscissa, then ordinate, then id */
    public static final Comparator<Glyph> globalComparator = new Comparator<Glyph>() {
        public int compare (Glyph o1,
                            Glyph o2)
        {
            if (o1 == o2) {
                return 0;
            }

            Point ref = o1.getContourBox()
                          .getLocation();
            Point otherRef = o2.getContourBox()
                               .getLocation();

            // Are x values different?
            int dx = ref.x - otherRef.x;

            if (dx != 0) {
                return dx;
            }

            // Vertically aligned, so use ordinates
            int dy = ref.y - otherRef.y;

            if (dy != 0) {
                return dy;
            }

            // Finally, use id ...
            return o1.getId() - o2.getId();
        }
    };

    /** For comparing glyphs according to their ordinate, then abscissa, then id */
    public static final Comparator<Glyph> ordinateComparator = new Comparator<Glyph>() {
        public int compare (Glyph o1,
                            Glyph o2)
        {
            if (o1 == o2) {
                return 0;
            }

            Point ref = o1.getContourBox()
                          .getLocation();
            Point otherRef = o2.getContourBox()
                               .getLocation();

            // Are y values different?
            int dy = ref.y - otherRef.y;

            if (dy != 0) {
                return dy;
            }

            // Horizontally aligned, so use abscissae
            int dx = ref.x - otherRef.x;

            if (dx != 0) {
                return dx;
            }

            // Finally, use id ...
            return o1.getId() - o2.getId();
        }
    };
}
