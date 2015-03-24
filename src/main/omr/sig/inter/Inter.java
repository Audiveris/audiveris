//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            I n t e r                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.ui.AttachmentHolder;

import omr.sheet.Staff;
import omr.sheet.rhythm.Voice;

import omr.sig.GradeImpacts;
import omr.sig.SIGraph;

import omr.util.Vip;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.Comparator;

/**
 * Interface {@code Inter} defines a possible interpretation.
 * <p>
 * Every Inter instance is assigned an <i>intrinsic</i> grade in range [0..1].
 * There usually exists two thresholds on grade value:<ol>
 * <li><b>Minimum</b> grade: this is the minimum value required to actually create (or keep) any
 * interpretation instance.</li>
 * <li><b>Good</b> grade: this is the value which designates a really reliable interpretation, which
 * could save the need to explore other possible interpretations.</li>
 * </ol>
 * <p>
 * Besides the intrinsic grade, we can generally compute a <i>contextual</i> grade, which augments
 * the intrinsic value by some increment brought by supporting interpretation(s) nearby, depending
 * on the intrinsic grade of the supporting interpretation as well as the quality of the geometric
 * relationship itself.
 * The <i>contextual</i> grade provides a more reliable quality evaluation than the mere
 * <i>intrinsic</i> grade, because it takes the surrounding entities into account.
 * <p>
 * To leave room for potential improvement brought by contextual data, the raw value computed for
 * any intrinsic grade is multiplied by an <i>intrinsicRatio</i>.
 *
 * @author Hervé Bitteur
 */
