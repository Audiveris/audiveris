//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S t e m I n t e r                                      //
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

import ij.process.ByteProcessor;

import org.audiveris.omr.glyph.BasicGlyph;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.run.Orientation.VERTICAL;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.relation.AbstractStemConnection;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.ChordStemRelation;
import org.audiveris.omr.sig.relation.FlagStemRelation;
import org.audiveris.omr.sig.relation.HeadHeadRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.StemPortion;
import static org.audiveris.omr.sig.relation.StemPortion.STEM_BOTTOM;
import static org.audiveris.omr.sig.relation.StemPortion.STEM_TOP;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code StemInter} represents Stem interpretations.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "stem")
public class StemInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(StemInter.class);

    /** Anchor vertical margin, relative to head height. */
    private static final double ANCHOR_MARGIN_RATIO = 0.67;

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Top point. */
    @XmlElement
    private Point2D top;

    /** Bottom point. */
    @XmlElement
    private Point2D bottom;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new StemInter object.
     *
     * @param glyph   the underlying glyph
     * @param impacts the grade details
     */
    public StemInter (Glyph glyph,
                      GradeImpacts impacts)
    {
        super(glyph, null, Shape.STEM, impacts);
        top = glyph.getStartPoint(VERTICAL);
        bottom = glyph.getStopPoint(VERTICAL);
    }

    /**
     * Creates a new StemInter object.
     *
     * @param glyph the underlying glyph
     * @param grade the assigned grade
     */
    public StemInter (Glyph glyph,
                      double grade)
    {
        super(glyph, null, Shape.STEM, grade);

        if (glyph != null) {
            top = glyph.getStartPoint(VERTICAL);
            bottom = glyph.getStopPoint(VERTICAL);
        }
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private StemInter ()
    {
        super(null, null, null, null);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //-------//
    // added //
    //-------//
    @Override
    public void added ()
    {
        super.added();

        setAbnormal(true); // No head linked yet
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        // Check if a head is connected
        setAbnormal(!sig.hasRelation(this, HeadStemRelation.class));

        return isAbnormal();
    }

    //---------------------//
    // computeAnchoredLine //
    //---------------------//
    /**
     * Compute the line between extreme anchors, assuming that wrong-side ending heads
     * have been disconnected.
     *
     * @return the anchor line
     */
    public Line2D computeAnchoredLine ()
    {
        final Set<Relation> links = sig.getRelations(this, HeadStemRelation.class);

        if (!links.isEmpty()) {
            int dir = computeDirection();

            if (dir > 0) {
                // Stem down, heads are at top of stem
                double yAnchor = Double.MAX_VALUE;

                for (Relation rel : links) {
                    HeadInter head = (HeadInter) sig.getEdgeSource(rel);
                    Rectangle headBox = head.bounds;
                    double y = headBox.y - (ANCHOR_MARGIN_RATIO * headBox.height);

                    if (y < yAnchor) {
                        yAnchor = y;
                    }
                }

                if (yAnchor > top.getY()) {
                    return new Line2D.Double(new Point2D.Double(top.getX(), yAnchor), bottom);
                }
            } else if (dir < 0) {
                // Stem up, heads are at bottom of stem
                double yAnchor = Double.MIN_VALUE;

                for (Relation rel : links) {
                    HeadInter head = (HeadInter) sig.getEdgeSource(rel);
                    Rectangle headBox = head.bounds;
                    double y = headBox.y + ((1 - ANCHOR_MARGIN_RATIO) * headBox.height);

                    if (y > yAnchor) {
                        yAnchor = y;
                    }
                }

                if (yAnchor < bottom.getY()) {
                    return new Line2D.Double(top, new Point2D.Double(bottom.getX(), yAnchor));
                }
            }
        }

        // No change
        return new Line2D.Double(top, bottom);
    }

    //------------------//
    // computeDirection //
    //------------------//
    /**
     * Report the direction (from head to tail) of this stem, compliant with standard
     * display y orientation (-1 for stem up, +1 for stem down, 0 for unknown).
     * <p>
     * For this, we check what is found on each stem end (is it a tail: beam/flag or is it a head)
     * and use contextual grade to pick up the best reference.
     *
     * @return the stem direction
     */
    public int computeDirection ()
    {
        Scale scale = sig.getSystem().getSheet().getScale();
        final Line2D stemLine = computeExtendedLine();
        final List<Relation> links = new ArrayList<Relation>(
                sig.getRelations(this, AbstractStemConnection.class));
        sig.sortBySource(links);

        for (Relation rel : links) {
            Inter source = sig.getEdgeSource(rel); // Source is a head, a beam or a flag

            // Retrieve the stem portion for this link
            if (rel instanceof HeadStemRelation) {
                // Head -> Stem
                HeadStemRelation link = (HeadStemRelation) rel;
                StemPortion portion = link.getStemPortion(source, stemLine, scale);

                if (portion == STEM_BOTTOM) {
                    if (link.getHeadSide() == RIGHT) {
                        return -1;
                    }
                } else if (portion == STEM_TOP) {
                    if (link.getHeadSide() == LEFT) {
                        return 1;
                    }
                }
            } else if (rel instanceof BeamStemRelation) {
                // Beam -> Stem
                BeamStemRelation link = (BeamStemRelation) rel;
                StemPortion portion = link.getStemPortion(source, stemLine, scale);

                return (portion == STEM_TOP) ? (-1) : 1;
            } else {
                // Flag -> Stem
                FlagStemRelation link = (FlagStemRelation) rel;
                StemPortion portion = link.getStemPortion(source, stemLine, scale);

                if (portion == STEM_TOP) {
                    return -1;
                }

                if (portion == STEM_BOTTOM) {
                    return 1;
                }
            }
        }

        return 0; // Cannot decide with current config!
    }

    //---------------------//
    // computeExtendedLine //
    //---------------------//
    /**
     * Compute the extended line, taking all stem connections into account.
     *
     * @return the connection range
     */
    public Line2D computeExtendedLine ()
    {
        Point2D extTop = new Point2D.Double(top.getX(), top.getY());
        Point2D extBottom = new Point2D.Double(bottom.getX(), bottom.getY());

        for (Relation rel : sig.getRelations(this, AbstractStemConnection.class)) {
            AbstractStemConnection link = (AbstractStemConnection) rel;
            Point2D ext = link.getExtensionPoint();

            if (ext != null) {
                if (ext.getY() < extTop.getY()) {
                    extTop = ext;
                }

                if (ext.getY() > extBottom.getY()) {
                    extBottom = ext;
                }
            }
        }

        return new Line2D.Double(extTop, extBottom);
    }

    //-----------//
    // duplicate //
    //-----------//
    public StemInter duplicate ()
    {
        StemInter clone = new StemInter(glyph, impacts);
        clone.setGlyph(this.glyph);
        clone.setMirror(this);

        if (impacts == null) {
            clone.setGrade(this.grade);
        }

        sig.addVertex(clone);
        setMirror(clone);

        return clone;
    }

    //----------------//
    // extractSubStem //
    //----------------//
    /**
     * Build a new stem from a portion of this one (extrema ordinates can be provided
     * in any order).
     *
     * @param y1 ordinate of one side of sub-stem
     * @param y2 ordinate of the other side of sub-stem
     * @return the extracted sub-stem inter
     */
    public StemInter extractSubStem (int y1,
                                     int y2)
    {
        final int yTop = Math.min(y1, y2);
        final int yBottom = Math.max(y1, y2);

        final Sheet sheet = sig.getSystem().getSheet();
        final ByteProcessor buffer = glyph.getRunTable().getBuffer();

        // ROI definition (WRT stem buffer coordinates)
        final Rectangle roi = new Rectangle(
                0,
                yTop - glyph.getTop(),
                glyph.getWidth(),
                yBottom - yTop + 1);

        // Create sub-glyph
        final Point stemOffset = new Point();
        final RunTableFactory factory = new RunTableFactory(VERTICAL);
        final RunTable table = factory.createTable(buffer, roi).trim(stemOffset);
        final int x = glyph.getLeft() + stemOffset.x;
        final int y = glyph.getTop() + roi.y + stemOffset.y;
        final Glyph g = sheet.getGlyphIndex().registerOriginal(
                new BasicGlyph(x, y, table));

        // Create sub-stem
        final StemInter subStem = new StemInter(g, getGrade());
        sheet.getInterIndex().register(subStem);
        sig.addVertex(subStem);

        return subStem;
    }

    //----------//
    // getBeams //
    //----------//
    /**
     * Report the beams linked to this stem.
     *
     * @return set of linked beams
     */
    public Set<AbstractBeamInter> getBeams ()
    {
        final Set<AbstractBeamInter> set = new LinkedHashSet<AbstractBeamInter>();

        for (Relation relation : sig.getRelations(this, BeamStemRelation.class)) {
            set.add((AbstractBeamInter) sig.getEdgeSource(relation));
        }

        return set;
    }

    //-----------//
    // getBottom //
    //-----------//
    /**
     * @return the bottom
     */
    public Point2D getBottom ()
    {
        return bottom;
    }

    //-----------//
    // getChords //
    //-----------//
    /**
     * Report the chord(s) currently attached to the provided stem.
     * <p>
     * We can have: <ul>
     * <li>No chord found, simply because this stem has not yet been processed.</li>
     * <li>One chord found, this is the normal case.</li>
     * <li>Two chords found, when the same stem is "shared" by two chords (as in complex structures
     * like in Dichterliebe example, part 2, page 2, measure 14).</li>
     * </ul>
     *
     * @return the perhaps empty collection of chords found for this stem
     */
    public List<HeadChordInter> getChords ()
    {
        List<HeadChordInter> chords = null;

        for (Relation rel : sig.getRelations(this, ChordStemRelation.class)) {
            if (chords == null) {
                chords = new ArrayList<HeadChordInter>();
            }

            chords.add((HeadChordInter) sig.getOppositeInter(this, rel));
        }

        if (chords == null) {
            return Collections.EMPTY_LIST;
        }

        return chords;
    }

    //----------//
    // getHeads //
    //----------//
    /**
     * Report the heads linked to this stem, whatever the side.
     *
     * @return set of linked heads
     */
    public Set<HeadInter> getHeads ()
    {
        final Set<HeadInter> set = new LinkedHashSet<HeadInter>();

        for (Relation relation : sig.getRelations(this, HeadStemRelation.class)) {
            set.add((HeadInter) sig.getEdgeSource(relation));
        }

        return set;
    }

    //-----------//
    // getMedian //
    //-----------//
    /**
     * Report the stem median line which goes from stem top to stem bottom.
     *
     * @return the stem median line
     */
    public Line2D getMedian ()
    {
        return new Line2D.Double(top, bottom);
    }

    //-------------//
    // getMinGrade //
    //-------------//
    public static double getMinGrade ()
    {
        return AbstractInter.getMinGrade();
    }

    //--------//
    // getTop //
    //--------//
    /**
     * @return the top
     */
    public Point2D getTop ()
    {
        return top;
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        for (Relation rel : sig.getRelations(this, HeadStemRelation.class)) {
            return sig.getOppositeInter(this, rel).getVoice();
        }

        return null;
    }

    //-------------//
    // isGraceStem //
    //-------------//
    /**
     * Report whether this stem is linked to grace note heads (rather than standard heads)
     *
     * @return true if connected head is small
     */
    public boolean isGraceStem ()
    {
        for (Relation rel : sig.getRelations(this, HeadStemRelation.class)) {
            Shape headShape = sig.getOppositeInter(this, rel).getShape();

            // First head tested is enough.
            return (headShape == Shape.NOTEHEAD_BLACK_SMALL)
                   || (headShape == Shape.NOTEHEAD_VOID_SMALL);
        }

        return false;
    }

    //------------//
    // lookupHead //
    //------------//
    /**
     * Lookup a head connected to this stem, with proper head side and pitch values.
     * Beware side is defined WRT head, not WRT stem.
     *
     * @param side  desired head side
     * @param pitch desired pitch position
     * @return the head instance if found, null otherwise
     */
    public Inter lookupHead (HorizontalSide side,
                             int pitch)
    {
        for (Relation rel : sig.getRelations(this, HeadStemRelation.class)) {
            HeadStemRelation hsRel = (HeadStemRelation) rel;

            // Check side
            if (hsRel.getHeadSide() == side) {
                // Check pitch
                HeadInter head = (HeadInter) sig.getEdgeSource(rel);

                if (head.getIntegerPitch() == pitch) {
                    return head;
                }
            }
        }

        return null;
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove head-head relations that were based on this stem.
     *
     * @param extensive true for non-manual removals only
     * @see #added()
     */
    @Override
    public void remove (boolean extensive)
    {
        if (isRemoved()) {
            return;
        }

        if (isGood()) {
            // Discard head-head relations that are based only on this stem instance
            Set<HeadInter> stemHeads = getHeads(); // Heads linked to this stem

            for (HeadInter head : stemHeads) {
                // Other stems this head is linked to
                Set<StemInter> otherStems = head.getStems();
                otherStems.remove(this);

                for (Relation rel : sig.getRelations(head, HeadHeadRelation.class)) {
                    HeadInter similarHead = (HeadInter) sig.getOppositeInter(head, rel);

                    if (stemHeads.contains(similarHead)) {
                        // Head - otherHead are both on this stem
                        // Keep HH support only if they are on same good stem (different of this)
                        Set<StemInter> similarStems = similarHead.getStems();
                        similarStems.retainAll(otherStems);

                        if (!Inters.hasGoodMember(similarStems)) {
                            logger.debug("Removing head-head within {} & {}", head, similarHead);
                            sig.removeEdge(rel);
                        }
                    }
                }
            }
        }

        super.remove(extensive);
    }

    //-----------//
    // setBounds //
    //-----------//
    @Override
    public void setBounds (Rectangle bounds)
    {
        super.setBounds(bounds);

        if (top == null) {
            top = new Point2D.Double(bounds.x + (0.5 * bounds.width), bounds.y);
        }

        if (bottom == null) {
            bottom = new Point2D.Double(
                    bounds.x + (0.5 * bounds.width),
                    (bounds.y + bounds.height) - 1);
        }
    }

    //----------//
    // setGlyph //
    //----------//
    @Override
    public void setGlyph (Glyph glyph)
    {
        super.setGlyph(glyph);

        top = glyph.getStartPoint(VERTICAL);
        bottom = glyph.getStopPoint(VERTICAL);
    }
}
