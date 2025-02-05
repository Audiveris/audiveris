//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A b s t r a c t B e a m I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Scale.Fraction;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.Versions;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sheet.stem.BeamLinker;
import org.audiveris.omr.sheet.ui.ObjectUIModel;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.BeamPortion;
import org.audiveris.omr.sig.relation.BeamRestRelation;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.sig.ui.LinkTask;
import org.audiveris.omr.sig.ui.RemovalTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.sig.ui.UnlinkTask;
import org.audiveris.omr.ui.symbol.BeamSymbol;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;
import org.audiveris.omr.util.Version;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.BOTTOM;
import static org.audiveris.omr.util.VerticalSide.TOP;
import org.audiveris.omr.util.WrappedBoolean;
import org.audiveris.omr.util.Wrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Abstract class <code>AbstractBeamInter</code> is the basis for {@link BeamInter},
 * {@link BeamHookInter} and {@link SmallBeamInter} classes.
 * <p>
 * The following image shows two beams - a (full) beam and a beam hook,
 * gathered in the same beam group:
 * <p>
 * <img alt="Beam image"
 * src=
 * "http://upload.wikimedia.org/wikipedia/commons/thumb/8/8e/Beamed_notes.svg/220px-Beamed_notes.svg.png">
 * <p>
 * NOTA: A beam (and thus a beam group) may span several measures as shown below:
 * <p>
 * <img src="doc-files/BeamAcrossMeasureBreak.png" alt="Beam across measure break">
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractBeamInter
        extends AbstractHorizontalInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AbstractBeamInter.class);

    //~ Instance fields ----------------------------------------------------------------------------

    // Transient data
    //---------------

    /** Beam-Stem linker. */
    private BeamLinker linker;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new AbstractBeamInter <b>ghost</b> object.
     * Median and height must be assigned later
     *
     * @param shape BEAM or BEAM_HOOK or BEAM_SMALL
     * @param grade the grade
     */
    protected AbstractBeamInter (Shape shape,
                                 Double grade)
    {
        super(shape, grade);
    }

    /**
     * Creates a new AbstractBeamInter object.
     * Note there is no underlying glyph, cleaning will be based on beam area.
     *
     * @param shape  BEAM or BEAM_HOOK or BEAM_SMALL or MULTIPLE_REST
     * @param grade  evaluated grade
     * @param median median beam line
     * @param height beam height
     */
    protected AbstractBeamInter (Shape shape,
                                 Double grade,
                                 Line2D median,
                                 double height)
    {
        super(shape, grade, median, height);
    }

    /**
     * Creates a new AbstractBeamInter object.
     * Note there is no underlying glyph, cleaning will be based on beam area.
     *
     * @param shape   BEAM or BEAM_HOOK or BEAM_SMALL
     * @param impacts the grade details
     * @param median  median beam line
     * @param height  beam height
     */
    protected AbstractBeamInter (Shape shape,
                                 GradeImpacts impacts,
                                 Line2D median,
                                 double height)
    {
        super(shape, impacts, median, height);
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
    /**
     * Check if this beam is connected to a stem or a rest on each end.
     *
     * @return true if abnormal
     */
    @Override
    public boolean checkAbnormal ()
    {
        boolean left = false;
        boolean right = false;

        for (Relation rel : sig.getRelations(
                this,
                BeamStemRelation.class,
                BeamRestRelation.class)) {
            final BeamPortion portion;

            if (rel instanceof BeamStemRelation) {
                final BeamStemRelation bsRel = (BeamStemRelation) rel;
                portion = bsRel.getBeamPortion();
            } else {
                final BeamRestRelation brRel = (BeamRestRelation) rel;
                portion = brRel.getBeamPortion();
            }

            if (portion == BeamPortion.LEFT) {
                left = true;
            } else if (portion == BeamPortion.RIGHT) {
                right = true;
            }
        }

        setAbnormal(!left || !right);

        return isAbnormal();
    }

    //------------//
    // deriveFrom //
    //------------//
    @Override
    public boolean deriveFrom (ShapeSymbol symbol,
                               Sheet sheet,
                               MusicFont font,
                               Point dropLocation)
    {
        /*
         * A beam (or a beam hook) can have its left and/or right sides snapped to existing stems,
         * which results in beam width which can increase/decrease according to the stems nearby.
         * <p>
         * Short beams (like beam hooks) can have their width decrease so much, that both their
         * left side and their right side may get snapped to the SAME stem, resulting in a width
         * equal to zero.
         * Such a zero-wide beam gets invisible, and if the end-user releases the mouse at this
         * moment, a very weird beam gets created.
         * To cope with this possibility, we have to make sure that the beam width never gets
         * lower than a minimum.
         */
        final BeamSymbol beamSymbol = (BeamSymbol) symbol;
        final Model model = beamSymbol.getModel(font, dropLocation);
        median = new Line2D.Double(model.p1, model.p2);
        height = model.thickness;

        computeArea();

        if (staff != null) {
            final List<Inter> systemStems = staff.getSystem().getSig().inters(StemInter.class);
            Collections.sort(systemStems, Inters.byAbscissa);

            // Snap sides?
            Double left = getSnapAbscissa(LEFT, systemStems);
            Double right = getSnapAbscissa(RIGHT, systemStems);

            if (left != null && right != null) {
                // Both sides get snapped, let's avoid beam collapse
                final double width = right - left;
                final int minWidth = sheet.getScale().toPixels(constants.minBeamWidth);

                if (width < minWidth) {
                    // Choose stem side
                    if (dropLocation.x > 0.5 * (left + right)) {
                        right = left + minWidth;
                    } else {
                        left = right - minWidth;
                    }
                }
            }

            if (left != null) {
                model.p1.setLocation(left, model.p1.getY());
            }

            if (right != null) {
                model.p2.setLocation(right, model.p2.getY());
            }

            if (left != null || right != null) {
                median.setLine(model.p1, model.p2);
                computeArea();
                dropLocation.setLocation(PointUtil.middle(median));
            }
        }

        return true;
    }

    //-----------//
    // getChords //
    //-----------//
    /**
     * Report the chords (head-chords and interleaved rest-chords) that are linked by this beam.
     *
     * @return the linked chords (heads and rests) ordered by center abscissa
     */
    public List<AbstractChordInter> getChords ()
    {
        final Set<AbstractChordInter> chords = new LinkedHashSet<>();

        // Linked head-chords
        for (StemInter stem : getStems()) {
            chords.addAll(stem.getChords());
        }

        // Linked rest-chords
        for (Relation rel : sig.getRelations(this, BeamRestRelation.class)) {
            final Inter rest = sig.getOppositeInter(this, rel);
            chords.add((AbstractChordInter) rest.getEnsemble());
        }

        final List<AbstractChordInter> chordList = new ArrayList<>(chords);
        Collections.sort(chordList, Inters.byCenterAbscissa);

        return chordList;
    }

    //------------------//
    // getCompetingHook //
    //------------------//
    /**
     * Report the competing beam hook, if any, which shares the same glyph as this beam.
     *
     * @return the competing hook, or null if none
     */
    public BeamHookInter getCompetingHook ()
    {
        for (Inter opposite : sig.getCompetingInters(this)) {
            if ((opposite.getShape() == Shape.BEAM_HOOK) && (opposite.getGlyph() == getGlyph())) {
                return (BeamHookInter) opposite;
            }
        }

        return null;
    }

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new Editor(this);
    }

    //----------//
    // getGroup //
    //----------//
    /**
     * Report the containing group.
     *
     * @return the containing group, if already set, or null
     */
    public BeamGroupInter getGroup ()
    {
        return (BeamGroupInter) getEnsemble();
    }

    //----------//
    // getHeads //
    //----------//
    /**
     * Report all heads connected to this beam via some stem.
     * <p>
     * This does not include embedded rests if any.
     *
     * @return the set of connected heads
     */
    public Set<HeadInter> getHeads ()
    {
        final Set<HeadInter> beamHeads = new LinkedHashSet<>();

        for (StemInter stem : getStems()) {
            beamHeads.addAll(stem.getHeads());
        }

        return beamHeads;
    }

    //---------------//
    // getHeadChords //
    //---------------//
    /**
     * Report just the head chords (no interleaved rest-chords) that are linked by this beam.
     *
     * @return the linked head chords (no rests) ordered by center abscissa
     */
    public List<AbstractChordInter> getHeadChords ()
    {
        final Set<AbstractChordInter> headChords = new LinkedHashSet<>();

        // Linked head-chords
        for (StemInter stem : getStems()) {
            headChords.addAll(stem.getChords());
        }

        final List<AbstractChordInter> chordList = new ArrayList<>(headChords);
        Collections.sort(chordList, Inters.byCenterAbscissa);

        return chordList;
    }

    //-----------//
    // getLinker //
    //-----------//
    /**
     * Report the dedicated beam-stem linker.
     *
     * @return the linker
     */
    public BeamLinker getLinker ()
    {
        return linker;
    }

    /**
     * Define lookup area around the beam for potential stems
     *
     * @return the look up area
     */
    private Area getLookupArea (Scale scale)
    {
        final Line2D top = getBorder(TOP);
        final Line2D bottom = getBorder(BOTTOM);
        final int xOut = scale.toPixels(BeamStemRelation.getXOutGapMaximum(getProfile()));
        final int yGap = scale.toPixels(BeamStemRelation.getYGapMaximum(getProfile()));

        final Path2D lu = new Path2D.Double();
        double xMin = top.getX1() - xOut;
        double xMax = top.getX2() + xOut;
        Point2D topLeft = LineUtil.intersectionAtX(top, xMin);
        lu.moveTo(topLeft.getX(), topLeft.getY() - yGap);

        Point2D topRight = LineUtil.intersectionAtX(top, xMax);
        lu.lineTo(topRight.getX(), topRight.getY() - yGap);

        Point2D bottomRight = LineUtil.intersectionAtX(bottom, xMax);
        lu.lineTo(bottomRight.getX(), bottomRight.getY() + yGap);

        Point2D bottomLeft = LineUtil.intersectionAtX(bottom, xMin);
        lu.lineTo(bottomLeft.getX(), bottomLeft.getY() + yGap);
        lu.closePath();

        return new Area(lu);
    }

    //-----------------//
    // getSnapAbscissa //
    //-----------------//
    /**
     * Report the theoretical abscissa of the provided beam side when correctly aligned
     * with a suitable stem.
     * <p>
     * Required properties: staff or sig, median, height
     *
     * @param side        the desired horizontal side
     * @param systemStems all stems in containing system
     * @return the proper abscissa if any, null otherwise
     */
    private Double getSnapAbscissa (HorizontalSide side,
                                    List<Inter> systemStems)
    {
        final SystemInfo system;

        if (sig != null) {
            system = sig.getSystem();
        } else if (staff != null) {
            system = staff.getSystem();
        } else {
            logger.warn("No system nor staff for {}", this);

            return null;
        }

        final int profile = Math.max(getProfile(), system.getProfile());
        final Link link = lookupSideLink(systemStems, system, side, profile);

        if (link != null) {
            final StemInter stem = (StemInter) link.partner;
            final Point2D beamEnd = (side == LEFT) ? median.getP1() : median.getP2();

            return LineUtil.xAtY(stem.getMedian(), beamEnd.getY());
        }

        return null;
    }

    //-----------//
    // getStemOn //
    //-----------//
    /**
     * Report the stem, if any, connected on desired beam portion (LEFT,CENTER or RIGHT).
     * <ul>
     * <li>For LEFT and for RIGHT, there can be at most one stem.
     * <li>For CENTER, there can be from 0 to N stems, so only the first one found is returned.
     * </ul>
     *
     * @param portion provided portion
     * @return the connected stem or null
     */
    public StemInter getStemOn (BeamPortion portion)
    {
        if (isVip()) {
            logger.info("VIP getStemOn for {} on {}", this, portion);
        }

        for (Relation rel : sig.getRelations(this, BeamStemRelation.class)) {
            BeamStemRelation bsRel = (BeamStemRelation) rel;
            BeamPortion p = bsRel.getBeamPortion();

            if (p == portion) {
                return (StemInter) sig.getOppositeInter(this, rel);
            }
        }

        return null;
    }

    //----------//
    // getStems //
    //----------//
    /**
     * Report the stems linked to this beam.
     *
     * @return the set of linked stems, perhaps empty
     */
    public Set<StemInter> getStems ()
    {
        final Set<StemInter> stems = new LinkedHashSet<>();

        for (Relation bs : sig.getRelations(this, BeamStemRelation.class)) {
            StemInter stem = (StemInter) sig.getOppositeInter(this, bs);
            stems.add(stem);
        }

        return stems;
    }

    //------------------//
    // getConcreteStems //
    //------------------//
    /**
     * Report the stems physically connected to this beam.
     *
     * @return the set of physically connected stems, perhaps empty
     */
    public Set<StemInter> getConcreteStems ()
    {
        final Scale scale = sig.getSystem().getSheet().getScale();
        final int maxGap = scale.toPixels(constants.maxBeamStemYGap);
        final Set<StemInter> concreteStems = new LinkedHashSet<>();
        final Set<StemInter> stems = getStems();

        for (StemInter stem : stems) {
            final double gap = getStemVerticalGap(stem);
            if (gap <= maxGap) {
                concreteStems.add(stem);
            }
        }

        return concreteStems;
    }

    //--------------------//
    // getStemVerticalGap //
    //--------------------//
    /**
     * Report the vertical gap between this beam and the provided stem.
     *
     * @param stem the stem to check
     * @return the vertical distance, perhaps 0, between stem and the nearest beam border
     */
    public double getStemVerticalGap (StemInter stem)
    {
        final Line2D stemLine = stem.getMedian();
        final double beamMiddle = LineUtil.intersection(stemLine, median).getY();

        if (stemLine.getY1() <= beamMiddle) {
            // Stem is above or across beam
            final double beamTop = LineUtil.intersection(stemLine, getBorder(TOP)).getY();
            return Math.max(0, beamTop - stemLine.getY2());
        } else {
            // Stem is below or across beam
            final double beamBottom = LineUtil.intersection(stemLine, getBorder(BOTTOM)).getY();
            return Math.max(0, stemLine.getY1() - beamBottom);
        }
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        final BeamGroupInter group = getGroup();

        if (group != null) {
            return group.getVoice();
        }

        return null;
    }

    //-------------------//
    // hasCommonStemWith //
    //-------------------//
    /**
     * Report whether this beam shares at least one stem with the provided other beam.
     * <p>
     * This allows the building of beam groups.
     *
     * @param that the other beam
     * @return true if so
     */
    public boolean hasCommonStemWith (AbstractBeamInter that)
    {
        final Set<StemInter> thisStems = this.getStems();
        final Set<StemInter> thatStems = that.getStems();
        thatStems.retainAll(thisStems);

        return !thatStems.isEmpty();
    }

    //--------//
    // isGood //
    //--------//
    @Override
    public boolean isGood ()
    {
        return getGrade() >= Grades.goodBeamGrade;
    }

    //--------//
    // isHook //
    //--------//
    /**
     * Report whether this beam is a beam hook.
     *
     * @return true if so
     */
    public boolean isHook ()
    {
        return false;
    }

    //---------//
    // isSmall //
    //---------//
    /**
     * Report whether this beam is a small beam.
     *
     * @return true if so
     */
    public boolean isSmall ()
    {
        return false;
    }

    //-------------//
    // lookupLinks //
    //-------------//
    /**
     * Look up for potential Beam-Stem links around this Beam instance.
     *
     * @param systemStems all stems in system, sorted by abscissa
     * @param system      containing system
     * @param profile     desired profile level
     * @return the potential links
     */
    public Collection<Link> lookupLinks (List<Inter> systemStems,
                                         SystemInfo system,
                                         int profile)
    {
        if (systemStems.isEmpty()) {
            return Collections.emptySet();
        }

        if (isVip()) {
            logger.info("VIP lookupLinks for {}", this);
        }

        final List<Link> links = new ArrayList<>();
        final Scale scale = system.getSheet().getScale();
        final Area luArea = getLookupArea(scale);
        List<Inter> stems = Inters.intersectedInters(systemStems, GeoOrder.NONE, luArea);

        for (Inter inter : stems) {
            StemInter stem = (StemInter) inter;
            Point2D stemMiddle = PointUtil.middle(stem.getMedian());
            VerticalSide vSide = (median.relativeCCW(stemMiddle) > 0) ? TOP : BOTTOM;
            int prof = Math.max(profile, stem.getProfile());
            Link link = BeamStemRelation.checkLink(this, stem, vSide, scale, prof);

            if (link != null) {
                links.add(link);
            }
        }

        return links;
    }

    //----------------//
    // lookupSideLink //
    //----------------//
    /**
     * Lookup for a potential Beam-Stem link on the desired horizontal side of this beam
     *
     * @param systemStems all stems in system, sorted by abscissa
     * @param system      containing system
     * @param side        the desired horizontal side
     * @param profile     desired profile level
     * @return the best potential link if any, null otherwise
     */
    private Link lookupSideLink (List<Inter> systemStems,
                                 SystemInfo system,
                                 HorizontalSide side,
                                 int profile)
    {
        if (systemStems.isEmpty()) {
            return null;
        }

        if (isVip()) {
            logger.info("VIP lookupSideLink for {} on {}", this, side);
        }

        final Line2D top = getBorder(VerticalSide.TOP);
        final Line2D bottom = getBorder(VerticalSide.BOTTOM);
        final Scale scale = system.getSheet().getScale();
        final int xOut = scale.toPixels(BeamStemRelation.getXOutGapMaximum(profile));
        final int xIn = scale.toPixels(BeamStemRelation.getXInGapMaximum(profile));
        final int yGap = scale.toPixels(BeamStemRelation.getYGapMaximum(profile));

        Link bestLink = null;
        double bestGrade = Double.MAX_VALUE;

        final Rectangle luBox = new Rectangle(-1, -1); // "Non-existent" rectangle

        if (side == HorizontalSide.LEFT) {
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

        List<Inter> stems = Inters.intersectedInters(systemStems, GeoOrder.NONE, luBox);

        for (Inter inter : stems) {
            final StemInter stem = (StemInter) inter;
            final int prof = Math.max(profile, stem.getProfile());

            for (VerticalSide vSide : VerticalSide.values()) {
                Link link = BeamStemRelation.checkLink(this, stem, vSide, scale, prof);

                if (link != null) {
                    BeamStemRelation rel = (BeamStemRelation) link.relation;

                    if ((bestLink == null) || (rel.getGrade() > bestGrade)) {
                        bestLink = link;
                        bestGrade = rel.getGrade();
                    }
                }
            }
        }

        return bestLink;
    }

    //--------//
    // preAdd //
    //--------//
    @Override
    public List<? extends UITask> preAdd (WrappedBoolean cancel,
                                          Wrapper<Inter> toPublish)
    {
        final List<UITask> tasks = new ArrayList<>(super.preAdd(cancel, toPublish));

        // Include the created beam into suitable beam group
        BeamGroupInter group = BeamGroupInter.findBeamGroup(this, staff.getSystem(), null);

        if (group != null) {
            tasks.add(new LinkTask(staff.getSystem().getSig(), group, this, new Containment()));
        } else {
            group = new BeamGroupInter();
            group.setManual(true);
            group.setStaff(staff);

            tasks.add(
                    new AdditionTask(
                            staff.getSystem().getSig(),
                            group,
                            getBounds(),
                            Arrays.asList(new Link(this, new Containment(), true))));
        }

        return tasks;
    }

    //---------//
    // preEdit //
    //---------//
    @Override
    public List<? extends UITask> preEdit (InterEditor editor)
    {
        final List<UITask> tasks = new ArrayList<>(super.preEdit(editor));

        // Keep same beam group?
        final BeamGroupInter oldGroup = getGroup();
        boolean oldStillOk = false;

        if (oldGroup != null) {
            // Check if this (edited) beam can still be part of the oldGroup
            final List<Inter> members = oldGroup.getMembers();

            if (members.size() == 1) {
                oldStillOk = true;
            } else {
                members.remove(this);

                final Scale scale = sig.getSystem().getSheet().getScale();

                for (Inter member : members) {
                    if (BeamGroupInter.canBeNeighbors(this, (AbstractBeamInter) member, scale)) {
                        oldStillOk = true;
                    }
                }
            }

            if (!oldStillOk) {
                // Unlink this beam from oldGroup
                final Relation rel = sig.getRelation(oldGroup, this, Containment.class);
                tasks.add(new UnlinkTask(sig, rel));
            }
        }

        // Check for another compatible beam group
        final BeamGroupInter otherGroup = BeamGroupInter.findBeamGroup(
                this,
                sig.getSystem(),
                oldGroup);

        if (otherGroup != null) {
            if ((oldGroup != null) && oldStillOk) {
                // Migrate all beams from oldGroup to otherGroup
                for (Relation r : sig.outgoingEdgesOf(oldGroup)) {
                    if (r instanceof Containment) {
                        Inter b = sig.getEdgeTarget(r);
                        tasks.add(new UnlinkTask(sig, r));
                        tasks.add(new LinkTask(sig, otherGroup, b, new Containment()));
                    }
                }

                // Then remove oldGroup
                tasks.add(new RemovalTask(oldGroup));
            } else {
                // Link this beam to otherGroup
                tasks.add(new LinkTask(sig, otherGroup, this, new Containment()));
            }
        } else {
            // Create a brand new group with just this beam
            final BeamGroupInter newGroup = new BeamGroupInter();
            newGroup.setManual(true);
            newGroup.setStaff(staff);
            tasks.add(
                    new AdditionTask(
                            sig,
                            newGroup,
                            getBounds(),
                            Arrays.asList(new Link(this, new Containment(), true))));
        }

        return tasks;
    }

    //--------//
    // remove //
    //--------//
    @Override
    public void remove (boolean extensive)
    {
        if (isRemoved()) {
            return;
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
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final int profile = Math.max(getProfile(), system.getProfile());
        final List<Inter> systemStems = system.getSig().inters(StemInter.class);
        Collections.sort(systemStems, Inters.byAbscissa);

        return lookupLinks(systemStems, system, profile);
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, BeamStemRelation.class);
    }

    //----------//
    // setGroup //
    //----------//
    /**
     * Assign the containing BeamGroupInter.
     *
     * @param group containing group
     */
    public void setGroup (BeamGroupInter group)
    {
        if (group != null) {
            group.addMember(this);
        }
    }

    //-----------//
    // setLinker //
    //-----------//
    /**
     * Set the dedicated beam-stem linker.
     *
     * @param linker the beam-stem linker
     */
    public void setLinker (BeamLinker linker)
    {
        this.linker = linker;
    }

    //---------------//
    // switchToGroup //
    //---------------//
    /**
     * Move this beam to a new BeamGroupInter.
     *
     * @param group the (new) containing beam group
     */
    public void switchToGroup (BeamGroupInter group)
    {
        final BeamGroupInter oldGroup = getGroup();

        logger.debug("Switching {} from {} to {}", this, oldGroup, group);

        // Trivial noop case
        if (oldGroup == group) {
            return;
        }

        // Remove from current group if any
        if (oldGroup != null) {
            oldGroup.removeMember(this);
        }

        // Assign to new group
        if (group != null) {
            group.addMember(this);
        }
    }

    //-----------------//
    // upgradeOldStuff //
    //-----------------//
    @Override
    public boolean upgradeOldStuff (List<Version> upgrades)
    {
        boolean upgraded = false;

        if (upgrades.contains(Versions.INTER_GEOMETRY)) {
            if (median != null) {
                median.setLine(
                        median.getX1(),
                        median.getY1() + 0.5,
                        median.getX2() + 1,
                        median.getY2() + 0.5);
                computeArea();
                upgraded = true;
            }
        }

        return upgraded;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Fraction minBeamWidth = new Fraction(
                0.5,
                "Minimum width for a beam or beam hook");

        private final Fraction maxBeamStemYGap = new Fraction(
                0.25,
                "Maximum vertical gap between beam and stem");
    }

    //--------//
    // Editor //
    //--------//
    /**
     * User editor for a beam.
     * <p>
     * For a beam, there are 3 handles:
     * <ul>
     * <li>left handle, moving in any direction
     * <li>middle handle, moving the whole beam in any direction
     * <li>right handle, moving in any direction
     * </ul>
     * Left and right end points can snap their abscissa on stems nearby
     */
    private static class Editor
            extends InterEditor
    {
        // Data
        private final Model originalModel;

        private final Model model;

        // To improve speed of stem search
        private SIGraph sig;

        private List<Inter> systemStems;

        public Editor (final AbstractBeamInter beam)
        {
            super(beam);

            originalModel = new Model();
            originalModel.p1 = beam.median.getP1();
            originalModel.p2 = beam.median.getP2();

            model = new Model();
            model.p1 = beam.median.getP1();
            model.p2 = beam.median.getP2();

            // Handles
            final Point2D p1 = beam.median.getP1();
            final Point2D p2 = beam.median.getP2();
            final Point2D middle = PointUtil.middle(p1, p2);

            // Move left
            handles.add(new Handle(p1)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    // Handles
                    PointUtil.add(p1, dx, dy);
                    PointUtil.add(middle, dx / 2.0, dy / 2.0);

                    // Data
                    beam.median.setLine(p1, p2);

                    Double x1 = beam.getSnapAbscissa(LEFT, getSystemStems());

                    if (x1 != null) {
                        // Avoid beam collapse
                        final Scale scale = sig.getSystem().getSheet().getScale();
                        final int minWidth = scale.toPixels(constants.minBeamWidth);
                        final double width = p2.getX() - x1;

                        if (width < minWidth) {
                            x1 = null; // No snap
                        }
                    }

                    model.p1.setLocation((x1 != null) ? x1 : p1.getX(), p1.getY());
                    beam.median.setLine(p1, p2);

                    return true;
                }
            });

            // Global move
            handles.add(selectedHandle = new Handle(middle)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    // Handles
                    for (Handle handle : handles) {
                        PointUtil.add(handle.getPoint(), dx, dy);
                    }

                    // Data
                    beam.median.setLine(p1, p2);

                    Double left = beam.getSnapAbscissa(LEFT, getSystemStems());
                    Double right = beam.getSnapAbscissa(RIGHT, getSystemStems());

                    if (left != null && right != null) {
                        // Both sides get snapped, let's avoid beam collapse
                        final double width = right - left;
                        final Scale scale = sig.getSystem().getSheet().getScale();
                        final int minWidth = scale.toPixels(constants.minBeamWidth);

                        if (width < minWidth) {
                            // Choose stem side
                            if (middle.getX() > 0.5 * (left + right)) {
                                right = left + minWidth;
                            } else {
                                left = right - minWidth;
                            }
                        }
                    }

                    model.p1.setLocation((left != null) ? left : p1.getX(), p1.getY());
                    model.p2.setLocation((right != null) ? right : p2.getX(), p2.getY());
                    beam.median.setLine(p1, p2);

                    return true;
                }
            });

            // Move right
            handles.add(new Handle(p2)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    // Handles
                    PointUtil.add(middle, dx / 2.0, dy / 2.0);
                    PointUtil.add(p2, dx, dy);

                    // Data
                    beam.median.setLine(p1, p2);

                    Double x2 = beam.getSnapAbscissa(RIGHT, getSystemStems());

                    if (x2 != null) {
                        // Avoid beam collapse
                        final Scale scale = sig.getSystem().getSheet().getScale();
                        final int minWidth = scale.toPixels(constants.minBeamWidth);
                        final double width = x2 - p1.getX();

                        if (width < minWidth) {
                            x2 = null; // No snap
                        }
                    }

                    model.p2.setLocation((x2 != null) ? x2 : p2.getX(), p2.getY());
                    beam.median.setLine(p1, p2);

                    return true;
                }
            });
        }

        @Override
        protected void doit ()
        {
            final AbstractBeamInter beam = (AbstractBeamInter) object;
            beam.median.setLine(model.p1, model.p2);
            beam.computeArea(); // Set bounds also

            super.doit(); // No more glyph
        }

        private List<Inter> getSystemStems ()
        {
            final AbstractBeamInter beam = (AbstractBeamInter) object;

            if (sig != beam.getSig()) {
                sig = beam.getSig();
                systemStems = sig.inters(StemInter.class);
                Collections.sort(systemStems, Inters.byAbscissa);
            }

            return systemStems;
        }

        @Override
        public void undo ()
        {
            final AbstractBeamInter beam = (AbstractBeamInter) object;
            beam.median.setLine(originalModel.p1, originalModel.p2);
            beam.computeArea(); // Set bounds also

            super.undo();
        }
    }

    //---------//
    // Impacts //
    //---------//
    /**
     * Impacts definition for a beam.
     */
    public static class Impacts
            extends GradeImpacts
    {
        private static final String[] NAMES = new String[]
        { "wdth", "minH", "maxH", "core", "belt", "jit" };

        private static final int DIST_INDEX = 5;

        private static final double[] WEIGHTS = new double[]
        { 0.5, 1, 1, 2, 2, 2 };

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

        public double getDistImpact ()
        {
            return getImpact(DIST_INDEX);
        }
    }

    //-------//
    // Model //
    //-------//
    public static class Model
            implements ObjectUIModel
    {
        // Left point of median line
        public Point2D p1;

        // Right point of median line
        public Point2D p2;

        // Beam thickness
        public double thickness;

        @Override
        public void translate (double dx,
                               double dy)
        {
            PointUtil.add(p1, dx, dy);
            PointUtil.add(p2, dx, dy);
        }
    }
}
