//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A b s t r a c t B e a m I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.math.AreaUtil;

import omr.sheet.beam.BeamGroup;
import omr.sheet.rhythm.Voice;

import omr.sig.BasicImpacts;
import omr.sig.GradeImpacts;

import omr.util.VerticalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class {@code AbstractBeamInter} is the basis for {@link FullBeamInter},
 * {@link BeamHookInter} and {@link SmallBeamInter} classes.
 * <p>
 * The following image shows two beams (a full beam and a beam hook):
 * <p>
 * <img alt="Beam image"
 * src="http://upload.wikimedia.org/wikipedia/commons/thumb/8/8e/Beamed_notes.svg/220px-Beamed_notes.svg.png">
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractBeamInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AbstractBeamInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Median line. */
    private final Line2D median;

    /** Beam height. */
    private final double height;

    /** The containing beam group. */
    private BeamGroup group;

    /** Chords that are linked by this beam. */
    private final List<ChordInter> chords = new ArrayList<ChordInter>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new AbstractBeamInter object.
     *
     * @param glyph   the underlying glyph
     * @param shape   BEAM or BEAM_HOOK
     * @param impacts the grade details
     * @param median  median beam line
     * @param height  beam height
     */
    protected AbstractBeamInter (Glyph glyph,
                                 Shape shape,
                                 GradeImpacts impacts,
                                 Line2D median,
                                 double height)
    {
        super(glyph, null, shape, impacts);
        this.median = median;
        this.height = height;

        setArea(AreaUtil.horizontalParallelogram(median.getP1(), median.getP2(), height));

        // Define precise bounds based on this path
        setBounds(getArea().getBounds());
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

    //----------//
    // addChord //
    //----------//
    public void addChord (ChordInter chord)
    {
        if (!chords.contains(chord)) {
            chords.add(chord);
        }
    }

    //
    //    //----------------//
    //    // determineGroup //
    //    //----------------//
    //    /**
    //     * Determine which BeamGroup this beam is part of.
    //     * The BeamGroup is either reused (if one of its beams has a linked chord
    //     * in common with this beam) or created from scratch otherwise
    //     *
    //     * @param measure containing measure
    //     */
    //    public void determineGroup (Measure measure)
    //    {
    //        // Check if this beam should belong to an existing group
    //        for (BeamGroup grp : measure.getBeamGroups()) {
    //            for (AbstractBeamInter beam : grp.getBeams()) {
    //                for (ChordInter chord : beam.getChords()) {
    //                    if (this.chords.contains(chord)) {
    //                        // We have a chord in common with this beam, so we are in same group
    //                        switchToGroup(grp);
    //                        logger.debug("{} Reused {} for {}", this, grp, this);
    //
    //                        return;
    //                    }
    //                }
    //            }
    //        }
    //
    //        // No compatible group found, let's build a new one
    //        switchToGroup(new BeamGroup(measure));
    //
    //        logger.debug("{} Created new {} for {}", this, getGroup(), this);
    //    }
    //
    //-----------//
    // getBorder //
    //-----------//
    /**
     * Report the beam border line on desired side
     *
     * @param side the desired side
     * @return the beam border line on desired side
     */
    public Line2D getBorder (VerticalSide side)
    {
        final double dy = (side == VerticalSide.TOP) ? (-height / 2) : (height / 2);

        return new Line2D.Double(
                median.getX1(),
                median.getY1() + dy,
                median.getX2(),
                median.getY2() + dy);
    }

    //-----------//
    // getChords //
    //-----------//
    /**
     * Report the chords that are linked by this beam.
     *
     * @return the linked chords
     */
    public List<ChordInter> getChords ()
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

    //-----------//
    // getHeight //
    //-----------//
    /**
     * @return the height
     */
    public double getHeight ()
    {
        return height;
    }

    //-----------//
    // getMedian //
    //-----------//
    /**
     * Report the median line
     *
     * @return the beam median line
     */
    public Line2D getMedian ()
    {
        return median;
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        if (group != null) {
            return group.getVoice();
        }

        return null;
    }

    //--------//
    // isGood //
    //--------//
    @Override
    public boolean isGood ()
    {
        return grade >= 0.35; // TODO: revise this!
    }

    //--------//
    // isHook //
    //--------//
    public boolean isHook ()
    {
        return false;
    }

    //-------------//
    // removeChord //
    //-------------//
    public void removeChord (ChordInter chord)
    {
        chords.remove(chord);
    }

    //----------//
    // setGroup //
    //----------//
    public void setGroup (BeamGroup group)
    {
        this.group = group;
    }

    //---------------//
    // switchToGroup //
    //---------------//
    /**
     * Move this beam to a BeamGroup, by setting the link both ways between this beam
     * and the containing group.
     *
     * @param group the (new) containing beam group
     */
    public void switchToGroup (BeamGroup group)
    {
        logger.debug("Switching {} from {} to {}", this, this.group, group);

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

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends BasicImpacts
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static final String[] NAMES = new String[]{
            "width", "minHgt", "maxHgt", "core", "belt",
            "dist"
        };

        private static final double[] WEIGHTS = new double[]{0.5, 1, 1, 2, 2, 2};

        //~ Constructors ---------------------------------------------------------------------------
        public Impacts (double width,
                        double minHeight,
                        double maxHeight,
                        double core,
                        double belt,
                        double dist)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, width);
            setImpact(1, minHeight);
            setImpact(2, maxHeight);
            setImpact(3, core);
            setImpact(4, belt);
            setImpact(5, dist);
        }

        //~ Methods --------------------------------------------------------------------------------
        public double getDistImpact ()
        {
            return getImpact(3);
        }
    }
}
