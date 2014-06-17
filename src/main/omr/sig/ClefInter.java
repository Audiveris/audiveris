//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        C l e f I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class {@code ClefInter} handles a Clef interpretation.
 * <p>
 * The following image, directly pulled from wikipedia, explains the most popular clefs today
 * (Treble, Alto, Tenor and Bass) and for each presents where the "Middle C" note (C4) would take
 * place.
 * These informations are used by methods {@link #octaveOf(omr.score.entity.Clef, int)} and
 * {@link #noteStepOf(omr.score.entity.Clef, int)}.</p>
 * <p>
 * <img
 * src="http://upload.wikimedia.org/wikipedia/commons/thumb/1/17/Middle_C_in_four_clefs.svg/600px-Middle_C_in_four_clefs.svg.png"
 * />
 * <p>
 * Step line of the clef : -4 for top line (Baritone), -2 for Bass and Tenor,
 * 0 for Alto, +2 for Treble and Mezzo-Soprano, +4 for bottom line (Soprano).
 *
 * @author Hervé Bitteur
 */
public class ClefInter
        extends AbstractPitchedInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ClefInter.class);

    /** Map of sharp pitches per clef kind. */
    public static final Map<Kind, int[]> sharpsMap = getSharpsMap();

    /** Map of flat pitches per clef kind. */
    public static final Map<Kind, int[]> flatsMap = getFlatsMap();

    //~ Enumerations -------------------------------------------------------------------------------
    public static enum Kind
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        TREBLE(Shape.G_CLEF, 2),
        BASS(Shape.F_CLEF, -2),
        ALTO(Shape.C_CLEF, 0),
        TENOR(Shape.C_CLEF, -2),
        PERCUSSION(Shape.PERCUSSION_CLEF, 0);

        //~ Instance fields ------------------------------------------------------------------------
        /** Symbol shape class. (regardless of ottava mark if any) */
        public final Shape shape;

        /** Pitch of reference line. */
        public final int pitch;

        //~ Constructors ---------------------------------------------------------------------------
        Kind (Shape shape,
              int pitch)
        {
            this.shape = shape;
            this.pitch = pitch;
        }
    }

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ClefInter object.
     *
     * @param glyph the glyph to interpret
     * @param shape the possible shape
     * @param grade the interpretation quality
     * @param pitch pitch position
     */
    public ClefInter (Glyph glyph,
                      Shape shape,
                      double grade,
                      int pitch)
    {
        super(glyph, null, shape, grade, pitch);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //--------//
    // create //
    //--------//
    /**
     * Create a Clef inter.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @param staff related staff
     * @return the created instance or null if failed
     */
    public static ClefInter create (Glyph glyph,
                                    Shape shape,
                                    double grade,
                                    StaffInfo staff)
    {
        switch (shape) {
        case G_CLEF:
        case G_CLEF_SMALL:
        case G_CLEF_8VA:
        case G_CLEF_8VB:
            return new ClefInter(glyph, shape, grade, 2);

        case C_CLEF:

            // Depending on precise clef position, we can have
            // an Alto C-clef (pp=0) or a Tenor C-clef (pp=-2)
            Point center = glyph.getLocation();
            int pp = (int) Math.rint(staff.pitchPositionOf(center));

            return new ClefInter(glyph, shape, grade, pp);

        case F_CLEF:
        case F_CLEF_SMALL:
        case F_CLEF_8VA:
        case F_CLEF_8VB:
            return new ClefInter(glyph, shape, grade, -2);

        case PERCUSSION_CLEF:
            return new ClefInter(glyph, shape, grade, 0);

        default:
            return null;
        }
    }

    //-----------//
    // guessKind //
    //-----------//
    public static Kind guessKind (Shape shape,
                                  Double[] measuredPitches)
    {
        Map<Kind, int[]> map = (shape == Shape.FLAT) ? flatsMap : sharpsMap;
        Map<Kind, Double> results = new EnumMap<Kind, Double>(Kind.class);
        Kind bestKind = null;
        double bestError = Double.MAX_VALUE;

        for (Entry<Kind, int[]> entry : map.entrySet()) {
            Kind kind = entry.getKey();
            int[] pitches = entry.getValue();
            int count = 0;
            double error = 0;

            for (int i = 0; i < measuredPitches.length; i++) {
                Double measured = measuredPitches[i];

                if (measured != null) {
                    count++;

                    double diff = measured - pitches[i];
                    error += (diff * diff);
                }
            }

            if (count > 0) {
                error /= count;
                error = Math.sqrt(error);
                results.put(kind, error);

                if (error < bestError) {
                    bestError = error;
                    bestKind = kind;
                }
            }
        }

        logger.debug("{} results:{}", bestKind, results);

        return bestKind;
    }

    //--------//
    // kindOf //
    //--------//
    public static Kind kindOf (Glyph glyph,
                               Shape shape,
                               StaffInfo staff)
    {
        switch (shape) {
        case G_CLEF:
        case G_CLEF_SMALL:
        case G_CLEF_8VA:
        case G_CLEF_8VB:
            return Kind.TREBLE;

        case C_CLEF:

            // Disambiguate between Alto C-clef (pp=0) and Tenor C-clef (pp=-2)
            Point center = glyph.getLocation();
            int pp = (int) Math.rint(staff.pitchPositionOf(center));

            return (pp >= -1) ? Kind.ALTO : Kind.TENOR;

        case F_CLEF:
        case F_CLEF_SMALL:
        case F_CLEF_8VA:
        case F_CLEF_8VB:
            return Kind.BASS;

        case PERCUSSION_CLEF:
            return Kind.PERCUSSION;

        default:
            return null;
        }
    }

    //-------------//
    // getFlatsMap //
    //-------------//
    private static Map<Kind, int[]> getFlatsMap ()
    {
        Map<Kind, int[]> map = new EnumMap<Kind, int[]>(Kind.class);
        map.put(Kind.TREBLE, new int[]{0, -3, 1, -2, 2, -1, 3});
        map.put(Kind.ALTO, new int[]{1, -2, 2, -1, 3, 0, 4});
        map.put(Kind.BASS, new int[]{2, -1, 3, 0, 4, 1, 5});
        map.put(Kind.TENOR, new int[]{-1, -4, 0, -3, 1, -2, 2});

        // No entry for Percussion
        return map;
    }

    //--------------//
    // getSharpsMap //
    //--------------//
    private static Map<Kind, int[]> getSharpsMap ()
    {
        Map<Kind, int[]> map = new EnumMap<Kind, int[]>(Kind.class);
        map.put(Kind.TREBLE, new int[]{-4, -1, -5, -2, 1, -3, 0});
        map.put(Kind.ALTO, new int[]{-3, 0, -4, -1, 2, -2, 1});
        map.put(Kind.BASS, new int[]{-2, 1, -3, 0, 3, -1, 2});
        map.put(Kind.TENOR, new int[]{2, -2, 1, -3, 0, -4, -1});

        // No entry for Percussion
        return map;
    }
}
