//----------------------------------------------------------------------------//
//                                                                            //
//                             D i r e c t i o n                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.sheet.PixelPoint;
import omr.sheet.PixelRectangle;
import omr.sheet.Scale;

import java.util.*;

/**
 * Class <code>Direction</code> is the basis for all variants of direction
 * indications: pedal, words, dynamics, wedge, dashes, etc...
 *
 * <p>For some directions (such as wedge, dashes, pedal), we may have two
 * "events": the starting event and the stopping event. Both will trigger the
 * creation of a Direction instance, the difference being made by the "start"
 * boolean.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class Direction
    extends MeasureNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    protected static final Constants constants = new Constants();

    //~ Instance fields --------------------------------------------------------

    /** The precise direction shape */
    private Shape shape;

    /** The glyph(s) that compose this direction, sorted by abscissa */
    private final SortedSet<Glyph> glyphs = new TreeSet<Glyph>();

    /** Is this a start (or a stop) */
    private final boolean start;

    /** Bounding box */
    private SystemRectangle box;

    /** Edge point */
    private SystemPoint point;

    /** Edge chord (in containing measure) */
    private final Chord chord;

    //~ Constructors -----------------------------------------------------------

    /** Creates a new instance of Direction */
    public Direction (boolean     start,
                      Measure     measure,
                      SystemPoint point,
                      Chord       chord)
    {
        super(measure);
        this.start = start;
        this.point = point;
        this.chord = chord;

        chord.addEvent(this);
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // getBox //
    //--------//
    public SystemRectangle getBox ()
    {
        if (box == null) {
            computeGeometry();
        }

        return box;
    }

    //----------//
    // getChord //
    //----------//
    public Chord getChord ()
    {
        return chord;
    }

    //----------//
    // getGlyph //
    //----------//
    public Glyph getGlyph ()
    {
        return glyphs.first();
    }

    //-----------//
    // getGlyphs //
    //-----------//
    public SortedSet<Glyph> getGlyphs ()
    {
        return glyphs;
    }

    //----------//
    // getPoint //
    //----------//
    public SystemPoint getPoint ()
    {
        if (point == null) {
            computeGeometry();
        }

        return point;
    }

    //----------//
    // getShape //
    //----------//
    public Shape getShape ()
    {
        if (shape == null) {
            shape = computeShape();
        }

        return shape;
    }

    //---------//
    // isStart //
    //---------//
    public boolean isStart ()
    {
        return start;
    }

    //----------//
    // addGlyph //
    //----------//
    public void addGlyph (Glyph glyph)
    {
        // Reset
        shape = null;
        point = null;
        box = null;

        glyphs.add(glyph);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        // Shape
        sb.append(getShape());

        // Start ?
        if (!isStart()) {
            sb.append("/stop");
        }

        // Chord
        sb.append(" ")
          .append(chord.getContextString());

        // Point
        sb.append(" point[x=")
          .append(getPoint().x)
          .append(",y=")
          .append(getPoint().y)
          .append("]");

        // Box
        sb.append(" box[x=")
          .append(getBox().x)
          .append(",y=")
          .append(getBox().y)
          .append(",w=")
          .append(getBox().width)
          .append(",h=")
          .append(getBox().height)
          .append("]");

        // Glyphs
        sb.append(Glyph.toString(glyphs));

        return sb.toString();
    }

    //--------------//
    // computeShape //
    //--------------//
    protected Shape computeShape ()
    {
        return getGlyph()
                   .getShape();
    }

    //-----------//
    // findChord //
    //-----------//
    protected static Chord findChord (Measure     measure,
                                      SystemPoint point)
    {
        // Shift on abscissa (because of left side of note heads)
        int dx = measure.getSystem()
                        .getScale()
                        .toUnits(constants.slotShift);

        return measure.findEventChord(new SystemPoint(point.x + dx, point.y));
    }

    //-----------------//
    // computeGeometry //
    //-----------------//
    protected void computeGeometry ()
    {
        PixelRectangle pbox = null;

        for (Glyph glyph : glyphs) {
            if (pbox == null) {
                pbox = glyph.getContourBox();
            } else {
                pbox.union(glyph.getContourBox());
            }
        }

        if (pbox != null) {
            box = getSystem()
                      .toSystemRectangle(pbox);
            point = new SystemPoint(
                box.x + (box.width / 2),
                box.y + (box.height / 2));
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /** Abscissa shift when looking for time slot (half a note head) */
        Scale.Fraction slotShift = new Scale.Fraction(
            0.5,
            "Abscissa shift when looking for time slot " +
            "(half a note head)");
    }
}
