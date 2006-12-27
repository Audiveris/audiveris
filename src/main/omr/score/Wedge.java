//----------------------------------------------------------------------------//
//                                                                            //
//                                 W e d g e                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;

import omr.score.visitor.ScoreVisitor;

import omr.sheet.PixelPoint;
import omr.sheet.PixelRectangle;
import omr.sheet.Scale;

import omr.util.Logger;

/**
 * Class <code>Wedge</code> represents a crescendo or decrescendo (diminuendo)
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Wedge
    extends PartNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Wedge.class);

    //~ Instance fields --------------------------------------------------------

    /** Underlying glyph */
    private final Glyph glyph;

    /** Starting point */
    private final SystemPoint startingPoint;

    /** Starting chord (in starting measure) */
    private final Chord startingChord;

    /** Ending point */
    private final SystemPoint endingPoint;

    /** Ending chord (in ending measure). If null, end is the measure end */
    private final Chord endingChord;

    /** Vertical spread in units */
    private final int spread;

    //~ Constructors -----------------------------------------------------------

    //-------//
    // Wedge //
    //-------//
    /**
     * Creates a new instance of Wedge
     *
     * @param part the containing system part
     * @param startingMeasure measure that contains the left side of the wedge
     * @param startingPoint middle point on left side
     * @param glyph the underlying glyph
     */
    public Wedge (SystemPart  part,
                  Measure     startingMeasure,
                  SystemPoint startingPoint,
                  Glyph       glyph)
    {
        super(part);

        this.glyph = glyph;

        System         system = part.getSystem();
        PixelRectangle box = glyph.getContourBox();

        // Shift on abscissa (because of left side of note heads)
        int dx = getSystem()
                     .getScale()
                     .toUnits(constants.slotShift);

        // Determine starting
        this.startingPoint = startingPoint;
        startingChord = startingMeasure.findEventChord(
            new SystemPoint(startingPoint.x + dx, startingPoint.y));
        startingChord.addEvent(this);

        // Determine ending
        endingPoint = system.toSystemPoint(
            new PixelPoint(box.x + box.width, box.y + (box.height / 2)));

        Measure endingMeasure = part.getMeasureAt(endingPoint);
        endingChord = endingMeasure.findEventChord(
            new SystemPoint(endingPoint.x + dx, endingPoint.y));
        endingChord.addEvent(this);

        // Spread
        spread = getSystem()
                     .getScale()
                     .pixelsToUnits(glyph.getContourBox().height);
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // getEndingPoint //
    //----------------//
    /**
     * Report the right point of the wedge
     *
     * @return right wedge point
     */
    public SystemPoint getEndingPoint ()
    {
        return endingPoint;
    }

    //----------//
    // getGlyph //
    //----------//
    /**
     * report the underlying glyph
     *
     * @return the underlying glyph (shape is crescendo or decrescendo)
     */
    public Glyph getGlyph ()
    {
        return glyph;
    }

    //-----------//
    // getSpread //
    //-----------//
    /**
     * Report the vertical spread of the wedge
     *
     * @return vertical spread in units
     */
    public int getSpread ()
    {
        return spread;
    }

    //------------------//
    // getStartingPoint //
    //------------------//
    /**
     * Report the left side of the wedge
     *
     * @return left side point
     */
    public SystemPoint getStartingPoint ()
    {
        return startingPoint;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Wedge ")
          .append(glyph.getShape());
        sb.append(" from ")
          .append(startingChord.getContextString());
        sb.append(" to ")
          .append(endingChord.getContextString());

        sb.append("}");

        return sb.toString();
    }

    //----------//
    // populate //
    //----------//
    /**
     * Used by ScoreBuilder to allocate the wedges
     *
     * @param glyph underlying glyph
     * @param startingMeasure measure where left side is located
     * @param startingPoint location for left point
     */
    static void populate (Glyph       glyph,
                          Measure     startingMeasure,
                          SystemPoint startingPoint)
    {
        new Wedge(
            startingMeasure.getPart(),
            startingMeasure,
            startingPoint,
            glyph);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /**
         * Abscissa shift when looking for time slot (half a note head)
         */
        Scale.Fraction slotShift = new Scale.Fraction(
            0.5,
            "Abscissa shift (in interline fraction) when looking for time slot " +
            "(half a note head)");
    }
}
