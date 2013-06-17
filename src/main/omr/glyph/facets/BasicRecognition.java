//----------------------------------------------------------------------------//
//                                                                            //
//                      B a s i c R e c o g n i t i o n                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.glyph.Evaluation;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;

import omr.score.entity.TimeRational;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Class {@code BasicRecognition} is the basic implementation of a
 * recognition facet.
 *
 * @author Hervé Bitteur
 */
class BasicRecognition
        extends BasicFacet
        implements GlyphRecognition
{

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(BasicRecognition.class);

    /** Current evaluation (shape + grade), if any */
    private Evaluation evaluation;

    /** Set of forbidden shapes, if any */
    private Set<Shape> forbiddenShapes;

    /** Related time sig rational information, if any */
    private TimeRational timeRational;

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

    //------------//
    // allowShape //
    //------------//
    @Override
    public void allowShape (Shape shape)
    {
        if (forbiddenShapes != null) {
            forbiddenShapes.remove(shape);
        }
    }

    //--------//
    // dumpOf //
    //--------//
    @Override
    public String dumpOf ()
    {
        StringBuilder sb = new StringBuilder();

        if (evaluation != null) {
            sb.append(String.format("   evaluation=%s%n", evaluation));
        }

        Shape physical = (getShape() != null) ? getShape().getPhysicalShape() : null;
        if (physical != null) {
            sb.append(String.format("   physical=%s%n", physical));
        }

        if (forbiddenShapes != null) {
            sb.append(String.format("   forbiddenShapes=%s%n", forbiddenShapes));
        }

        if (timeRational != null) {
            sb.append(String.format("   rational=%s%n", timeRational));
        }

        return sb.toString();
    }

    //-------------//
    // forbidShape //
    //-------------//
    @Override
    public void forbidShape (Shape shape)
    {
        if (forbiddenShapes == null) {
            forbiddenShapes = new HashSet<>();
        }

        forbiddenShapes.add(shape);
    }

    //---------------//
    // getEvaluation //
    //---------------//
    @Override
    public Evaluation getEvaluation ()
    {
        return evaluation;
    }

    //----------//
    // getGrade //
    //----------//
    @Override
    public double getGrade ()
    {
        if (evaluation != null) {
            return evaluation.grade;
        } else {
            // No real interest
            return Evaluation.ALGORITHM;
        }
    }

    //----------//
    // getShape //
    //----------//
    @Override
    public Shape getShape ()
    {
        if (evaluation != null) {
            return evaluation.shape;
        } else {
            return null;
        }
    }

    //-----------------//
    // getTimeRational //
    //-----------------//
    @Override
    public TimeRational getTimeRational ()
    {
        return timeRational;
    }

    //-------//
    // isBar //
    //-------//
    @Override
    public boolean isBar ()
    {
        return ShapeSet.Barlines.contains(getShape());
    }

    //--------//
    // isClef //
    //--------//
    @Override
    public boolean isClef ()
    {
        return ShapeSet.Clefs.contains(getShape());
    }

    //---------//
    // isKnown //
    //---------//
    @Override
    public boolean isKnown ()
    {
        Shape shape = getShape();

        return (shape != null) && (shape != Shape.NOISE);
    }

    //---------------//
    // isManualShape //
    //---------------//
    @Override
    public boolean isManualShape ()
    {
        return getGrade() == Evaluation.MANUAL;
    }

    //------------------//
    // isShapeForbidden //
    //------------------//
    @Override
    public boolean isShapeForbidden (Shape shape)
    {
        return (forbiddenShapes != null) && forbiddenShapes.contains(shape);
    }

    //--------//
    // isStem //
    //--------//
    @Override
    public boolean isStem ()
    {
        return getShape() == Shape.STEM;
    }

    //--------//
    // isText //
    //--------//
    @Override
    public boolean isText ()
    {
        Shape shape = getShape();

        return (shape != null) && shape.isText();
    }

    //-------------//
    // isWellKnown //
    //-------------//
    @Override
    public boolean isWellKnown ()
    {
        Shape shape = getShape();

        return (shape != null) && shape.isWellKnown();
    }

    //-----------------//
    // resetEvaluation //
    //-----------------//
    @Override
    public void resetEvaluation ()
    {
        evaluation = null;
    }

    //---------------//
    // setEvaluation //
    //---------------//
    @Override
    public void setEvaluation (Evaluation evaluation)
    {
        setShape(evaluation.shape, evaluation.grade);
    }

    //----------//
    // setShape //
    //----------//
    @Override
    public void setShape (Shape shape)
    {
        setShape(shape, Evaluation.ALGORITHM);
    }

    //----------//
    // setShape //
    //----------//
    @Override
    public void setShape (Shape shape,
                          double grade)
    {
//        // Check status
//        if (glyph.isTransient()) {
//            logger.error("Setting shape of a transient glyph");
//        }

        // Blacklist the old shape if any
        Shape oldShape = getShape();

        if ((oldShape != null) && (oldShape != shape)
            && (oldShape != Shape.GLYPH_PART)) {
            forbidShape(oldShape);

            if (glyph.isVip()) {
                logger.info("Shape {} forbidden for {}",
                        oldShape, glyph.idString());
            }
        }

        if (shape != null) {
            // Remove the new shape from the blacklist if any
            allowShape(shape);
        }

        // Remember the new shape
        evaluation = new Evaluation(shape, grade);

        if (glyph.isVip()) {
            logger.info("{} assigned {}", glyph.idString(), evaluation);
        }
    }

    //-----------------//
    // setTimeRational //
    //-----------------//
    @Override
    public void setTimeRational (TimeRational timeRational)
    {
        this.timeRational = timeRational;
    }
}
