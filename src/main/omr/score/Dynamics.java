//----------------------------------------------------------------------------//
//                                                                            //
//                              D y n a m i c s                               //
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
import omr.glyph.Shape;

import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;

import omr.util.Logger;
import omr.util.TreeNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Class <code>Dynamics</code> represents a dynamics event
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Dynamics
    extends Direction
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Dynamics.class);

    /** Specific application parameters */
    protected static final Constants constants = new Constants();

    /** Map Shape -> Signature */
    private static final Map<Shape, String> sigs = new HashMap<Shape, String>();

    static {
        sigs.put(Shape.PIANISSISSIMO, "ppp");
        sigs.put(Shape.PIANISSIMO, "pp");
        sigs.put(Shape.PIANO, "p");
        sigs.put(Shape.MEZZO_PIANO, "mp");
        sigs.put(Shape.MEZZO, "m");
        sigs.put(Shape.MEZZO_FORTE, "mf");
        sigs.put(Shape.FORTE, "f");
        sigs.put(Shape.FORTISSIMO, "ff");
        sigs.put(Shape.FORTISSISSIMO, "fff");
    }

    /* Map Signature -> Shape */
    private static final Map<String, Shape> shapes = new HashMap<String, Shape>();

    static {
        shapes.put("ppp", Shape.PIANISSISSIMO);
        shapes.put("pp", Shape.PIANISSIMO);
        shapes.put("p", Shape.PIANO);
        shapes.put("mp", Shape.MEZZO_PIANO);
        shapes.put("m", Shape.MEZZO);
        shapes.put("mf", Shape.MEZZO_FORTE);
        shapes.put("f", Shape.FORTE);
        shapes.put("ff", Shape.FORTISSIMO);
        shapes.put("fff", Shape.FORTISSISSIMO);
    }

    //~ Constructors -----------------------------------------------------------

    //----------//
    // Dynamics //
    //----------//
    /**
     * Creates a new instance of Dynamics event
     *
     * @param measure measure that contains this mark
     * @param point location of mark
     * @param chord the chord related to the mark
     * @param glyph the underlying glyph
     */
    public Dynamics (Measure     measure,
                     SystemPoint point,
                     Chord       chord,
                     Glyph       glyph)
    {
        super(true, measure, point, chord);
        addGlyph(glyph);
    }

    //~ Methods ----------------------------------------------------------------

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
        sb.append("{Dynamics ");
        sb.append(super.toString());
        sb.append("}");

        return sb.toString();
    }

    //--------------//
    // computeShape //
    //--------------//
    @Override
    protected Shape computeShape ()
    {
        String sig = "";

        for (Glyph glyph : getGlyphs()) {
            sig += sigs.get(glyph.getShape());
        }

        Shape shape = shapes.get(sig);

        if (shape == null) {
            logger.warning("Invalid dynamics signature:" + sig);
        }

        return shape;
    }

    //----------//
    // populate //
    //----------//
    /**
     * Used by ScoreBuilder to allocate the dynamics marks
     *
     * @param glyph underlying glyph
     * @param measure measure where the mark is located
     * @param point location for the mark
     */
    static void populate (Glyph       glyph,
                          Measure     measure,
                          SystemPoint point)
    {
        // Can we gather with another dynamics letter? (e.g. m + p -> mp)
        for (TreeNode node : measure.getChildren()) {
            if (node instanceof Dynamics) {
                Dynamics d = (Dynamics) node;

                if (d.isCompatibleWith(point)) {
                    d.addGlyph(glyph);

                    return;
                }
            }
        }

        // Create a brand new instance
        new Dynamics(measure, point, findChord(measure, point), glyph);
    }

    //------------------//
    // isCompatibleWith //
    //------------------//
    private boolean isCompatibleWith (SystemPoint point)
    {
        // Check x-proximity and y-alignment (TBI)
        Scale scale = getSystem()
                          .getScale();
        int   dx = scale.toUnits(constants.maxDx);
        int   dy = scale.toUnits(constants.maxDy);

        return (Math.abs(getPoint().x - point.x) <= dx) &&
               (Math.abs(getPoint().y - point.y) <= dy);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /** Maximum abscissa difference */
        Scale.Fraction maxDx = new Scale.Fraction(
            1,
            "Maximum abscissa difference (in interline fraction)");

        /** Maximum ordinate difference */
        Scale.Fraction maxDy = new Scale.Fraction(
            0.5,
            "Maximum ordinate difference (in interline fraction)");
    }
}