public interface Inter
        extends VisitableInter, Vip, AttachmentHolder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /**
     * For comparing interpretations by id.
     */
    public static final Comparator<Inter> byId = new Comparator<Inter>()
    {
        @Override
        public int compare (Inter i1,
                            Inter i2)
        {
            return Integer.compare(i1.getId(), i2.getId());
        }
    };

    /**
     * For comparing interpretations by left abscissa.
     */
    public static final Comparator<Inter> byAbscissa = new Comparator<Inter>()
    {
        @Override
        public int compare (Inter i1,
                            Inter i2)
        {
            return Integer.compare(i1.getBounds().x, i2.getBounds().x);
        }
    };

    /**
     * For comparing interpretations by center abscissa.
     */
    public static final Comparator<Inter> byCenterAbscissa = new Comparator<Inter>()
    {
        @Override
        public int compare (Inter i1,
                            Inter i2)
        {
            return Integer.compare(i1.getCenter().x, i2.getCenter().x);
        }
    };

    /**
     * For comparing interpretations by right abscissa.
     */
    public static final Comparator<Inter> byRightAbscissa = new Comparator<Inter>()
    {
        @Override
        public int compare (Inter i1,
                            Inter i2)
        {
            Rectangle b1 = i1.getBounds();
            Rectangle b2 = i2.getBounds();

            return Integer.compare(b1.x + b1.width, b2.x + b2.width);
        }
    };

    /**
     * For comparing interpretations by abscissa, ensuring that only identical
     * interpretations are found equal.
     * This comparator can thus be used for a TreeSet.
     */
    public static final Comparator<Inter> byFullAbscissa = new Comparator<Inter>()
    {
        @Override
        public int compare (Inter o1,
                            Inter o2)
        {
            if (o1 == o2) {
                return 0;
            }

            Point loc1 = o1.getBounds().getLocation();
            Point loc2 = o2.getBounds().getLocation();

            // Are x values different?
            int dx = loc1.x - loc2.x;

            if (dx != 0) {
                return dx;
            }

            // Vertically aligned, so use ordinates
            int dy = loc1.y - loc2.y;

            if (dy != 0) {
                return dy;
            }

            // Finally, use id ...
            return o1.getId() - o2.getId();
        }
    };

    /**
     * For comparing interpretations by ordinate.
     */
    public static final Comparator<Inter> byOrdinate = new Comparator<Inter>()
    {
        @Override
        public int compare (Inter i1,
                            Inter i2)
        {
            return Integer.compare(i1.getBounds().y, i2.getBounds().y);
        }
    };

    /**
     * For comparing interpretations by increasing grade.
     */
    public static final Comparator<Inter> byGrade = new Comparator<Inter>()
    {
        @Override
        public int compare (Inter i1,
                            Inter i2)
        {
            return Double.compare(i1.getGrade(), i2.getGrade());
        }
    };

    /**
     * For comparing interpretations by decreasing grade.
     */
    public static final Comparator<Inter> byReverseGrade = new Comparator<Inter>()
    {
        @Override
        public int compare (Inter i1,
                            Inter i2)
        {
            return Double.compare(i2.getGrade(), i1.getGrade());
        }
    };

    /**
     * For comparing interpretations by decreasing best grade.
     */
    public static final Comparator<Inter> byReverseBestGrade = new Comparator<Inter>()
    {
        @Override
        public int compare (Inter i1,
                            Inter i2)
        {
            return Double.compare(i2.getBestGrade(), i1.getBestGrade()); // Reverse order
        }
    };

    static final Constants constants = new Constants();

    /** Ratio applied on intrinsic value, to leave room for contextual. */
    static double intrinsicRatio = constants.intrinsicRatio.getValue();

    /** The minimum grade to consider an interpretation as acceptable. */
    static double minGrade = intrinsicRatio * constants.minGrade.getValue();

    /** The minimum contextual grade for an interpretation. */
    static double minContextualGrade = constants.minContextualGrade.getValue();

    /** The minimum grade to consider an interpretation as good. */
    static double goodGrade = intrinsicRatio * constants.goodGrade.getValue();

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Decrease the inter grade.
     *
     * @param ratio ratio applied
     */
    void decrease (double ratio);

    /**
     * Delete this instance, and remove it from its containing SIG.
     */
    void delete ();

    /**
     * Report a complete dump for this interpretation.
     *
     * @return a complete string dump
     */
    String dumpOf ();

    /**
     * Mark this inter as frozen, that cannot be deleted even by a conflicting
     * instance with higher grade.
     */
    void freeze ();

    /**
     * Report the precise defining area
     *
     * @return the inter area, if any
     */
    Area getArea ();

    /**
     * Report the best grade (either contextual or intrinsic) assigned to interpretation
     *
     * @return the contextual grade if available, otherwise the intrinsic grade
     */
    double getBestGrade ();

    /**
     * Report a COPY of the bounding box for this interpretation.
     *
     * @return the bounding box
     */
    Rectangle getBounds ();

    /**
     * Report the inter center.
     *
     * @return center
     */
    Point getCenter ();

    /**
     * Report the inter center left.
     *
     * @return left point at mid height
     */
    Point getCenterLeft ();

    /**
     * Report the inter center right.
     *
     * @return right point at mid height
     */
    Point getCenterRight ();

    /**
     * Report the contextual grade, (0..1 probability) computed for interpretation.
     *
     * @return the contextual grade, if any, or null
     */
    Double getContextualGrade ();

    /**
     * Report the core box for this interpretation.
     *
     * @return a small core box
     */
    Rectangle2D getCoreBounds ();

    /**
     * Details for tip.
     *
     * @return informations for a tip
     */
    String getDetails ();

    /**
     * Report the ensemble this inter is member of, if any.
     *
     * @return the containing ensemble or null
     */
    InterEnsemble getEnsemble ();

    /**
     * Report the glyph, if any, which is concerned by this interpretation.
     *
     * @return the underlying glyph, or null
     */
    Glyph getGlyph ();

    /**
     * Report the intrinsic grade (0..1 probability) assigned to interpretation
     *
     * @return the intrinsic grade
     */
    double getGrade ();

    /**
     * Report the interpretation id (for debugging)
     *
     * @return the id or 0 if not yet identified
     */
    int getId ();

    /**
     * Report details about the final grade
     *
     * @return the grade details
     */
    GradeImpacts getImpacts ();

    /**
     * Report the inter, if any, this instance is a duplicate of.
     *
     * @return the mirror instance or null
     */
    Inter getMirror ();

    /**
     * Report the shape related to interpretation.
     *
     * @return the shape
     */
    Shape getShape ();

    /**
     * Report the sig which hosts this interpretation.
     *
     * @return the containing sig
     */
    SIGraph getSig ();

    /**
     * Report the related staff, if any.
     *
     * @return the related staff or null
     */
    Staff getStaff ();

    /**
     * Report a COPY of the bounding box based on MusicFont symbol.
     *
     * @param interline scaling factor
     * @return the symbol bounding box
     */
    Rectangle getSymbolBounds (int interline);

    /**
     * Report the voice, if any, this inter belongs to
     *
     * @return the inter voice or null
     */
    Voice getVoice ();

    /**
     * Increase the inter grade.
     *
     * @param ratio ratio applied
     */
    void increase (double ratio);

    /**
     * Report whether the interpretation has a good contextual grade.
     *
     * @return true if contextual grade is good
     */
    boolean isContextuallyGood ();

    /**
     * Report whether this instance has been deleted.
     *
     * @return true if deleted
     */
    boolean isDeleted ();

    /**
     * Report whether this instance has been frozen.
     *
     * @return true if frozen
     */
    boolean isFrozen ();

    /**
     * Report whether the interpretation has a good (intrinsic) grade.
     *
     * @return true if grade is good
     */
    boolean isGood ();

    /**
     * Report whether this interpretation represents the same thing as that interpretation
     *
     * @param that the other inter to check
     * @return true if identical, false otherwise
     */
    boolean isSameAs (Inter that);

    /**
     * Check whether this inter instance overlaps that inter instance.
     * <p>
     * <b>NOTA</b>, this method is not always commutative, meaning <code>one.overlaps(two)</code>
     * and <code>two.overlaps(one)</code> are not assumed to always give the same result.
     * For reliable results, test both:
     * <code>if (one.overlaps(two) && two.overlaps(one)) {...}</code>
     * <p>
     * <b>NOTA</b>, precise meaning is: <i>"Does this inter step on the toes of that other one in
     * some incompatible way?"</i>.
     * Keeping this in mind, an InterEnsemble does not "overlap" any of its contained members, and
     * vice versa.
     * <p>
     * To save on CPU, this method does not test for trivial overlap
     * (<code>box1.intersects(box2)</code>) which is assumed to have already been tested positively.
     *
     * @param that the other instance
     * @return true if (incompatible) overlap is detected
     */
    boolean overlaps (Inter that);

    /**
     * Assign the bounding box for this interpretation.
     * The assigned bounds may be different from the underlying glyph bounds.
     *
     * @param box the bounding box
     */
    void setBounds (Rectangle box);

    /**
     * Assign the contextual grade, (0..1 probability) computed for
     * interpretation.
     *
     * @param value the contextual grade value
     */
    void setContextualGrade (double value);

    /**
     * Set the containing ensemble for this inter.
     *
     * @param ensemble the containing ensemble
     */
    void setEnsemble (InterEnsemble ensemble);

    /**
     * Assign an intrinsic grade (0..1 probability) to interpretation
     *
     * @param grade the new value for intrinsic grade
     */
    void setGrade (double grade);

    /**
     * Assign an id to the interpretation
     *
     * @param id the inter id
     */
    void setId (int id);

    /**
     * Assign the mirror instance.
     *
     * @param mirror the mirrored instance
     */
    void setMirror (Inter mirror);

    /**
     * Assign the containing SIG
     *
     * @param sig the containing SIG
     */
    void setSig (SIGraph sig);

    /**
     * Report a shape-based string.
     *
     * @return shape.toString() by default. To be overridden if shape is null.
     */
    String shapeString ();

    /**
     * Un-delete this instance.
     */
    void undelete ();

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Ratio intrinsicRatio = new Constant.Ratio(
                0.8,
                "Reduction ratio applied on any intrinsic grade");

        final Constant.Ratio minGrade = new Constant.Ratio(
                0.1,
                "Default minimum interpretation grade");

        final Constant.Ratio minContextualGrade = new Constant.Ratio(
                0.5,
                "Default minimum interpretation contextual grade");

        final Constant.Ratio goodGrade = new Constant.Ratio(
                0.5,
                "Default good interpretation grade");
    }
}
