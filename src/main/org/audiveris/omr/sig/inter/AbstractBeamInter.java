//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A b s t r a c t B e a m I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.Versions;
import org.audiveris.omr.sheet.beam.BeamGroup;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.BeamPortion;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.sig.ui.InterEditor.Handle;
import org.audiveris.omr.sig.ui.InterUIModel;
import org.audiveris.omr.ui.symbol.Alignment;
import org.audiveris.omr.ui.symbol.BeamSymbol;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Version;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
 * src=
 * "http://upload.wikimedia.org/wikipedia/commons/thumb/8/8e/Beamed_notes.svg/220px-Beamed_notes.svg.png">
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractBeamInter
        extends AbstractInter
{

    private static final Logger logger = LoggerFactory.getLogger(AbstractBeamInter.class);

    // Persistent data
    //----------------
    //
    /** Beam height. */
    @XmlAttribute
    @XmlJavaTypeAdapter(type = double.class, value = Jaxb.Double1Adapter.class)
    protected double height;

    /** Median line. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.Line2DAdapter.class)
    protected Line2D median;

    // Transient data
    //---------------
    //
    /** The containing beam group. */
    private BeamGroup group;

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
    }

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
        final int yDir = (headToBeam == TOP) ? (-1) : 1;
        final Line2D beamBorder = getBorder(headToBeam.opposite());
        bRel = new BeamStemRelation();

        // Precise cross point
        Point2D start = stem.getTop();
        Point2D stop = stem.getBottom();
        Point2D crossPt = LineUtil.intersection(stem.getMedian(), beamBorder);

        // Extension point
        bRel.setExtensionPoint(
                new Point2D.Double(crossPt.getX(), crossPt.getY() + (yDir * (getHeight() - 1))));

        // Abscissa -> beamPortion
        // toLeft & toRight are >0 if within beam, <0 otherwise
        double toLeft = crossPt.getX() - beamBorder.getX1();
        double toRight = beamBorder.getX2() - crossPt.getX();
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

        bRel.setOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), manual);

        if (bRel.getGrade() >= bRel.getMinGrade()) {
            logger.debug("{} {} {}", this, stem, bRel);

            return new Link(stem, bRel, true);
        } else {
            return null;
        }
    }

    //----------//
    // contains //
    //----------//
    @Override
    public boolean contains (Point point)
    {
        getBounds();

        if ((bounds != null) && !bounds.contains(point)) {
            return false;
        }

        if ((glyph != null) && glyph.contains(point)) {
            return true;
        }

        if (area == null) {
            computeArea();
        }

        return area.contains(point);
    }

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
        final double dy = (side == TOP) ? (-height / 2) : (height / 2);

        return new Line2D.Double(
                median.getX1(),
                median.getY1() + dy,
                median.getX2(),
                median.getY2() + dy);
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds != null) {
            return new Rectangle(bounds);
        }

        if (glyph != null) {
            return new Rectangle(bounds = glyph.getBounds());
        }

        if (area == null) {
            computeArea();
        }

        return new Rectangle(bounds = area.getBounds());
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
        List<AbstractChordInter> chords = new ArrayList<>();

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

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        StringBuilder sb = new StringBuilder(super.getDetails());

        if (group != null) {
            sb.append(" group:").append(group.getId());
        }

        return sb.toString();
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
    public BeamGroup getGroup ()
    {
        return group;
    }

    //----------//
    // setGroup //
    //----------//
    /**
     * Assign the containing BeamGroup.
     *
     * @param group containing group
     */
    public void setGroup (BeamGroup group)
    {
        this.group = group;
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
     * Report the stems connected to this beam.
     *
     * @return the set of connected stems, perhaps empty
     */
    public Set<StemInter> getStems ()
    {
        Set<StemInter> stems = new LinkedHashSet<>();

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

    //------------//
    // deriveFrom //
    //------------//
    @Override
    public void deriveFrom (ShapeSymbol symbol,
                            Sheet sheet,
                            MusicFont font,
                            Point dropLocation,
                            Alignment alignment)
    {
        BeamSymbol beamSymbol = (BeamSymbol) symbol;
        Model model = beamSymbol.getModel(font, dropLocation, alignment);
        median = new Line2D.Double(model.p1, model.p2);

        // Beam height adjusted according to sheet scale?
        final Integer beamThickness = sheet.getScale().getBeamThickness();
        height = (beamThickness != null) ? beamThickness : model.thickness;

        computeArea();

        if (staff != null) {
            SIGraph sig = staff.getSystem().getSig();
            List<Inter> systemStems = sig.inters(StemInter.class);
            Collections.sort(systemStems, Inters.byAbscissa);

            // Snap sides?
            boolean modified = false;

            final Double x1 = getSnapAbscissa(LEFT, systemStems);
            if (x1 != null) {
                model.p1.setLocation(x1, model.p1.getY());
                modified = true;
            }

            final Double x2 = getSnapAbscissa(RIGHT, systemStems);
            if (x2 != null) {
                model.p2.setLocation(x2, model.p2.getY());
                modified = true;
            }

            if (modified) {
                median.setLine(model.p1, model.p2);
                computeArea();
                dropLocation.setLocation(PointUtil.middle(median));
            }
        }
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
    /**
     * Report whether this beam is a beam hook.
     *
     * @return true if so
     */
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
    public Collection<Link> searchLinks (SystemInfo system)
    {
        List<Inter> systemStems = system.getSig().inters(StemInter.class);
        Collections.sort(systemStems, Inters.byAbscissa);

        return lookupLinks(systemStems, system);
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
    // setGlyph //
    //----------//
    @Override
    public void setGlyph (Glyph glyph)
    {
        super.setGlyph(glyph);

        if ((median == null) && (glyph != null)) {
            // Case of manual beam: Compute height and median parameters and area
            height = (int) Math.rint(glyph.getMeanThickness(Orientation.HORIZONTAL));
            median = glyph.getCenterLine();

            computeArea();
        }
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

    //-----------------//
    // upgradeOldStuff //
    //-----------------//
    @Override
    public boolean upgradeOldStuff (List<Version> upgrades)
    {
        boolean upgraded = false;

        if (upgrades.contains(Versions.INTER_GEOMETRY)) {
            if (median != null) {
                median.setLine(median.getX1(), median.getY1() + 0.5,
                               median.getX2() + 1, median.getY2() + 0.5);
                computeArea();
                upgraded = true;
            }
        }

        return upgraded;
    }

    //-------------//
    // computeArea //
    //-------------//
    /**
     * Compute the beam area.
     */
    protected final void computeArea ()
    {
        setArea(AreaUtil.horizontalParallelogram(median.getP1(), median.getP2(), height));

        // Define precise bounds based on this path
        bounds = getArea().getBounds();
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

    /**
     * Define lookup area around the beam for potential stems
     *
     * @return the look up area
     */
    private Area getLookupArea (Scale scale)
    {
        final Line2D top = getBorder(TOP);
        final Line2D bottom = getBorder(BOTTOM);
        final int xOut = scale.toPixels(BeamStemRelation.getXOutGapMaximum(manual));
        final int yGap = scale.toPixels(BeamStemRelation.getYGapMaximum(manual));

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

    //-------------//
    // lookupLinks //
    //-------------//
    /**
     * Look up for potential Beam-Stem links around this Beam instance.
     * <p>
     * This method used to check for stems only on beam left and right sides.
     * Now, it check also for stems within the whole beam width.
     *
     * @param systemStems all stems in system, sorted by abscissa
     * @param system      containing system
     * @return the potential links
     */
    private Collection<Link> lookupLinks (List<Inter> systemStems,
                                          SystemInfo system)
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
            Link link = checkLink(stem, vSide, scale);

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
     * @return the best potential link if any, null otherwise
     */
    private Link lookupSideLink (List<Inter> systemStems,
                                 SystemInfo system,
                                 HorizontalSide side)
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
        final int xOut = scale.toPixels(BeamStemRelation.getXOutGapMaximum(manual));
        final int xIn = scale.toPixels(BeamStemRelation.getXInGapMaximum(manual));
        final int yGap = scale.toPixels(BeamStemRelation.getYGapMaximum(manual));

        Link bestLink = null;
        double bestGrade = Double.MAX_VALUE;

        final Rectangle luBox = new Rectangle(-1, -1); // "Non-existant" rectangle

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

        return bestLink;
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

        final Link link = lookupSideLink(systemStems, system, side);

        if (link != null) {
            final StemInter stem = (StemInter) link.partner;
            final Point2D beamEnd = (side == LEFT) ? median.getP1() : median.getP2();
            return LineUtil.xAtY(stem.getMedian(), beamEnd.getY());
        }

        return null;
    }

    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends GradeImpacts
    {

        private static final String[] NAMES = new String[]{
            "wdth",
            "minH",
            "maxH",
            "core",
            "belt",
            "jit"};

        private static final int DIST_INDEX = 5;

        private static final double[] WEIGHTS = new double[]{0.5, 1, 1, 2, 2, 2};

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
                public boolean move (Point vector)
                {
                    // Handles
                    PointUtil.add(p1, vector);
                    PointUtil.add(middle, vector.x / 2.0, vector.y / 2.0);

                    // Data
                    beam.median.setLine(p1, p2);

                    final Double x1 = beam.getSnapAbscissa(LEFT, getSystemStems());
                    model.p1.setLocation((x1 != null) ? x1 : p1.getX(), p1.getY());

                    return true;
                }
            });

            // Global move
            handles.add(selectedHandle = new Handle(middle)
            {
                @Override
                public boolean move (Point vector)
                {
                    // Handles
                    for (Handle handle : handles) {
                        PointUtil.add(handle.getHandleCenter(), vector);
                    }

                    // Data
                    beam.median.setLine(p1, p2);

                    final Double x1 = beam.getSnapAbscissa(LEFT, getSystemStems());
                    model.p1.setLocation((x1 != null) ? x1 : p1.getX(), p1.getY());

                    final Double x2 = beam.getSnapAbscissa(RIGHT, getSystemStems());
                    model.p2.setLocation((x2 != null) ? x2 : p2.getX(), p2.getY());

                    return true;
                }
            });

            // Move right
            handles.add(new Handle(p2)
            {
                @Override
                public boolean move (Point vector)
                {
                    // Handles
                    PointUtil.add(middle, vector.x / 2.0, vector.y / 2.0);
                    PointUtil.add(p2, vector);

                    // Data
                    beam.median.setLine(p1, p2);

                    final Double x2 = beam.getSnapAbscissa(RIGHT, getSystemStems());
                    model.p2.setLocation((x2 != null) ? x2 : p2.getX(), p2.getY());

                    return true;
                }
            });
        }

        private List<Inter> getSystemStems ()
        {
            final AbstractBeamInter beam = (AbstractBeamInter) inter;

            if (sig != beam.getSig()) {
                sig = beam.getSig();
                systemStems = sig.inters(StemInter.class);
                Collections.sort(systemStems, Inters.byAbscissa);
            }

            return systemStems;
        }

        @Override
        protected void doit ()
        {
            final AbstractBeamInter beam = (AbstractBeamInter) inter;
            beam.median.setLine(model.p1, model.p2);
            beam.computeArea(); // Set bounds also

            super.doit(); // No more glyph
        }

        @Override
        public void undo ()
        {
            final AbstractBeamInter beam = (AbstractBeamInter) inter;
            beam.median.setLine(originalModel.p1, originalModel.p2);
            beam.computeArea(); // Set bounds also

            super.undo();
        }
    }

    //-------//
    // Model //
    //-------//
    public static class Model
            implements InterUIModel
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
