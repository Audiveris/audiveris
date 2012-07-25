//----------------------------------------------------------------------------//
//                                                                            //
//                                  B e a m                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.Main;

import omr.constant.ConstantSet;

import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.math.BasicLine;
import omr.math.Line;

import omr.score.common.PixelPoint;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.TreeNode;
import omr.util.Vip;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code Beam} represents a beam "line", that may
 * be composed of several BeamItem instances, aligned horizontally one
 * after the other, along the same line.
 * It can degenerate to just a single beam hook.
 *
 * @author Hervé Bitteur
 */
public class Beam
        extends MeasureNode
        implements Comparable<Beam>,
                   Vip
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Beam.class);

    //~ Instance fields --------------------------------------------------------
    /** (Debug) flag this object as VIP */
    private boolean vip;

    /** Id for debug */
    private final int id;

    /** The containing beam group */
    private BeamGroup group;

    /** Items that compose this beam, sorted by abscissa */
    private SortedSet<BeamItem> items = new TreeSet<>();

    /** Sequence of Chords that are linked by this beam, ordered by abscissa */
    private SortedSet<Chord> chords = new TreeSet<>();

    /** Line equation for the beam */
    private Line line;

    /** Left point of beam */
    private PixelPoint left;

    /** Right point of beam */
    private PixelPoint right;

    //~ Constructors -----------------------------------------------------------
    //------//
    // Beam //
    //------//
    /** Creates a new instance of Beam.
     *
     * @param measure the enclosing measure
     */
    public Beam (Measure measure)
    {
        super(measure);

        id = 1 + getChildIndex();

        logger.fine("{0} Created {1}", new Object[]{measure.getContextString(),
                                                    this});
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // populate //
    //----------//
    /**
     * Populate a (or create a brand new) beam with this glyph.
     *
     * @param item    a beam item
     * @param measure the containing measure
     */
    public static void populate (BeamItem item,
                                 Measure measure)
    {
        ///logger.info("Populating " + glyph);
        Beam beam = null;

        // Browse existing beams, to check if this glyph can be appended
        for (TreeNode node : measure.getBeams()) {
            Beam b = (Beam) node;

            if (b.isCompatibleWith(item)) {
                beam = b;

                break;
            }
        }

        // If not, create a brand new beam entity
        if (beam == null) {
            beam = new Beam(measure);
        }

        beam.addItem(item);

        ////glyph.addTranslation(item);
        logger.fine("{0} {1}", new Object[]{beam.getContextString(), beam});
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
        chords.add(chord);
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
            Chord first = chords.first();
            Chord last = chords.last();
            boolean started = false;

            // Add interleaved chords if any, plus relevant chords of the group
            SortedSet<Chord> adds = Chord.lookupInterleavedChords(first, last);
            adds.addAll(group.getChords());

            for (Chord chord : adds) {
                if (chord == first) {
                    started = true;
                }

                if (started) {
                    chords.add(chord);
                    chord.addBeam(this);
                }

                if (chord == last) {
                    break;
                }
            }
        }
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Implement the order between two beams (of the same BeamGroup).
     * We use the order along the first common chord, starting from chord tail.
     * Note that, apart from the trivial case where a beam is compared to
     * itself,
     * two beams of the same group cannot be equal.
     *
     * @param other the other beam to be compared with
     * @return -1, 0, +1 according to the comparison result
     */
    @Override
    public int compareTo (Beam other)
    {
        // Process trivial case
        if (this == other) {
            return 0;
        }

        // Find a common chord, and use reverse order from head location
        for (Chord chord : chords) {
            if (other.chords.contains(chord)) {
                int x = chord.getStem().getLocation().x;
                int y = getLine().yAtX(x);
                int yOther = other.getLine().yAtX(x);
                int yHead = chord.getHeadLocation().y;

                int result = Integer.signum(
                        Math.abs(yHead - yOther) - Math.abs(yHead - y));

                if (result == 0) {
                    // This should not happen
                    //                    logger.warning(
                    //                        other.getContextString() + " equality between " +
                    //                        this.toLongString() + " and " + other.toLongString());
                    //                    logger.warning(
                    //                        "Beam comparison data " + "x=" + x + " y=" + y +
                    //                        " yOther=" + yOther + " yHead=" + yHead);
                    addError(chord.getStem(), "Weird beam configuration");
                }

                return result;
            }
        }

        // This case corresponds to two beam hooks, use abscissa to order them
        return Integer.signum(this.left.x - other.left.x);
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
                        switchGroup(group);
                        logger.fine("{0} Reused {1} for {2}",
                                    new Object[]{getContextString(), group, this});

                        return;
                    }
                }
            }
        }

        // No compatible group found, let's build a new one
        switchGroup(new BeamGroup(getMeasure()));

        logger.fine("{0} Created new {1} for {2}",
                    new Object[]{getContextString(), getGroup(), this});
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
     * Report the sequence of chords that are linked by this beam.
     *
     * @return the sorted set of linked chords
     */
    public SortedSet<Chord> getChords ()
    {
        return chords;
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
    public SortedSet<BeamItem> getItems ()
    {
        return items;
    }

    //----------//
    // getLevel //
    //----------//
    /**
     * Report the level of this beam within the containing BeamGroup,
     * starting from 1.
     *
     * @return the beam level in its group
     */
    public int getLevel ()
    {
        return getGroup().getLevel(this);
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
        if ((line == null) && !items.isEmpty()) {
            line = new BasicLine();

            // Take left side of first item, and right side of last item
            left = getPoint(LEFT);
            line.includePoint(left.x, left.y);
            right = getPoint(RIGHT);
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
     * @return the PixelPoint coordinates of the point on desired side
     */
    public PixelPoint getPoint (HorizontalSide side)
    {
        if (side == LEFT) {
            return items.first().getPoint(LEFT);
        } else {
            return items.last().getPoint(RIGHT);
        }
    }

    //--------//
    // isHook //
    //--------//
    public boolean isHook ()
    {
        return items.first().isHook();
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
        for (BeamItem item : items) {
            //////////////////////////////////////////////////////////////////
            // TODO for a beam (non hook) both stems must exist and be linked
            //////////////////////////////////////////////////////////////////
            linkChordsOnStems(item);

            // Include other stems in the middle
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

    //-------------//
    // switchGroup //
    //-------------//
    /**
     * Switch this beam to a BeamGroup, by setting the link both ways
     * between this beam and the containing group.
     *
     * @param group the (new) containing beam group
     */
    public void switchGroup (BeamGroup group)
    {
        logger.fine("Switching {0} from {1} to {2}",
                    new Object[]{this, this.group, group});

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

    //--------------//
    // toLongString //
    //--------------//
    /**
     * A rather lengthy version of toString().
     *
     * @return a complete description string
     */
    public String toLongString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Beam");

        sb.append(" #").append(id);

        if (getGroup() != null) {
            sb.append(" lv=").append(getLevel());
        }

        sb.append(" left=").append(getPoint(LEFT));

        sb.append(" right=").append(getPoint(LEFT));

        sb.append(BeamItem.toString(items));
        sb.append("}");

        return sb.toString();
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

            if (getGroup() != null) {
                sb.append(" lv=").append(getLevel());
            }

            sb.append(BeamItem.toString(items));
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
        getLine();
        setCenter(
                new PixelPoint((left.x + right.x) / 2, (left.y + right.y) / 2));
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
        left = null;
        right = null;
    }

    //---------//
    // addItem //
    //---------//
    /**
     * Insert a (BEAM/BEAM_HOOK) item as a component of this beam.
     *
     * @param item the beam item to insert
     */
    private void addItem (BeamItem item)
    {
        items.add(item);
        reset();

        if (item.isVip()) {
            setVip();
        }

        if (isVip() || logger.isFineEnabled()) {
            logger.info("{0} Added {1} to {2}", new Object[]{getMeasure().
                        getContextString(), item, this});
        }
    }

    //------------------//
    // isCompatibleWith //
    //------------------//
    /**
     * Check compatibility of a given BeamItem with this Beam instance.
     * We use alignment and distance criterias.
     *
     * @param item the beam item to check for compatibility
     * @return true if compatible
     */
    private boolean isCompatibleWith (BeamItem item)
    {
        boolean logging = isVip() || item.isVip() || logger.isFineEnabled();

        if (logging) {
            logger.info("Check beam item {0} with {1}", item, this);
        }

        // Check alignment, using distance to line
        PixelPoint gsp = item.getCenter();
        double dy = getScale().pixelsToFrac(getLine().distanceOf(gsp.x, gsp.y));

        if (logging) {
            logger.info("dy={0} vs {1}", (float) Math.abs(dy),
                        constants.maxDistance.getValue());
        }

        if (Math.abs(dy) > constants.maxDistance.getValue()) {
            return false;
        }

        // Check distance along the same alignment
        for (HorizontalSide side : HorizontalSide.values()) {
            double dx = getScale().pixelsToFrac(
                    item.getPoint(side).distance(
                    getPoint((side == LEFT) ? RIGHT : LEFT)));

            if (logging) {
                logger.info("dx={0} vs {1}", (float) dx,
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
                        chords.add(chord);
                        chord.addBeam(this);
                    }
                } else {
                    addError("Beam with no chord on " + side + " stem");


                }
            }
        }
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
