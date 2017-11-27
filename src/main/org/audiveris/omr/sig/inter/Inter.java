//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            I n t e r                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.Partnership;
import org.audiveris.omr.ui.util.AttachmentHolder;
import org.audiveris.omr.util.Entity;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.Collection;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Interface {@code Inter} defines a possible interpretation.
 * <p>
 * Every Inter instance is assigned an <i>intrinsic</i> grade in range [0..1].
 * There usually exist two thresholds on grade value:<ol>
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
@XmlJavaTypeAdapter(AbstractInter.Adapter.class)
public interface Inter
        extends Entity, VisitableInter, AttachmentHolder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    static final Constants constants = new Constants();

    /** Ratio applied on intrinsic value, to leave room for contextual. */
    static double intrinsicRatio = constants.intrinsicRatio.getValue();

    /** The minimum grade to consider an interpretation as acceptable. */
    static double minGrade = intrinsicRatio * constants.minGrade.getValue();

    /** The minimum contextual grade for an interpretation. */
    static double minContextualGrade = constants.minContextualGrade.getValue();

    /** The minimum grade to consider an interpretation as good. */
    static double goodGrade = intrinsicRatio * constants.goodGrade.getValue();

    /** The minimum grade to consider an interpretation as really good. */
    static double reallyGoodGrade = intrinsicRatio * constants.reallyGoodGrade.getValue();

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Call-back when this instance has just been added to SIG.
     * This is meant for additional inter house keeping.
     */
    void added ();

    /**
     * Decrease the inter grade.
     *
     * @param ratio ratio applied
     */
    void decrease (double ratio);

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
     * Report the core bounds for this interpretation.
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
     * Report the containing part, if any.
     *
     * @return the containing part, or null
     */
    Part getPart ();

    /**
     * Report the inter center for relation drawing.
     *
     * @return center for relation
     */
    Point getRelationCenter ();

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
     * @see #getBounds()
     */
    Rectangle getSymbolBounds (int interline);

    /**
     * Report the voice, if any, this inter belongs to
     *
     * @return the inter voice or null
     */
    Voice getVoice ();

    /**
     * Report whether a staff has been assigned.
     *
     * @return true if staff assigned
     */
    boolean hasStaff ();

    /**
     * Increase the inter grade.
     *
     * @param ratio ratio applied
     */
    void increase (double ratio);

    /**
     * Nullify cached data.
     */
    void invalidateCache ();

    /**
     * Report whether the interpretation has a good contextual grade.
     *
     * @return true if contextual grade is good
     */
    boolean isContextuallyGood ();

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
     * Report whether the interpretation has a really good (intrinsic) grade.
     *
     * @return true if grade is good
     */
    boolean isReallyGood ();

    /**
     * Report whether this instance has been removed from SIG.
     *
     * @return true if removed
     */
    boolean isRemoved ();

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
     * @throws org.audiveris.omr.sig.inter.DeletedInterException
     */
    boolean overlaps (Inter that)
            throws DeletedInterException;

    /**
     * Remove this Inter instance from its containing SIG.
     * This is equivalent to remove(true).
     */
    void remove ();

    /**
     * Remove this Inter instance from its containing SIG.
     *
     * @param extensive option to forward removal to containing ensemble if any
     */
    void remove (boolean extensive);

    /**
     * Look for partners around this inter instance.
     * <p>
     * Relationships that are searched for inters (which cannot survive without such partnership):
     * <ul>
     * <li>For a flag: 1 stem
     * <li>For a head: 1 stem
     * <li>For an alteration: 1 head
     * <li>For an arpeggiato: 1 headChord
     * <li>For an articulation: 1 headChord
     * <li>For a dynamic: 1 chord
     * <li>For a tuplet: 3 or 6 chords (approximately)
     * </ul>
     *
     * @param system containing system
     * @param doit   if true, relations are actually added to the sig
     * @return the collection of partnerships found, perhaps empty
     */
    Collection<Partnership> searchPartnerships (SystemInfo system,
                                                boolean doit);

    /**
     * Assign the bounding box for this interpretation.
     * The assigned bounds may be different from the underlying glyph bounds.
     *
     * @param box the bounding box
     */
    void setBounds (Rectangle box);

    /**
     * Assign the contextual grade, (0..1 probability) computed for interpretation.
     *
     * @param value the contextual grade value
     */
    void setContextualGrade (double value);

    /**
     * Assign the glyph which is concerned by this interpretation.
     *
     * @param glyph the underlying glyph (non null)
     */
    void setGlyph (Glyph glyph);

    /**
     * Assign an intrinsic grade (0..1 probability) to interpretation
     *
     * @param grade the new value for intrinsic grade
     */
    void setGrade (double grade);

    /**
     * Assign the mirror instance.
     *
     * @param mirror the mirrored instance
     */
    void setMirror (Inter mirror);

    /**
     * Assign the related part, if any.
     *
     * @param part the part to set
     */
    void setPart (Part part);

    /**
     * Assign the containing SIG
     *
     * @param sig the containing SIG
     */
    void setSig (SIGraph sig);

    /**
     * Assign the related staff, if any.
     *
     * @param staff the staff to set
     */
    void setStaff (Staff staff);

    /**
     * Report a shape-based string.
     *
     * @return shape.toString() by default. To be overridden if shape is null.
     */
    String shapeString ();

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio intrinsicRatio = new Constant.Ratio(
                0.8,
                "Reduction ratio applied on any intrinsic grade");

        private final Constant.Ratio minGrade = new Constant.Ratio(
                0.1,
                "Default minimum interpretation grade");

        private final Constant.Ratio minContextualGrade = new Constant.Ratio(
                0.5,
                "Default minimum interpretation contextual grade");

        private final Constant.Ratio goodGrade = new Constant.Ratio(
                0.5,
                "Default good interpretation grade");

        private final Constant.Ratio reallyGoodGrade = new Constant.Ratio(
                0.75,
                "Default really good interpretation grade");
    }
}
