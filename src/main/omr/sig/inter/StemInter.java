//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S t e m I n t e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.BasicGlyph;
import omr.glyph.Glyph;
import omr.glyph.Shape;
import static omr.run.Orientation.VERTICAL;
import omr.run.RunTable;
import omr.run.RunTableFactory;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.rhythm.Voice;

import omr.sig.GradeImpacts;
import omr.sig.relation.AbstractStemConnection;
import omr.sig.relation.BeamStemRelation;
import omr.sig.relation.FlagStemRelation;
import omr.sig.relation.HeadStemRelation;
import omr.sig.relation.Relation;
import omr.sig.relation.StemPortion;
import static omr.sig.relation.StemPortion.STEM_BOTTOM;
import static omr.sig.relation.StemPortion.STEM_TOP;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.LEFT;
import static omr.util.HorizontalSide.RIGHT;

import ij.process.ByteProcessor;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
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

    /** Anchor vertical margin, relative to head height. */
    private static final double ANCHOR_MARGIN_RATIO = 0.67;

    //~ Instance fields ----------------------------------------------------------------------------
    /** Top point. */
    @XmlElement
    private final Point2D top;

    /** Bottom point. */
    @XmlElement
    private final Point2D bottom;

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
        top = glyph.getStartPoint(VERTICAL);
        bottom = glyph.getStopPoint(VERTICAL);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private StemInter ()
    {
        super(null, null, null, null);
        top = bottom = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // getMinGrade //
    //-------------//
    public static double getMinGrade ()
    {
        return AbstractInter.getMinGrade();
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
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
                    AbstractHeadInter head = (AbstractHeadInter) sig.getEdgeSource(rel);
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
                    AbstractHeadInter head = (AbstractHeadInter) sig.getEdgeSource(rel);
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

            if (ext.getY() < extTop.getY()) {
                extTop = ext;
            }

            if (ext.getY() > extBottom.getY()) {
                extBottom = ext;
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

    //--------//
    // isGood //
    //--------//
    @Override
    public boolean isGood ()
    {
        return getGrade() >= 0.45; // BINGO DIRTY HACK
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
            Shape shape = sig.getOppositeInter(this, rel).getShape();

            // First head tested is enough.
            return (shape == Shape.NOTEHEAD_BLACK_SMALL) || (shape == Shape.NOTEHEAD_VOID_SMALL);
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
                AbstractHeadInter head = (AbstractHeadInter) sig.getEdgeSource(rel);

                if (head.getIntegerPitch() == pitch) {
                    return head;
                }
            }
        }

        return null;
    }
}
