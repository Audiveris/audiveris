//----------------------------------------------------------------------------//
//                                                                            //
//                                 W e d g e                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.visitor.ScoreVisitor;

/**
 * Class {@code Wedge} represents a crescendo (&lt;) or a
 * decrescendo (&gt;).
 *
 * @author Hervé Bitteur
 */
public class Wedge
        extends AbstractDirection
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Wedge.class);

    //~ Instance fields --------------------------------------------------------
    /** Vertical spread in units */
    private final int spread;

    //~ Constructors -----------------------------------------------------------
    //-------//
    // Wedge //
    //-------//
    /**
     * Creates a new instance of Wedge edge.
     * (there must be one for the wedge start, and one for the wedge stop).
     *
     * @param measure measure that contains this wedge edge
     * @param start   indicate a wedge start
     * @param point   middle point on wedge edge
     * @param chord   a related chord if any
     * @param glyph   the underlying glyph
     */
    public Wedge (Measure measure,
                  boolean start,
                  PixelPoint point,
                  Chord chord,
                  Glyph glyph)
    {
        super(measure, start, point, chord, glyph);

        // Spread
        if ((start && (getShape() == Shape.DECRESCENDO))
            || (!start && (getShape() == Shape.CRESCENDO))) {
            spread = glyph.getBounds().height;
        } else {
            spread = 0;
        }
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // populate //
    //----------//
    /**
     * Used by SystemTranslator to allocate the wedges
     *
     * @param glyph           underlying glyph
     * @param startingMeasure measure where left side is located
     * @param startingPoint   location for left point
     */
    public static void populate (Glyph glyph,
                                 Measure startingMeasure,
                                 PixelPoint startingPoint)
    {
        if (glyph.isVip()) {
            logger.info("Wedge. populate {0}", glyph.idString());
        }
        
        SystemPart part = startingMeasure.getPart();
        PixelRectangle box = glyph.getBounds();

        // Start
        glyph.setTranslation(
                new Wedge(
                startingMeasure,
                true,
                startingPoint,
                findChord(startingMeasure, startingPoint),
                glyph));

        // Stop
        PixelPoint endingPoint = new PixelPoint(
                box.x + box.width,
                box.y + (box.height / 2));
        Measure endingMeasure = part.getMeasureAt(endingPoint);
        glyph.addTranslation(
                new Wedge(
                endingMeasure,
                false,
                endingPoint,
                findChord(endingMeasure, endingPoint),
                glyph));
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
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
}
