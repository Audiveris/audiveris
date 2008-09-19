//----------------------------------------------------------------------------//
//                                                                            //
//                        M e a s u r e E l e m e n t                         //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.entity;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.score.common.PixelRectangle;
import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;

import omr.sheet.Scale;

import java.util.*;

/**
 * Class <code>MeasureElement</code> is the basis for measure elements
 * (directions, notations, etc.)
 *
 * <p>For some elements (such as wedge, dashes, pedal, slur, tuplet), we may
 * have two "events": the starting event and the stopping event.
 * Both will trigger the creation of a MeasureElement instance, the difference
 * being made by the "start" boolean.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class MeasureElement
    extends MeasureNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    //~ Instance fields --------------------------------------------------------

    /** The precise shape */
    private Shape shape;

    /** The glyph(s) that compose this element, sorted by abscissa */
    private final SortedSet<Glyph> glyphs = new TreeSet<Glyph>();

    /** Is this a start (rather than a stop) */
    private final boolean start;

    /** Bounding box */
    private SystemRectangle box;

    /** Center point */
    private SystemPoint point;

    /** Related chord if any (in containing measure) */
    private final Chord chord;

    //~ Constructors -----------------------------------------------------------

    /** Creates a new instance
     * @param measure the containing measure
     * @param start is this a starting element (of a two-piece entity)
     * @param point the location within the system
     * @param chord the related chord, if any
     * @param glyph the underlying glyph
     */
    public MeasureElement (Measure     measure,
                           boolean     start,
                           SystemPoint point,
                           Chord       chord,
                           Glyph       glyph)
    {
        super(measure);

        this.start = start;
        this.point = point;
        this.chord = chord;

        if (glyph != null) {
            addGlyph(glyph);
        }
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
    // setPoint //
    //----------//
    public void setPoint (SystemPoint point)
    {
        this.point = point;
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
        setPoint(null);
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

        // Actual element class name
        String name = getClass()
                          .getName();
        int    period = name.lastIndexOf(".");
        sb.append("{")
          .append((period != -1) ? name.substring(period + 1) : name);

        try {
            // Shape
            sb.append(" ")
              .append(getShape());

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
        } catch (NullPointerException e) {
            sb.append(" INVALID");
        }

        sb.append(internalsString())
          .append("}");

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

        return measure.getEventChord(new SystemPoint(point.x + dx, point.y));
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
            setPoint(
                new SystemPoint(
                    box.x + (box.width / 2),
                    box.y + (box.height / 2)));
        }
    }

    //-----------------//
    // internalsString //
    //-----------------//
    /**
     * Return the string of the internals of this class, for inclusion in a
     * toString
     *
     * @return the string of internals
     */
    protected String internalsString ()
    {
        return "";
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Abscissa shift when looking for time slot (half a note head) */
        Scale.Fraction slotShift = new Scale.Fraction(
            0.5,
            "Abscissa shift when looking for time slot " +
            "(half a note head)");
    }
}
