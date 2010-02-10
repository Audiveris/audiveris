//----------------------------------------------------------------------------//
//                                                                            //
//                      B a s i c R e c o g n i t i o n                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.glyph.Evaluation;
import omr.glyph.Shape;
import omr.glyph.ShapeRange;
import omr.glyph.text.TextInfo;

import omr.math.Rational;

import java.util.*;

/**
 * Class {@code BasicRecognition} is the basic implementation of a recognition
 * facet
 *
 * @author Hervé Bitteur
 */
class BasicRecognition
    extends BasicFacet
    implements GlyphRecognition
{
    //~ Instance fields --------------------------------------------------------

    /** Current evaluation (shape + doubt), if any */
    private Evaluation evaluation;

    /** Set of forbidden shapes, if any */
    private Set<Shape> forbiddenShapes;

    /** Related textual information, if any */
    private TextInfo textInfo;

    /** Related time sig rational information, if any */
    private Rational rational;

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // BasicRecognition //
    //------------------//
    /**
     * Creates a new BasicRecognition object.
     *
     * @param glyph our glyph
     */
    public BasicRecognition (Glyph glyph)
    {
        super(glyph);
    }

    //~ Methods ----------------------------------------------------------------

    //-------//
    // isBar //
    //-------//
    public boolean isBar ()
    {
        return ShapeRange.Barlines.contains(getShape());
    }

    //--------//
    // isClef //
    //--------//
    public boolean isClef ()
    {
        return ShapeRange.Clefs.contains(getShape());
    }

    //----------//
    // getDoubt //
    //----------//
    public double getDoubt ()
    {
        if (evaluation != null) {
            return evaluation.doubt;
        } else {
            // No real interest
            return Evaluation.ALGORITHM;
        }
    }

    //---------------//
    // setEvaluation //
    //---------------//
    public void setEvaluation (Evaluation evaluation)
    {
        this.evaluation = evaluation;
    }

    //---------------//
    // getEvaluation //
    //---------------//
    public Evaluation getEvaluation ()
    {
        return evaluation;
    }

    //---------//
    // isKnown //
    //---------//
    public boolean isKnown ()
    {
        Shape shape = getShape();

        return (shape != null) && (shape != Shape.NOISE);
    }

    //---------------//
    // isManualShape //
    //---------------//
    public boolean isManualShape ()
    {
        return getDoubt() == Evaluation.MANUAL;
    }

    //-------------//
    // setRational //
    //-------------//
    public void setRational (Rational rational)
    {
        this.rational = rational;
    }

    //-------------//
    // getRational //
    //-------------//
    public Rational getRational ()
    {
        return rational;
    }

    //----------//
    // setShape //
    //----------//
    public void setShape (Shape shape)
    {
        setShape(shape, Evaluation.ALGORITHM);
    }

    //----------//
    // setShape //
    //----------//
    public void setShape (Shape  shape,
                          double doubt)
    {
        // Blacklist the old shape if any
        Shape oldShape = getShape();

        if ((oldShape != null) && (shape != Shape.GLYPH_PART)) {
            forbidShape(oldShape);
        }

        if (shape == null) {
            // Set the part shape to null as well (rather than GLYPH_PART)
            for (Glyph part : glyph.getParts()) {
                part.setShape(null, doubt);
            }
        } else {
            // Remove the new shape from the blacklist if any
            allowShape(shape);
        }

        // Remember the new shape
        evaluation = new Evaluation(shape, doubt);
    }

    //----------//
    // getShape //
    //----------//
    public Shape getShape ()
    {
        if (evaluation != null) {
            return evaluation.shape;
        } else {
            return null;
        }
    }

    //------------------//
    // isShapeForbidden //
    //------------------//
    public boolean isShapeForbidden (Shape shape)
    {
        return (forbiddenShapes != null) && forbiddenShapes.contains(shape);
    }

    //--------//
    // isStem //
    //--------//
    public boolean isStem ()
    {
        return getShape() == Shape.COMBINING_STEM;
    }

    //--------//
    // isText //
    //--------//
    public boolean isText ()
    {
        Shape shape = getShape();

        return (shape != null) && shape.isText();
    }

    //-------------//
    // getTextInfo //
    //-------------//
    public TextInfo getTextInfo ()
    {
        if (textInfo == null) {
            textInfo = new TextInfo(glyph);
        }

        return textInfo;
    }

    //-------------//
    // isWellKnown //
    //-------------//
    public boolean isWellKnown ()
    {
        Shape shape = getShape();

        return (shape != null) && shape.isWellKnown();
    }

    //------------//
    // allowShape //
    //------------//
    /**
     * Remove the provided shape from the collection of forbidden shaped, if any
     * @param shape the shape to allow
     */
    public void allowShape (Shape shape)
    {
        if (forbiddenShapes != null) {
            forbiddenShapes.remove(shape);
        }
    }

    //------//
    // dump //
    //------//
    @Override
    public void dump ()
    {
        System.out.println("   evaluation=" + evaluation);
        System.out.println(
            "   training=" +
            ((getShape() != null) ? getShape().getTrainingShape() : null));
        System.out.println("   forbiddenShapes=" + forbiddenShapes);
        System.out.println("   textInfo=" + textInfo);
        System.out.println("   rational=" + rational);
    }

    //-------------//
    // forbidShape //
    //-------------//
    public void forbidShape (Shape shape)
    {
        if (forbiddenShapes == null) {
            forbiddenShapes = new HashSet<Shape>();
        }

        forbiddenShapes.add(shape);
    }
}
