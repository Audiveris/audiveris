//----------------------------------------------------------------------------//
//                                                                            //
//                              D y n a m i c s                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.entity;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.log.Logger;

import omr.score.common.SystemPoint;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;

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
    extends MeasureElement
    implements Direction, Notation
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Dynamics.class);

    /** Specific application parameters */
    protected static final Constants constants = new Constants();

    /** Map Shape -> Signature */
    private static final Map<Shape, String> sigs = new HashMap<Shape, String>();

    static {
        // Additional characters : m, r, s & z
        sigs.put(Shape.DYNAMICS_CHAR_M, "m");
        sigs.put(Shape.DYNAMICS_CHAR_R, "r");
        sigs.put(Shape.DYNAMICS_CHAR_S, "s");
        sigs.put(Shape.DYNAMICS_CHAR_Z, "z");
        //
        // True dynamics symbols
        sigs.put(Shape.DYNAMICS_F, "f");
        sigs.put(Shape.DYNAMICS_FF, "ff");
        sigs.put(Shape.DYNAMICS_FFF, "fff");
        sigs.put(Shape.DYNAMICS_FFFF, "ffff");
        sigs.put(Shape.DYNAMICS_FFFFF, "fffff");
        sigs.put(Shape.DYNAMICS_FFFFFF, "ffffff");
        sigs.put(Shape.DYNAMICS_FP, "fp");
        sigs.put(Shape.DYNAMICS_FZ, "fz");
        sigs.put(Shape.DYNAMICS_MF, "mf");
        sigs.put(Shape.DYNAMICS_MP, "mp");
        sigs.put(Shape.DYNAMICS_P, "p");
        sigs.put(Shape.DYNAMICS_PP, "pp");
        sigs.put(Shape.DYNAMICS_PPP, "ppp");
        sigs.put(Shape.DYNAMICS_PPPP, "pppp");
        sigs.put(Shape.DYNAMICS_PPPPP, "ppppp");
        sigs.put(Shape.DYNAMICS_PPPPPP, "pppppp");
        sigs.put(Shape.DYNAMICS_RF, "rf");
        sigs.put(Shape.DYNAMICS_RFZ, "rfz");
        sigs.put(Shape.DYNAMICS_SF, "sf");
        sigs.put(Shape.DYNAMICS_SFFZ, "sffz");
        sigs.put(Shape.DYNAMICS_SFP, "sfp");
        sigs.put(Shape.DYNAMICS_SFPP, "sfpp");
        sigs.put(Shape.DYNAMICS_SFZ, "sfz");
    }

    /* Map Signature -> Shape */
    private static final Map<String, Shape> shapes = new HashMap<String, Shape>();

    static {
        shapes.put("f", Shape.DYNAMICS_F);
        shapes.put("ff", Shape.DYNAMICS_FF);
        shapes.put("fff", Shape.DYNAMICS_FFF);
        shapes.put("ffff", Shape.DYNAMICS_FFFF);
        shapes.put("fffff", Shape.DYNAMICS_FFFFF);
        shapes.put("ffffff", Shape.DYNAMICS_FFFFFF);
        shapes.put("fp", Shape.DYNAMICS_FP);
        shapes.put("fz", Shape.DYNAMICS_FZ);
        shapes.put("mf", Shape.DYNAMICS_MF);
        shapes.put("mp", Shape.DYNAMICS_MP);
        shapes.put("p", Shape.DYNAMICS_P);
        shapes.put("pp", Shape.DYNAMICS_PP);
        shapes.put("ppp", Shape.DYNAMICS_PPP);
        shapes.put("pppp", Shape.DYNAMICS_PPPP);
        shapes.put("ppppp", Shape.DYNAMICS_PPPPP);
        shapes.put("pppppp", Shape.DYNAMICS_PPPPPP);
        shapes.put("rf", Shape.DYNAMICS_RF);
        shapes.put("rfz", Shape.DYNAMICS_RFZ);
        shapes.put("sf", Shape.DYNAMICS_SF);
        shapes.put("sffz", Shape.DYNAMICS_SFFZ);
        shapes.put("sfp", Shape.DYNAMICS_SFP);
        shapes.put("sfpp", Shape.DYNAMICS_SFPP);
        shapes.put("sfz", Shape.DYNAMICS_SFZ);
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
        super(measure, true, point, chord, glyph);

        if (chord != null) {
            chord.addDirection(this); ////// TBD: Not always !!!!!!!!!!!!!!!!!!!
        }
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
    // populate //
    //----------//
    /**
     * Used by ScoreBuilder to allocate the dynamics marks
     *
     * @param glyph underlying glyph
     * @param measure measure where the mark is located
     * @param point location for the mark
     */
    public static void populate (Glyph       glyph,
                                 Measure     measure,
                                 SystemPoint point)
    {
        // Can we gather with another dynamics letter? (e.g. m + p -> mp)
        for (TreeNode node : measure.getChildren()) {
            if (node instanceof Dynamics) {
                Dynamics d = (Dynamics) node;

                if (d.isCompatibleWith(point)) {
                    d.addGlyph(glyph);
                    glyph.setTranslation(d);

                    return;
                }
            }
        }

        // Otherwise, create a brand new instance
        glyph.setTranslation(
            new Dynamics(measure, point, findChord(measure, point), glyph));
    }

    //---------------//
    // computeCenter //
    //---------------//
    @Override
    protected void computeCenter ()
    {
        setCenter(computeGlyphsCenter(getGlyphs()));
    }

    //--------------//
    // computeShape //
    //--------------//
    @Override
    protected Shape computeShape ()
    {
        StringBuilder sig = new StringBuilder();

        for (Glyph glyph : getGlyphs()) {
            sig.append(sigs.get(glyph.getShape()));
        }

        Shape shape = shapes.get(sig.toString());

        if (shape == null) {
            addError("Invalid dynamics signature:" + sig);
        }

        return shape;
    }

    //------------------//
    // isCompatibleWith //
    //------------------//
    private boolean isCompatibleWith (SystemPoint point)
    {
        // Check x-proximity and y-alignment
        Scale scale = getSystem()
                          .getScale();
        int   dx = scale.toUnits(constants.maxDx);
        int   dy = scale.toUnits(constants.maxDy);

        // Horizontal distance
        int xDist = Math.min(
            Math.abs(getBox().x - point.x),
            Math.abs((getBox().x + getBox().width) - point.x));

        // Vertical distance
        int yDist = Math.abs(getPoint().y - point.y);

        return (xDist <= dx) && (yDist <= dy);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Maximum abscissa difference */
        Scale.Fraction maxDx = new Scale.Fraction(
            1,
            "Maximum abscissa difference");

        /** Maximum ordinate difference */
        Scale.Fraction maxDy = new Scale.Fraction(
            0.5,
            "Maximum ordinate difference");
    }
}
