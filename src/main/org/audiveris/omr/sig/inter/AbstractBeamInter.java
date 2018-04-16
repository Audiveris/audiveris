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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.beam.BeamGroup;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.BasicImpacts;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.BeamPortion;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.util.HorizontalSide;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.VerticalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

        setAbnormal(true); // No stem linked yet
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        // Check if beam is connected to stems on both ends
        boolean left = false;
        boolean right = false;

        for (Relation rel : sig.getRelations(this, BeamStemRelation.class)) {
            BeamStemRelation bsRel = (BeamStemRelation) rel;
            BeamPortion portion = bsRel.getBeamPortion();

            if (portion == BeamPortion.LEFT) {
                left = true;
            } else if (portion == BeamPortion.RIGHT) {
                right = true;
            }
        }

        setAbnormal(!left || !right);

        return isAbnormal();
    }

    //-----------//
    // checkLink //
    //-----------//
    /**
     * Check if a Beam-Stem link is possible between this beam and the provided stem.
     *
     * @param stem       the provided stem
     * @param headToBeam vertical direction (from head) to beam
     * @param scale      scaling information
     * @return the link if OK, otherwise null
     */
    public Link checkLink (StemInter stem,
                           VerticalSide headToBeam,
                           Scale scale)
    {
        if (isVip() && stem.isVip()) {
            logger.info("VIP checkLink {} & {}", this, stem);
        }

        // Relation beam -> stem (if not yet present)
        BeamStemRelation bRel;
        final int yDir = (headToBeam == VerticalSide.TOP) ? (-1) : 1;
        final Glyph stemGlyph = stem.getGlyph();
        final Line2D beamLimit = getBorder(headToBeam.opposite());
        bRel = new BeamStemRelation();

        // Precise cross point
        Point2D start = stemGlyph.getStartPoint(Orientation.VERTICAL);
        Point2D stop = stemGlyph.getStopPoint(Orientation.VERTICAL);
        Point2D crossPt = LineUtil.intersection(start, stop, beamLimit.getP1(), beamLimit.getP2());

        // Extension point
        bRel.setExtensionPoint(
                new Point2D.Double(crossPt.getX(), crossPt.getY() + (yDir * (getHeight() - 1))));

        // Abscissa -> beamPortion
        // toLeft & toRight are >0 if within beam, <0 otherwise
        double toLeft = crossPt.getX() - beamLimit.getX1();
        double toRight = beamLimit.getX2() - crossPt.getX();
        final double xGap;

        final int maxBeamInDx = scale.toPixels(BeamStemRelation.getXInGapMaximum(manual));

        if (this instanceof BeamInter && (Math.min(toLeft, toRight) > maxBeamInDx)) {
            // It's a beam center connection
            bRel.setBeamPortion(BeamPortion.CENTER);
            xGap = 0;
        } else if (toLeft < toRight) {
            bRel.setBeamPortion(BeamPortion.LEFT);
            xGap = Math.max(0, -toLeft);
        } else {
            bRel.setBeamPortion(BeamPortion.RIGHT);
            xGap = Math.max(0, -toRight);
        }

        // Ordinate
        final double yGap = (yDir > 0) ? Math.max(0, crossPt.getY() - stop.getY())
                : Math.max(0, start.getY() - crossPt.getY());

        bRel.setGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), manual);

        if (bRel.getGrade() >= bRel.getMinGrade()) {
            logger.debug("{} {} {}", this, stem, bRel);

            return new Link(stem, bRel, true);
        } else {
            return null;
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
        List<AbstractChordInter> chords = new ArrayList<AbstractChordInter>();

        for (StemInter stem : getStems()) {
            for (AbstractChordInter chord : stem.getChords()) {
                if (!chords.contains(chord)) {
                    chords.add(chord);
                }
            }
        }

        Collections.sort(chords, Inters.byCenterAbscissa);

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

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system,
                                         boolean doit)
    {
        // Not very optimized!
        List<Inter> systemStems = system.getSig().inters(StemInter.class);
        Collections.sort(systemStems, Inters.byAbscissa);

        Collection<Link> links = lookupLinks(systemStems, system);

        if (doit) {
            for (Link link : links) {
                link.applyTo(this);
            }
        }

        return links;
    }

    //----------//
    // setGlyph //
    //----------//
    @Override
    public void setGlyph (Glyph glyph)
    {
        super.setGlyph(glyph);

        if (area == null) {
            // Case of manual beam: Compute height and median parameters and area
            height = (int) Math.rint(glyph.getMeanThickness(Orientation.HORIZONTAL));
            median = glyph.getLine();

            computeArea();
        }
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

    //-------------//
    // lookupLinks //
    //-------------//
    private Collection<Link> lookupLinks (List<Inter> systemStems,
                                          SystemInfo system)
    {
        if (systemStems.isEmpty()) {
            return Collections.emptySet();
        }

        if (isVip()) {
            logger.info("VIP lookupLinks for {}", this);
        }

        final Line2D top = getBorder(VerticalSide.TOP);
        final Line2D bottom = getBorder(VerticalSide.BOTTOM);
        final Scale scale = system.getSheet().getScale();
        final int xOut = scale.toPixels(BeamStemRelation.getXOutGapMaximum(manual));
        final int xIn = scale.toPixels(BeamStemRelation.getXInGapMaximum(manual));
        final int yGap = scale.toPixels(BeamStemRelation.getYGapMaximum(manual));

        final Map<HorizontalSide, Link> sideLinks = new EnumMap<HorizontalSide, Link>(
                HorizontalSide.class);

        for (HorizontalSide hSide : HorizontalSide.values()) {
            Link bestLink = null;
            double bestGrade = Double.MAX_VALUE;

            final Rectangle luBox = new Rectangle(-1, -1); // "Non-existant" rectangle

            if (hSide == HorizontalSide.LEFT) {
                Point iTop = PointUtil.rounded(top.getP1());
                luBox.add(iTop.x - xOut, iTop.y - yGap);
                luBox.add(iTop.x + xIn, iTop.y - yGap);

                Point iBottom = PointUtil.rounded(bottom.getP1());
                luBox.add(iBottom.x - xOut, iBottom.y + yGap);
                luBox.add(iBottom.x + xIn, iBottom.y + yGap);
            } else {
                Point iTop = PointUtil.rounded(top.getP2());
                luBox.add(iTop.x - xIn, iTop.y - yGap);
                luBox.add(iTop.x + xOut, iTop.y - yGap);

                Point iBottom = PointUtil.rounded(bottom.getP2());
                luBox.add(iBottom.x - xIn, iBottom.y + yGap);
                luBox.add(iBottom.x + xOut, iBottom.y + yGap);
            }

            List<Inter> stems = SIGraph.intersectedInters(systemStems, GeoOrder.NONE, luBox);

            for (Inter inter : stems) {
                StemInter stem = (StemInter) inter;

                for (VerticalSide vSide : VerticalSide.values()) {
                    Link link = checkLink(stem, vSide, scale);

                    if (link != null) {
                        BeamStemRelation rel = (BeamStemRelation) link.relation;

                        if ((bestLink == null) || (rel.getGrade() > bestGrade)) {
                            bestLink = link;
                            bestGrade = rel.getGrade();
                        }
                    }
                }
            }

            if (bestLink != null) {
                sideLinks.put(hSide, bestLink);
            }
        }

        return sideLinks.values();
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
