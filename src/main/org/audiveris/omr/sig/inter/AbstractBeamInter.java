//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A b s t r a c t B e a m I n t e r                               //
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

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.sheet.beam.BeamGroup;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.BasicImpacts;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.VerticalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Abstract class {@code AbstractBeamInter} is the basis for {@link BeamInter},
 * {@link BeamHookInter} and {@link SmallBeamInter} classes.
 * <p>
 * The following image shows two beams - a (full) beam and a beam hook:
 * <p>
 * <img alt="Beam image"
 * src="http://upload.wikimedia.org/wikipedia/commons/thumb/8/8e/Beamed_notes.svg/220px-Beamed_notes.svg.png">
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractBeamInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            AbstractBeamInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Beam height. */
    @XmlAttribute
    @XmlJavaTypeAdapter(type = double.class, value = Jaxb.Double1Adapter.class)
    protected double height;

    /** Median line. */
    @XmlElement
    protected Line2D median;

    // Transient data
    //---------------
    //
    /** Chords that are linked by this beam. */
    private List<AbstractChordInter> chords;

    /** The containing beam group. */
    private BeamGroup group;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new AbstractBeamInter object.
     * Note there is no underlying glyph, cleaning will be based on beam area.
     *
     * @param shape   BEAM or BEAM_HOOK
     * @param impacts the grade details
     * @param median  median beam line
     * @param height  beam height
     */
    protected AbstractBeamInter (Shape shape,
                                 GradeImpacts impacts,
                                 Line2D median,
                                 double height)
    {
        super(null, null, shape, impacts);
        this.median = median;
        this.height = height;

        if (median != null) {
            computeArea();
        }
    }

    /**
     * Creates a new AbstractBeamInter <b>ghost</b> object.
     * Median and height must be assigned later
     *
     * @param shape BEAM or BEAM_HOOK
     * @param grade the grade
     */
    protected AbstractBeamInter (Shape shape,
                                 double grade)
    {
        super(null, null, shape, grade);

        if (median != null) {
            computeArea();
        }
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

        // Update group?
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
    //    private void determineGroup ()
    //    {
    //        // Check if this beam should belong to an existing group
    //        Point center = getCenter();
    //        MeasureStack stack = sig.getSystem().getMeasureStackAt(center);
    //        stack.getMeasureAt(part)
    //        for (BeamGroup grp : measure.getBeamGroups()) {
    //            for (AbstractBeamInter beam : grp.getBeams()) {
    //                for (AbstractChordInter chord : beam.getChords()) {
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
    public List<AbstractChordInter> getChords ()
    {
        if (chords == null) {
            chords = new ArrayList<AbstractChordInter>();

            for (StemInter stem : getStems()) {
                for (Relation hs : sig.getRelations(stem, HeadStemRelation.class)) {
                    HeadInter head = (HeadInter) sig.getOppositeInter(stem, hs);
                    AbstractChordInter chord = head.getChord();

                    if (chord != null) {
                        chords.add(chord);
                    }
                }
            }
        }

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
    // getStems //
    //----------//
    /**
     * Report the stems connected to this beam.
     *
     * @return the set of connected stems, perhaps empty
     */
    public Set<StemInter> getStems ()
    {
        Set<StemInter> stems = new LinkedHashSet<StemInter>();

        for (Relation bs : sig.getRelations(this, BeamStemRelation.class)) {
            StemInter stem = (StemInter) sig.getOppositeInter(this, bs);
            stems.add(stem);
        }

        return stems;
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

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        chords = null;
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

    //--------//
    // remove //
    //--------//
    @Override
    public void remove (boolean extensive)
    {
        if (group != null) {
            group.removeBeam(this);
        }

        for (AbstractChordInter chord : getChords()) {
            chord.invalidateCache();
        }

        super.remove(extensive);
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

    //-------------//
    // computeArea //
    //-------------//
    protected void computeArea ()
    {
        setArea(AreaUtil.horizontalParallelogram(median.getP1(), median.getP2(), height));

        // Define precise bounds based on this path
        setBounds(getArea().getBounds());
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called after all the properties (except IDREF) are unmarshalled for this object,
     * but before this object is set to the parent object.
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        if (median != null) {
            computeArea();
        }
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
            "wdth", "minH", "maxH", "core", "belt", "jit"
        };

        private static final int DIST_INDEX = 5;

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
            return getImpact(DIST_INDEX);
        }
    }
}
