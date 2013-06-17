//----------------------------------------------------------------------------//
//                                                                            //
//                                  B e a m                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.Main;

import omr.constant.ConstantSet;

import omr.glyph.facets.Glyph;

import omr.math.BasicLine;
import omr.math.Line;

import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.TreeNode;
import omr.util.Vip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

/**
 * Class {@code Beam} represents a beam "line", that may be composed of
 * several BeamItem "segments", aligned horizontally one after the
 * other, along the same line.
 * It can degenerate to just a single beam hook.
 *
 * <div style="float: right;">
 * <img src="doc-files/Beam.jpg" alt="diagram">
 * </div>
 *
 * @author Hervé Bitteur
 */
public class Beam
        extends MeasureNode
        implements Vip
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Beam.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** (Debug) flag this object as VIP. */
    private boolean vip;

    /** Id for debug. */
    private final int id;

    /** The containing beam group. */
    private BeamGroup group;

    /** Chords that are linked by this beam. */
    private List<Chord> chords = new ArrayList<>();

    /** Line equation for the beam. */
    private Line line;

    //~ Constructors -----------------------------------------------------------
    //
    //------//
    // Beam //
    //------//
    /** Creates a new instance of Beam.
     *
     * @param measure the enclosing measure
     */
    private Beam (Measure measure)
    {
        super(measure);

        id = 1 + getChildIndex();

        logger.debug("{} Created {}", measure.getContextString(), this);
    }

    //~ Methods ----------------------------------------------------------------
    //
    //----------//
    // populate //
    //----------//
    /**
     * Retrieve (or create a brand new) beam to host the item known by
     * its left and right points.
     * Remark: We cannot create the BeamItem instance before its hosting
     * Beam instance exists.
     *
     * @param left    left point of the candidate
     * @param right   right point of the candidate
     * @param measure the containing measure
     * @return the (perhaps new) containing Beam instance
     */
    public static Beam populate (Point left,
                                 Point right,
                                 Measure measure)
    {
        ///logger.info("Populating " + glyph);
        Beam beam = null;

        // Browse existing beams, to check if this item can be appended
        for (TreeNode node : measure.getBeams()) {
            Beam b = (Beam) node;

            if (b.isCompatibleWith(left, right)) {
                beam = b;

                break;
            }
        }

        // If not, create a brand new beam entity
        if (beam == null) {
            beam = new Beam(measure);
        }

//////////////////        beam.addItem(item);
        // TODO: preserve order in items !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

        logger.debug("{} {}", beam.getContextString(), beam);

        return beam;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // addChord //
    //----------//
    /**
     * Insert a chord linked by this beam.
     *
     * @param chord the linked chord
     */
    public void addChord (Chord chord)
    {
        if (!chords.contains(chord)) {
            chords.add(chord);
        }
    }

    //------------------//
    // closeConnections //
    //------------------//
    /**
     * Make sure all connections between this beam and the linked
     * chords/stems are actually recorded.
     */
    public void closeConnections ()
    {
        if (chords.isEmpty()) {
            addError("No chords connected to " + this);
        } else {
            Collections.sort(chords, Chord.byAbscissa);
            Chord first = chords.get(0);
            Chord last = chords.get(chords.size() - 1);
            boolean started = false;

            // Add interleaved chords if any, plus relevant chords of the group
            SortedSet<Chord> adds = Chord.lookupInterleavedChords(first, last);
            adds.add(first);
            adds.add(last);

            for (Chord chord : adds) {
                if (chord == first) {
                    started = true;
                }

                if (started) {
                    addChord(chord);
                    chord.addBeam(this);
                }

                if (chord == last) {
                    break;
                }
            }
        }
    }

    //----------------//
    // determineGroup //
    //----------------//
    /**
     * Determine which BeamGroup this beam is part of.
     * The BeamGroup is either reused (if one of its beams has a linked chord
     * in common with this beam) or created from scratch otherwise
     */
    public void determineGroup ()
    {
        // Check if this beam should belong to an existing group
        for (BeamGroup group : getMeasure().getBeamGroups()) {
            for (Beam beam : group.getBeams()) {
                for (Chord chord : beam.getChords()) {
                    if (this.chords.contains(chord)) {
                        // We have a chord in common with this beam, so we are
                        // part of the same group
                        switchToGroup(group);
                        logger.debug("{} Reused {} for {}",
                                getContextString(), group, this);

                        return;
                    }
                }
            }
        }

        // No compatible group found, let's build a new one
        switchToGroup(new BeamGroup(getMeasure()));

        logger.debug("{} Created new {} for {}",
                getContextString(), getGroup(), this);
    }

    //------//
    // dump //
    //------//
    /**
     * Utility method for easy dumping of the beam entity.
     */
    public void dump ()
    {
        getLine();
        Main.dumping.dump(this);
    }

    //-----------//
    // getChords //
    //-----------//
    /**
     * Report the chords that are linked by this beam.
     *
     * @return the linked chords
     */
    public List<Chord> getChords ()
    {
        return Collections.unmodifiableList(chords);
    }

    //----------//
    // getGroup //
    //----------//
    /**
     * Report the containing group.
     *
     * @return the containing group, if already set, or null
     */
    public BeamGroup getGroup ()
    {
        return group;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the unique id of the beam within its containing measure.
     *
     * @return the beam id, starting from 1
     */
    public int getId ()
    {
        return id;
    }

    //----------//
    // getItems //
    //----------//
    /**
     * Report the ordered sequence of items (one or several BeamItem
     * instances of BEAM shape, or one glyph of BEAM_HOOK shape) that
     * compose this beam.
     *
     * @return the ordered set of beam items
     */
    public List<TreeNode> getItems ()
    {
        return children;
    }

    //--------------//
    // getFirstItem //
    //--------------//
    /**
     * Report the first of beam items
     *
     * @return the first item (on left)
     */
    public BeamItem getFirstItem ()
    {
        return (BeamItem) getItems().get(0);
    }

    //-------------//
    // getLastItem //
    //-------------//
    /**
     * Report the last of beam items
     *
     * @return the last item (on right)
     */
    public BeamItem getLastItem ()
    {
        return (BeamItem) getItems().get(getItems().size() - 1);
    }

    //---------//
    // getLine //
    //---------//
    /**
     * Report the line equation defined by the beam.
     *
     * @return the line equation
     */
    public Line getLine ()
    {
        if ((line == null) && !getItems().isEmpty()) {
            line = new BasicLine();

            // Take left side of first item, and right side of last item
            Point left = getPoint(LEFT);
            line.includePoint(left.x, left.y);
            Point right = getPoint(RIGHT);
            line.includePoint(right.x, right.y);
        }

        return line;
    }

    //----------//
    // getPoint //
    //----------//
    /**
     * Report the point that define the desired edge of the beam.
     *
     * @return the Point coordinates of the point on desired side
     */
    public Point getPoint (HorizontalSide side)
    {
        if (side == LEFT) {
            return getFirstItem().getPoint(LEFT);
        } else {
            return getLastItem().getPoint(RIGHT);
        }
    }

    //----------//
    // setPoint //
    //----------//
    /**
     * Assign the point that define the desired edge of the beam.
     *
     * @param side  the desired side
     * @param point the Point coordinates of the point on desired side
     */
    public void setPoint (HorizontalSide side,
                          Point point)
    {
        if (side == LEFT) {
            getFirstItem().setPoint(LEFT, point);
        } else {
            getLastItem().setPoint(RIGHT, point);
        }

        reset();
    }

    //--------//
    // isHook //
    //--------//
    public boolean isHook ()
    {
        return getFirstItem().isHook();
    }

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip ()
    {
        return vip;
    }

    //------------//
    // linkChords //
    //------------//
    /**
     * Assign the both-way link between this beam and the chords
     * connected by the beam.
     */
    public void linkChords ()
    {
        for (TreeNode node : getItems()) {
            BeamItem item = (BeamItem) node;
            linkChordsOnStems(item);
        }
    }

    //-------------//
    // removeChord //
    //-------------//
    /**
     * Remove a chord from this beam.
     *
     * @param chord the chord to remove
     */
    public void removeChord (Chord chord)
    {
        chords.remove(chord);
    }

    //--------//
    // setVip //
    //--------//
    @Override
    public void setVip ()
    {
        vip = true;
    }

    //---------------//
    // switchToGroup //
    //---------------//
    /**
     * Move this beam to a BeamGroup, by setting the link both ways
     * between this beam and the containing group.
     *
     * @param group the (new) containing beam group
     */
    public void switchToGroup (BeamGroup group)
    {
        logger.debug("Switching {} from {} to {}",
                this, this.group, group);

        // Trivial noop case
        if (this.group == group) {
            return;
        }

        // Remove from current group if any
        if (this.group != null) {
            this.group.removeBeam(this);
        }

        // Assign to new group
        if (group != null) {
            group.addBeam(this);
        }

        // Remember assignment
        this.group = group;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Beam");

        try {
            sb.append("#").append(id);

            if (isHook()) {
                sb.append(" hook");
            }

            sb.append(" items[");
            for (TreeNode node : getItems()) {
                BeamItem item = (BeamItem) node;
                sb.append("#").append(item.getGlyph().getId());
            }
            sb.append("]");

        } catch (NullPointerException e) {
            sb.append(" INVALID");
        }

        sb.append("}");

        return sb.toString();
    }

    //---------------//
    // computeCenter //
    //---------------//
    /**
     * Compute the center of this beam.
     */
    @Override
    protected void computeCenter ()
    {
        Point left = getPoint(LEFT);
        Point right = getPoint(RIGHT);

        setCenter(
                new Point((left.x + right.x) / 2, (left.y + right.y) / 2));
    }

    //-------//
    // reset //
    //-------//
    /**
     * Invalidate cached data, to force its recomputation when needed.
     */
    @Override
    protected void reset ()
    {
        super.reset();

        line = null;
    }

    //----------//
    // addChild //
    //----------//
    @Override
    public void addChild (TreeNode node)
    {
        super.addChild(node);

        reset();
    }

    //------------------//
    // isCompatibleWith //
    //------------------//
    /**
     * Check compatibility of a candidate item with this Beam instance.
     * We use alignment and distance criterias.
     *
     * @param left  left point of item candidate
     * @param right right point of item candidate
     * @return true if compatible
     */
    private boolean isCompatibleWith (Point left,
                                      Point right)
    {
        boolean logging = isVip() || logger.isDebugEnabled();

        // Check alignment, using distance to line
        int centerX = (left.x + right.x) / 2;
        int centerY = (left.y + right.y) / 2;
        double dy = getScale().pixelsToFrac(
                getLine().distanceOf(centerX, centerY));

        if (logging) {
            logger.info("dy={} vs {}", (float) Math.abs(dy),
                    constants.maxDistance.getValue());
        }

        if (Math.abs(dy) > constants.maxDistance.getValue()) {
            return false;
        }

        // Check distance along the same alignment
        for (HorizontalSide side : HorizontalSide.values()) {
            Point itemPoint = (side == LEFT) ? left : right;
            Point beamPoint = getPoint((side == LEFT) ? RIGHT : LEFT);
            double dx = getScale().pixelsToFrac(itemPoint.distance(beamPoint));

            if (logging) {
                logger.info("dx={} vs {}", (float) dx,
                        constants.maxGap.getValue());
            }

            if (dx <= constants.maxGap.getValue()) {
                return true;
            }
        }


        return false;
    }

    //------------------//
    // linkChordsOnStems //
    //------------------//
    private void linkChordsOnStems (BeamItem item)
    {
        for (HorizontalSide side : HorizontalSide.values()) {
            Glyph stem = item.getStem(side);

            if (stem != null) {
                List<Chord> sideChords = Chord.getStemChords(
                        getMeasure(),
                        stem);

                if (!sideChords.isEmpty()) {
                    for (Chord chord : sideChords) {
                        addChord(chord);
                        chord.addBeam(this);
                    }
                } else {
                    addError("Beam with no chord on " + side + " stem");
                }
            }
        }
    }

    //-----------//
    // getGlyphs //
    //-----------//
    @Override
    public Collection<Glyph> getGlyphs ()
    {
        List<Glyph> glyphs = new ArrayList<>();

        for (TreeNode node : getItems()) {
            BeamItem item = (BeamItem) node;
            glyphs.add(item.getGlyph());
        }
        return glyphs;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction maxDistance = new Scale.Fraction(
                0.5,
                "Maximum euclidian distance between glyph center and beam line");

        Scale.Fraction maxGap = new Scale.Fraction(
                0.5,
                "Maximum gap along alignment with beam left or right extremum");

    }
}
