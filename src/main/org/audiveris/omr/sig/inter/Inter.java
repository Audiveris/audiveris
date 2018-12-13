//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            I n t e r                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.ui.util.AttachmentHolder;
import org.audiveris.omr.util.Entity;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Set;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Interface {@code Inter} defines a possible interpretation.
 * <p>
 * Every Inter instance is assigned an <i>intrinsic</i> grade in range [0..1].
 * There usually exist two thresholds on grade value:
 * <ol>
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

    /**
     * Call-back when this instance has just been added to SIG.
     * This is meant for additional inter house keeping.
     */
    void added ();

    /**
     * Run checks to detect if this inter is abnormal.
     *
     * @return the resulting status
     */
    boolean checkAbnormal ();

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
     * Report all the ensembles this inter is member of.
     * In rare cases, an inter may (temporarily) be part of several ensembles.
     *
     * @return the set of all containing ensembles, perhaps empty but never null
     * @see #getEnsemble()
     */
    Set<Inter> getAllEnsembles ();

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
     * Report the color to use, depending on inter shape and status.
     *
     * @return the color to display inter
     */
    Color getColor ();

    /**
     * Report the contextual grade, (0..1 probability) computed for interpretation.
     *
     * @return the contextual grade, if any, or null
     */
    Double getContextualGrade ();

    /**
     * Assign the contextual grade, (0..1 probability) computed for interpretation.
     *
     * @param value the contextual grade value
     */
    void setContextualGrade (double value);

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
     * Actually, it returns the first containing ensemble found.
     *
     * @return the containing ensemble or null
     * @see #getAllEnsembles()
     */
    InterEnsemble getEnsemble ();

    /**
     * Report the glyph, if any, which is concerned by this interpretation.
     *
     * @return the underlying glyph, or null
     */
    Glyph getGlyph ();

    /**
     * Assign the glyph which is concerned by this interpretation.
     *
     * @param glyph the underlying glyph (non null)
     */
    void setGlyph (Glyph glyph);

    /**
     * Report the intrinsic grade (0..1 probability) assigned to interpretation
     *
     * @return the intrinsic grade
     */
    double getGrade ();

    /**
     * Assign an intrinsic grade (0..1 probability) to interpretation
     *
     * @param grade the new value for intrinsic grade
     */
    void setGrade (double grade);

    /**
     * Report details about the final grade
     *
     * @return the grade details
     */
    GradeImpacts getImpacts ();

    /**
     * Report the inter, if any, this instance is a mirror of.
     * <p>
     * This is used only for HeadInter and HeadChordInter classes.
     *
     * @return the mirror instance or null
     */
    Inter getMirror ();

    /**
     * Assign the mirror instance.
     *
     * @param mirror the mirrored instance
     */
    void setMirror (Inter mirror);

    /**
     * Report the containing part, if any.
     *
     * @return the containing part, or null
     */
    Part getPart ();

    /**
     * Assign the related part, if any.
     *
     * @param part the part to set
     */
    void setPart (Part part);

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
     * Assign the containing SIG
     *
     * @param sig the containing SIG
     */
    void setSig (SIGraph sig);

    /**
     * Report the related staff, if any.
     *
     * @return the related staff or null
     */
    Staff getStaff ();

    /**
     * Assign the related staff, if any.
     *
     * @param staff the staff to set
     */
    void setStaff (Staff staff);

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
     * Report whether this instance is in abnormal state (such as lacking a mandatory
     * relation).
     *
     * @return true if abnormal
     */
    boolean isAbnormal ();

    /**
     * Set this inter as an abnormal one.
     *
     * @param abnormal new value
     */
    void setAbnormal (boolean abnormal);

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
     * Report whether this instance has been set manually.
     *
     * @return true if manual
     */
    boolean isManual ();

    /**
     * Set this inter as a manual one.
     *
     * @param manual new value
     */
    void setManual (boolean manual);

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
     * {@code if (one.overlaps(two) && two.overlaps(one)) {...}}
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
     * @throws DeletedInterException when an Inter instance no longer exists in its SIG
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
     * Look for potential partners around this inter instance.
     * <p>
     * NOTA: Since this method can be used with a not-yet-settled candidate, implementations cannot
     * assume that Inter instance already has a related staff or sig.
     * <p>
     * Relationships that are searched for inters (which cannot survive without such link):
     * <ul>
     * <li>For a flag: 1 stem
     * <li>For a head: 1 stem
     * <li>For a stem: 1 head
     * <li>For an alteration: 1 head
     * <li>For an arpeggiato: 1 headChord
     * <li>For an articulation: 1 headChord
     * <li>For an augmentation dot: 1 note or another dot
     * <li>For a dynamic: 1 chord (really?)
     * <li>For a slur: 1 or 2 heads (or connection across system/page break)
     * <li>For a tuplet: 3 or 6 chords (approximately)
     * <li>For a fermata: 1 barline of 1 chord
     * </ul>
     * Manual inters survive but are displayed in red, to show they are not yet in normal status.
     *
     * @param system containing system
     * @param doit   if true, relations are actually added to the sig
     * @return the collection of links found, perhaps empty
     */
    Collection<Link> searchLinks (SystemInfo system,
                                  boolean doit);

    /**
     * Assign the bounding box for this interpretation.
     * The assigned bounds may be different from the underlying glyph bounds.
     *
     * @param box the bounding box
     */
    void setBounds (Rectangle box);

    /**
     * Report a shape-based string.
     *
     * @return shape.toString() by default. To be overridden if shape is null.
     */
    String shapeString ();
}
