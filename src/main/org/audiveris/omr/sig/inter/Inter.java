//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            I n t e r                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.sig.ui.InterTracker;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.util.AttachmentHolder;
import org.audiveris.omr.util.Entity;
import org.audiveris.omr.util.Version;
import org.audiveris.omr.util.WrappedBoolean;
import org.audiveris.omr.util.Wrapper;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Interface <code>Inter</code> defines a possible interpretation.
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
@XmlJavaTypeAdapter(AbstractInter.JaxbAdapter.class)
public interface Inter
        extends Entity, VisitableInter, AttachmentHolder
{
    //~ Methods ------------------------------------------------------------------------------------

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
     * Derive (ghost) inter geometry from the provided symbol, font and current mouse location
     * (when ghost is dragged, dropped or when created with repetitive input).
     *
     * @param symbol       the dropped symbol
     * @param sheet        containing sheet
     * @param font         properly sized font
     * @param dropLocation (input/output) current drag/drop location.
     *                     Input: location is assumed to be the symbol focus center
     *                     Output: location may get modified when some snapping occurs
     * @return true if OK
     */
    boolean deriveFrom (ShapeSymbol symbol,
                        Sheet sheet,
                        MusicFont font,
                        Point dropLocation);

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
     * Report the inter center point.
     *
     * @return center as Point
     */
    Point getCenter ();

    /**
     * Report the inter center point2D.
     *
     * @return center as Point2D
     */
    Point2D getCenter2D ();

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
     * Report (a COPY of) the core bounds for this interpretation.
     *
     * @return a COPY of core box
     */
    Rectangle2D getCoreBounds ();

    /**
     * Details for tip.
     *
     * @return informations for a tip
     */
    String getDetails ();

    /**
     * Report a proper editor for manual inter edition.
     *
     * @return proper editor
     */
    InterEditor getEditor ();

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
     * Report the intrinsic grade (0..1 probability) assigned to interpretation
     *
     * @return the intrinsic grade, null if unknown
     */
    Double getGrade ();

    /**
     * Report details about the final grade
     *
     * @return the grade details
     */
    GradeImpacts getImpacts ();

    /**
     * Retrieve the current links around this inter.
     *
     * @return its links, perhaps empty
     */
    Collection<Link> getLinks ();

    /**
     * Report the inter, if any, this instance is a mirror of.
     * <p>
     * This is used only for HeadInter and HeadChordInter classes.
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
     * Report the profile level for relations with this inter
     *
     * @return level based on being manual or not
     */
    int getProfile ();

    /**
     * Report the inter center for relation drawing.
     *
     * @return center for relation
     */
    Point2D getRelationCenter ();

    /**
     * Report the inter center for drawing the specified relation instance.
     *
     * @param relation the provided relation if any
     * @return center for relation
     */
    Point2D getRelationCenter (Relation relation);

    /**
     * Report the shape related to interpretation.
     *
     * @return the shape
     */
    Shape getShape ();

    /**
     * Report a shape-based string.
     *
     * @return shape.toString() by default.
     */
    String getShapeString ();

    /**
     * Report a shape-based symbol.
     *
     * @param family the MusicFont family
     * @return shape.getDecoratedSymbol() by default.
     */
    ShapeSymbol getShapeSymbol (MusicFamily family);

    /**
     * Report the sig which hosts this interpretation.
     *
     * @return the containing sig
     */
    SIGraph getSig ();

    /**
     * Report the containing part specifically assigned, if any.
     *
     * @return the containing part specifically assigned, or null
     */
    Part getSpecificPart ();

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
     * @return a copy of the symbol bounding box
     * @see #getBounds()
     */
    Rectangle getSymbolBounds (int interline);

    /**
     * Report a suitable tracker, to render decorations on the fly.
     *
     * @param sheet containing sheet
     * @return suitable tracker for this inter
     */
    InterTracker getTracker (Sheet sheet);

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
     * Do we impose the inter to be located within staff limits (width and height).
     *
     * @return true if so
     */
    boolean imposeWithinStaffLimits ();

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
     * Report whether the interpretation has a good contextual grade.
     *
     * @return true if contextual grade is good
     */
    boolean isContextuallyGood ();

    /**
     * Report whether the inter can be manually edited.
     *
     * @return true id editable
     */
    boolean isEditable ();

    /**
     * Report whether this instance has been frozen.
     *
     * @return true if frozen
     */
    boolean isFrozen ();

    /**
     * Report whether the interpretation has a good (intrinsic) grade.
     *
     * @return true if grade is (intrinsically) good
     */
    boolean isGood ();

    /**
     * Report whether this instance is implicit.
     *
     * @return true if implicit
     */
    boolean isImplicit ();

    /**
     * Report whether this instance has been set manually.
     *
     * @return true if manual
     */
    boolean isManual ();

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
     * <code>if (one.overlaps(two) &amp;&amp; two.overlaps(one)) {...</code>}
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
     * Prepare the manual addition of this inter, for which only staff and bounds have
     * been set (notably, sig is not yet set).
     * <p>
     * Build <b>all</b> the UI tasks to insert this inter: the addition task itself, together with
     * related tasks if any (other additions, links, ...).
     *
     * @param cancel    (output) ability to cancel processing by setting its value to true
     * @param toPublish (output) ability to designate a different inter instance to be published
     * @return the sequence of UI tasks
     */
    List<? extends UITask> preAdd (WrappedBoolean cancel,
                                   Wrapper<Inter> toPublish);

    /**
     * Prepare the manual edition of this inter.
     *
     * @param editor the current editor on this inter
     * @return the sequence of additional UI tasks
     */
    List<? extends UITask> preEdit (InterEditor editor);

    /**
     * Prepare the manual removal of this inter.
     *
     * @param cancel (input/output)
     *               A null-value for cancel means user has already validated the decision, so the
     *               user should not be prompted for further confirmation.
     *               If non null, the method can cancel processing by setting its value to true.
     * @return the inters to remove: that is this inter plus all other inters to remove,
     *         with the exception of ensemble members since those are handled automatically.
     */
    Set<? extends Inter> preRemove (WrappedBoolean cancel);

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
     * WARNING: This method is reserved for administrative purpose only.
     *
     * @param shape new shape for the inter
     * @return true if shape was actually renamed
     */
    boolean renameShapeAs (Shape shape);

    /**
     * Look for potential partners around this inter instance.
     * <p>
     * NOTA: Since this method can be used with a not-yet-settled candidate, implementations cannot
     * assume that Inter instance already has a related staff or sig.
     * <p>
     * Relationships that are searched for inters (plain inters cannot survive without such links):
     * <ul>
     * <li>For a flag: 1 stem
     * <li>For a head: 1 stem
     * <li>For a stem: 1 head
     * <li>For an alteration: 1 head
     * <li>For an arpeggiato: 1 headChord, perhaps 2 vertically aligned
     * <li>For an articulation: 1 headChord
     * <li>For an augmentation dot: 1 note or another dot
     * <li>For a dynamic: 1 chord (really?)
     * <li>For a slur: 1 head + a second head or a connection across system/page break
     * <li>For a tuplet: 3 or 6 chords (approximately)
     * <li>For a fermata: 1 barline or 1 chord
     * <li>For a measure number: 1 multiple rest or measure repeat sign
     * <li>For an octave shift: 1 chord
     * </ul>
     * Manual inters survive but are displayed in red, to show they are not yet in normal status.
     * <p>
     * This method provides no 'profile' parameter.
     * The profile level should be determined from inside the method implementation, using both the
     * Inter instance itself (a manual inter has a specific profile level) and the profile level of
     * the containing sheet.
     *
     * @param system containing system
     * @return the collection of links found, perhaps empty
     */
    Collection<Link> searchLinks (SystemInfo system);

    /**
     * Look for existing relations that would be discarded in favor of the provided links.
     *
     * @param system containing system
     * @param links  potential links
     * @return links to be discarded
     */
    Collection<Link> searchUnlinks (SystemInfo system,
                                    Collection<Link> links);

    /**
     * Set this inter as an abnormal one.
     *
     * @param abnormal new value
     */
    void setAbnormal (boolean abnormal);

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
    void setGrade (Double grade);

    /**
     * Set this inter as implicit.
     *
     * @param implicit new value
     */
    void setImplicit (boolean implicit);

    /**
     * Set this inter as a manual one.
     *
     * @param manual new value
     */
    void setManual (boolean manual);

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
     * Check inter instance for an upgrade with respect to its persisted data.
     * <p>
     * This is meant to ease transition of .omr files produced by previous Audiveris versions.
     *
     * @param upgrades sequence of upgrading versions to apply
     * @return true if any upgrade was actually performed
     */
    boolean upgradeOldStuff (List<Version> upgrades);
}
