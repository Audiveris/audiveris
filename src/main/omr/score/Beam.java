//----------------------------------------------------------------------------//
//                                                                            //
//                                  B e a m                                   //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;

import omr.math.BasicLine;
import omr.math.Line;

import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;

import omr.stick.Stick;

import omr.util.Dumper;
import omr.util.Logger;
import omr.util.TreeNode;
import static java.lang.Math.*;
import java.util.*;

/**
 * Class <code>Beam</code> represents a beam hook or a beam, that may be
 * composed of several beam glyphs, aligned one after the other, along the same
 * line.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Beam
    extends MeasureNode
    implements Comparable<Beam>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Beam.class);

    //~ Instance fields --------------------------------------------------------

    /** Id for debug */
    private final int id;

    /** The containing beam group */
    private BeamGroup group;

    /** Glyphs that compose this beam, ordered by abscissa */
    private SortedSet<Glyph> glyphs = new TreeSet<Glyph>();

    /** Sequence of Chords that are linked by this beam, ordered by abscissa */
    private SortedSet<Chord> chords = new TreeSet<Chord>();

    /** Line equation for the beam */
    private Line line;

    /** Left point of beam */
    private SystemPoint left;

    /** Right point of beam */
    private SystemPoint right;

    //~ Constructors -----------------------------------------------------------

    //------//
    // Beam //
    //------//
    /** Creates a new instance of Beam */
    public Beam (Measure measure)
    {
        super(measure);
        id = measure.getBeams()
                    .indexOf(this) + 1;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getChords //
    //-----------//
    /**
     * Report the sequence of chords that are linked by this beam
     *
     * @return the sorted set of linked chords
     */
    public SortedSet<Chord> getChords ()
    {
        return chords;
    }

    //-----------//
    // getGlyphs //
    //-----------//
    /**
     * Report the ordered sequence of glyphs (one or several glyphs of BEAM
     * shape, or one glyph of BEAM_HOOK shape) that compose this beam
     *
     * @return the ordered set ofbeam glyphs
     */
    public SortedSet<Glyph> getGlyphs ()
    {
        return glyphs;
    }

    //----------//
    // setGroup //
    //----------//
    /**
     * Assign this beam to a BeamGroup, by setting the link both ways between
     * this beam and the containing group.
     *
     * @param group the (new) containing beam group
     */
    public void setGroup (BeamGroup group)
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                "Assigning " + this + " from " + this.group + " to " + group);
        }

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
    // getGroup //
    //----------//
    /**
     * Report the containing group
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
     * Report the unique id of the beam within its containing measure
     *
     * @return the beam id, starting from 1
     */
    public int getId ()
    {
        return id;
    }

    //---------//
    // getLeft //
    //---------//
    /**
     * Report the point that define the left edge of the beam
     *
     * @return the SystemPoint coordinates of the left point
     */
    public SystemPoint getLeft ()
    {
        getLine();

        return left;
    }

    //----------//
    // getLevel //
    //----------//
    /**
     * Report the level of this beam within the containing BeamGroup, starting
     * from 1
     *
     * @return the beam level in its group
     */
    public int getLevel ()
    {
        return getGroup()
                   .getLevel(this);
    }

    //---------//
    // getLine //
    //---------//
    /**
     * Report the line equation defined by the beam
     *
     * @return the line equation
     */
    public Line getLine ()
    {
        if ((line == null) && (glyphs.size() > 0)) {
            line = new BasicLine();

            // Take left side of first glyph, and right side of last glyph
            left = getLeftPoint(glyphs.first());
            line.includePoint(left.x, left.y);
            right = getRightPoint(glyphs.last());
            line.includePoint(right.x, right.y);
        }

        return line;
    }

    //----------//
    // getRight //
    //----------//
    /**
     * Report the point that define the right edge of the beam
     *
     * @return the SystemPoint coordinates of the right point
     */
    public SystemPoint getRight ()
    {
        getLine();

        return right;
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
     * Insert a chord linked by this beam
     *
     * @param chord the linked chord
     */
    public void addChord (Chord chord)
    {
        chords.add(chord);
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Implement the order between two beams (of the same BeamGroup). We use the
     * order along the first common chord, starting from chord tail. Note that,
     * apart from the trivial case where a beam is compared to itself, two beams
     * of the same group cannot be equal.
     *
     * @param other the other beam to be compared with
     * @return -1, 0, +1 according to the comparison result
     */
    public int compareTo (Beam other)
    {
        // Process trivial case
        if (this == other) {
            return 0;
        }

        // Find a common chord, and use reverse order from head location
        for (Chord chord : chords) {
            if (other.chords.contains(chord)) {
                int x = getMeasure()
                            .getSystem()
                            .toSystemPoint(chord.getStem().getCenter()).x;
                int y = getLine()
                            .yAt(x);
                int yOther = other.getLine()
                                  .yAt(x);
                int yHead = chord.getHeadLocation().y;

                int result = Integer.signum(
                    Math.abs(yHead - yOther) - Math.abs(yHead - y));

                if (result == 0) {
                    // This should not happen
                    logger.warning(
                        "equality between " + this.toLongString() + " and " +
                        other.toLongString());
                    logger.warning(
                        "x=" + x + " y=" + y + " yOther=" + yOther + " yHead=" +
                        yHead);
                    Dumper.dump(this, "this");
                    Dumper.dump(other, "other");
                }

                return result;
            }
        }

        // Should not happen, but let's keep the compiler happy
        logger.warning(
            getContextString() + " Comparing 2 beams with no common chord : " +
            this.toLongString() + " & " + other.toLongString());

        return 0;
    }

    //----------------//
    // determineGroup //
    //----------------//
    /**
     * Determine which BeamGroup this beam is part of. The BeamGroup is either
     * reused (if one of its beams has a linked chord in common with this beam)
     * or created from scratch otherwise
     */
    public void determineGroup ()
    {
        // Check if this beam should belong to an existing group
        for (BeamGroup group : getMeasure()
                                   .getBeamGroups()) {
            for (Beam beam : group.getBeams()) {
                for (Chord chord : beam.getChords()) {
                    if (this.chords.contains(chord)) {
                        // We have a chord in common with this beam, so we are
                        // part of the same group
                        setGroup(group);

                        if (logger.isFineEnabled()) {
                            logger.fine(
                                getContextString() + " Reused " + group +
                                " for " + this + " stick #" +
                                glyphs.first().getId());
                        }

                        return;
                    }
                }
            }
        }

        // No compatible group found, let's build a new one
        setGroup(new BeamGroup(getMeasure()));

        if (logger.isFineEnabled()) {
            logger.fine(
                getContextString() + " Created new " + getGroup() + " for " +
                this + " stick #" + glyphs.first().getId());
        }
    }

    //------//
    // dump //
    //------//
    /**
     * Utility method for easy dumping of the beam entity
     */
    public void dump ()
    {
        getLine();
        Dumper.dump(this);
    }

    //------------//
    // linkChords //
    //------------//
    /**
     * Assign the both-way link between this beam and the chords connected by the
     * beam
     */
    public void linkChords ()
    {
        for (Glyph glyph : getGlyphs()) {
            linkChordsOnStem("left", glyph.getLeftStem(), glyph);
            linkChordsOnStem("right", glyph.getRightStem(), glyph);
        }
    }

    //----------//
    // populate //
    //----------//
    /**
     * Populate a (or create a brand new) beam with this glyph
     *
     * @param glyph a beam glyph
     * @param measure the containing measure
     */
    public static void populate (Glyph   glyph,
                                 Measure measure)
    {
        ///logger.info("Populating " + glyph);
        Beam beam = null;

        // Browse existing beams, to check if this glyph can be appended
        for (TreeNode node : measure.getBeams()) {
            Beam b = (Beam) node;

            if (b.isCompatibleWith(glyph)) {
                beam = b;

                break;
            }
        }

        // If not, create a brand new beam entity
        if (beam == null) {
            beam = new Beam(measure);
        }

        beam.addGlyph(glyph);

        if (logger.isFineEnabled()) {
            logger.fine(beam.getContextString() + " " + beam);
        }
    }

    //--------------//
    // toLongString //
    //--------------//
    /**
     * A rather lengthy version of toString()
     *
     * @return a complete description string
     */
    public String toLongString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Beam");

        sb.append(" #")
          .append(id);

        if (getGroup() != null) {
            sb.append(" lv=")
              .append(getLevel());
        }

        sb.append(" left=")
          .append(getLeft());

        sb.append(" right=")
          .append(getRight());

        sb.append(Glyph.toString(glyphs));
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

        sb.append(" #")
          .append(id);

        if (getGroup() != null) {
            sb.append(" lv=")
              .append(getLevel());
        }

        sb.append(Glyph.toString(glyphs));
        sb.append("}");

        return sb.toString();
    }

    //------------------//
    // isCompatibleWith //
    //------------------//
    /**
     * Check compatibility of a given BEAM/BEAM_HOOK glyph with this beam. We
     * use alignment and distance criterias.
     *
     * @param glyph the glyph to check for compatibility
     * @return true if compatible
     */
    private boolean isCompatibleWith (Glyph glyph)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Check glyph " + glyph.getId() + " with " + this);
        }

        // Check alignment
        SystemPoint gsp = computeGlyphCenter(glyph);
        double      dist = getLine()
                               .distanceOf(gsp.x, gsp.y);
        double      maxDistance = getScale()
                                      .toUnits(constants.maxDistance);

        if (logger.isFineEnabled()) {
            logger.fine("maxDistance=" + maxDistance + " dist=" + dist);
        }

        if (abs(dist) > maxDistance) {
            return false;
        }

        // Check distance along the same alignment
        double maxGap = getScale()
                            .toUnits(constants.maxGap);

        if (logger.isFineEnabled()) {
            logger.fine(
                "maxGap=" + maxGap + " leftGap=" +
                getRightPoint(glyph).distance(getLeft()) + " rightGap=" +
                getLeftPoint(glyph).distance(getRight()));
        }

        if ((getRightPoint(glyph)
                 .distance(getLeft()) <= maxGap) ||
            (getLeftPoint(glyph)
                 .distance(getRight()) <= maxGap)) {
            return true;
        }

        return false;
    }

    //--------------//
    // getLeftPoint //
    //--------------//
    /**
     * Report the left point of a (BEAM/BEAM_HOOK) glyph
     *
     * @param glyph the given glyph
     * @return the glyph left point
     */
    private SystemPoint getLeftPoint (Glyph glyph)
    {
        Stick  stick = (Stick) glyph;
        int    lx = stick.getFirstPos();
        System system = getMeasure()
                            .getSystem();

        return new SystemPoint(
            getScale().pixelsToUnits(lx) - system.getTopLeft().x,
            (int) rint(
                getScale().pixelsToUnitsDouble(
                    stick.getLine().xAt((double) lx)) - system.getTopLeft().y));
    }

    //---------------//
    // getRightPoint //
    //---------------//
    /**
     * Report the right point of a (BEAM/BEAM_HOOK) glyph
     *
     * @param glyph the given glyph
     * @return the glyph right point
     */
    private SystemPoint getRightPoint (Glyph glyph)
    {
        Stick  stick = (Stick) glyph;
        int    rx = stick.getLastPos();
        System system = getMeasure()
                            .getSystem();

        return new SystemPoint(
            getScale().pixelsToUnits(rx) - system.getTopLeft().x,
            (int) rint(
                getScale().pixelsToUnitsDouble(
                    stick.getLine().xAt((double) rx)) - system.getTopLeft().y));
    }

    //----------//
    // addGlyph //
    //----------//
    /**
     * Insert a (BEAM/BEAM_HOOK) glyph as a component of this beam
     *
     * @param glyph the glyph to insert
     */
    private void addGlyph (Glyph glyph)
    {
        glyphs.add(glyph);
        reset();
    }

    //------------------//
    // linkChordsOnStem //
    //------------------//
    private void linkChordsOnStem (String side,
                                   Glyph  stem,
                                   Glyph  glyph)
    {
        if (stem != null) {
            List<Chord> sideChords = Chord.getStemChords(getMeasure(), stem);

            if (sideChords.size() > 0) {
                for (Chord chord : sideChords) {
                    chords.add(chord);
                    chord.addBeam(this);
                }
            } else {
                logger.warning(
                    getContextString() + " Beam glyph " + glyph.getId() +
                    " with no chord on " + side + " stem");
            }
        }
    }

    //-------//
    // reset //
    //-------//
    /**
     * Invalidate cached data, to force its recomputation when needed
     */
    private void reset ()
    {
        line = null;
        left = null;
        right = null;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /**
         * Maximum euclidian distance between glyph center and beam line
         */
        Scale.Fraction maxDistance = new Scale.Fraction(
            0.5,
            "Maximum euclidian distance between glyph center and beam line");

        /**
         * Maximum gap along alignment with beam left or right extremum
         */
        Scale.Fraction maxGap = new Scale.Fraction(
            0.5,
            "Maximum gap along alignment with beam left or right extremum");
    }
}
