//----------------------------------------------------------------------------//
//                                                                            //
//                        M e a s u r e E l e m e n t                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.ConstantSet;

import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.sheet.Scale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code MeasureElement} is the basis for measure elements
 * (directions, notations, etc.)
 *
 * <p>For some elements (such as wedge, dashes, pedal, slur, tuplet), we may
 * have two "events": the starting event and the stopping event.
 * Both will trigger the creation of a MeasureElement instance, the difference
 * being made by the "start" boolean.
 *
 * @author Hervé Bitteur
 */
public abstract class MeasureElement
        extends MeasureNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            MeasureElement.class);

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    //~ Instance fields --------------------------------------------------------
    /** The precise shape */
    private Shape shape;

    /** Is this a start (rather than a stop) */
    private final boolean start;

    /** Related chord if any (in containing measure) */
    private final Chord chord;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new instance
     *
     * @param measure        the containing measure
     * @param start          is this a starting element (of a two-piece entity)
     * @param referencePoint the reference location within the system
     * @param chord          the related chord, if any
     * @param glyph          the underlying glyph
     */
    public MeasureElement (Measure measure,
                           boolean start,
                           Point referencePoint,
                           Chord chord,
                           Glyph glyph)
    {
        super(measure);

        if (glyph != null) {
            addGlyph(glyph);
        }

        this.start = start;
        setReferencePoint(referencePoint);
        this.chord = chord;
    }

    //~ Methods ----------------------------------------------------------------
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

    //---------------------//
    // getTranslationLinks //
    //---------------------//
    @Override
    public List<Line2D> getTranslationLinks (Glyph glyph)
    {
        if (chord != null) {
            return chord.getTranslationLinks(glyph);
        } else {
            return Collections.emptyList();
        }
    }

    //---------//
    // isStart //
    //---------//
    public boolean isStart ()
    {
        return start;
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
        int period = name.lastIndexOf('.');
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

            // Point
            sb.append(" ref[x=")
                    .append(getReferencePoint().x)
                    .append(",y=")
                    .append(getReferencePoint().y)
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
            sb.append(" ")
                    .append(Glyphs.toString(glyphs));

            // Chord
            sb.append(" ")
                    .append(chord.getContextString())
                    .append(" ")
                    .append(chord);
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
    protected static Chord findChord (Measure measure,
                                      Point point)
    {
        // Shift on abscissa (because of left side of note heads)
        int dx = measure.getSystem()
                .getScale()
                .toPixels(constants.slotShift);

        return measure.getEventChord(new Point(point.x + dx, point.y));
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

    //-------//
    // reset //
    //-------//
    @Override
    protected void reset ()
    {
        super.reset();

        shape = null;
        setReferencePoint(null);
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
                "Abscissa shift when looking for time slot "
                + "(half a note head)");

    }
}
