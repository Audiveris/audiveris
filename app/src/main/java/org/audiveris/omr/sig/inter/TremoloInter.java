//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     T r e m o l o I n t e r                                    //
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.TREMOLO_1;
import static org.audiveris.omr.glyph.Shape.TREMOLO_2;
import static org.audiveris.omr.glyph.Shape.TREMOLO_3;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.ChordOrnamentRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.TremoloStemRelation;
import org.audiveris.omr.sig.relation.TremoloWholeRelation;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.sig.ui.LinkTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.util.WrappedBoolean;
import org.audiveris.omr.util.Wrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>TremoloInter</code> represents a tremolo, perhaps with several lines,
 * as indicated by the precise shape (TREMOLO_1, TREMOLO_2 or TREMOLO_3).
 * <p>
 * We don't support line numbers higher than 3 for the time being.
 * <p>
 * These tremolo signs are of "single" type, linked to a single stem or a single stemless head,
 * as opposed to "double" type located between two stems.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "tremolo")
public class TremoloInter
        extends OrnamentInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TremoloInter.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Meant for JAXB.
     */
    @SuppressWarnings("unchecked")
    private TremoloInter ()
    {
    }

    /**
     * Creates a new TremoloInter object.
     *
     * @param glyph  the glyph to interpret
     * @param bounds object bounds
     * @param shape  TREMOLO_1, TREMOLO_2 or TREMOLO_3
     * @param grade  evaluation grade
     */
    public TremoloInter (Glyph glyph,
                         Rectangle bounds,
                         Shape shape,
                         Double grade)
    {
        super(glyph, bounds, shape, grade);
    }

    /**
     * Creates manually a new TremoloInter ghost object.
     *
     * @param shape TREMOLO_1, TREMOLO_2 or TREMOLO_3
     * @param grade quality grade
     */
    public TremoloInter (Shape shape,
                         Double grade)
    {
        this(null, null, shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        // Check if a stem or a whole head is connected on tremolo
        // @formatter:off
        setAbnormal(!sig.hasRelation(this, TremoloStemRelation.class)
                 && !sig.hasRelation(this, TremoloWholeRelation.class));
        // @formatter:on

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
        // First call needed to get tremolo bounds
        super.deriveFrom(symbol, sheet, font, dropLocation);

        // For a tremolo, we snap center abscissa to nearby stem or whole head if any
        if (staff != null) {
            final Double x = getSnapAbscissa();

            if (x != null) {
                dropLocation.x = (int) Math.rint(x);

                // Final call with refined dropLocation
                super.deriveFrom(symbol, sheet, font, dropLocation);
            }
        }

        return true;
    }

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new Editor(this);
    }

    //-----------------//
    // getSnapAbscissa //
    //-----------------//
    /**
     * Report the theoretical abscissa of the provided tremolo center when correctly aligned
     * with a suitable stem or whole head in its middle.
     *
     * @return the proper abscissa if any, null otherwise
     */
    private Double getSnapAbscissa ()
    {
        if (staff == null) {
            return null;
        }

        // Stem or whole head nearby
        for (Link link : searchLinks(staff.getSystem())) {
            if (link.relation instanceof TremoloStemRelation) {
                final StemInter stem = (StemInter) link.partner;
                return LineUtil.xAtY(stem.getMedian(), getCenter().y);
            }

            if (link.relation instanceof TremoloWholeRelation) {
                final HeadInter head = (HeadInter) link.partner;
                return head.getCenter().getX();
            }
        }

        return null;
    }

    //---------//
    // getStem //
    //---------//
    /**
     * Report the stem, if any, connected to this tremolo.
     *
     * @return the connected stem, perhaps null
     * @see #getWhole
     */
    public StemInter getStem ()
    {
        for (Relation bs : sig.getRelations(this, TremoloStemRelation.class)) {
            return (StemInter) sig.getOppositeInter(this, bs);
        }

        return null;
    }

    //-----------------//
    // getTremoloValue //
    //-----------------//
    /**
     * Report the number of individual lines in this tremolo.
     *
     * @return the number of individual lines
     */
    public int getTremoloValue ()
    {
        return getTremoloValue(shape);
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        final StemInter stem = getStem();
        if (stem != null) {
            return stem.getVoice();
        }

        final HeadInter whole = getWhole();
        if (whole != null) {
            return whole.getVoice();
        }

        return null;
    }

    //----------//
    // getWhole //
    //----------//
    /**
     * Report the whole head, if any, connected to this tremolo.
     *
     * @return the connected whole head, perhaps null
     * @see #getStem
     */
    public HeadInter getWhole ()
    {
        for (Relation bs : sig.getRelations(this, TremoloWholeRelation.class)) {
            return (HeadInter) sig.getOppositeInter(this, bs);
        }

        return null;
    }

    //----------------//
    // linkAsOrnament //
    //----------------//
    /**
     * TremoloInter is an ornament and as such must be linked via a ChordOrnamentRelation
     * with the chord of the related stem or whole head.
     *
     * @param inter the tremolo stem or whole head
     */
    private void linkAsOrnament (Inter inter)
    {
        if (inter instanceof StemInter stem) {
            for (HeadChordInter headChord : stem.getChords()) {
                sig.addEdge(headChord, this, new ChordOrnamentRelation());
            }
        } else if (inter instanceof HeadInter whole) {
            final HeadChordInter headChord = whole.getChord();
            if (headChord != null) {
                sig.addEdge(headChord, this, new ChordOrnamentRelation());
            }
        } else {
            logger.error("linkAsOrnament called with unexpected {}", inter);
        }
    }

    //----------------//
    // lookupStemLink //
    //----------------//
    /**
     * Lookup for a potential Tremolo-Stem link.
     *
     * @param systemStems all stems in system, sorted by abscissa
     * @param system      containing system
     * @param profile     desired profile level
     * @return the best potential link if any, null otherwise
     */
    private Link lookupStemLink (List<Inter> systemStems,
                                 SystemInfo system,
                                 int profile)
    {
        if (systemStems.isEmpty()) {
            return null;
        }

        if (isVip()) {
            logger.info("VIP lookupStemLink for {}", this);
        }

        // Lookup area centered on tremolo
        final Scale scale = system.getSheet().getScale();
        final int xOut = scale.toPixels(TremoloStemRelation.getCenterDxMaximum(profile));
        final int yGap = scale.toPixels(TremoloStemRelation.getYGapMaximum(profile));
        final Rectangle tBox = getBounds();
        final double yShift = Math.abs(tBox.width * 0.5 * constants.slope.getValue());
        final Point center = getCenter();
        final Rectangle luBox = new Rectangle(center.x, center.y, 0, 0);
        luBox.grow(xOut, (int) Math.rint(tBox.height * 0.5 - yShift + yGap));

        double bestGrade = Double.MAX_VALUE;
        Relation bestRel = null;
        StemInter bestStem = null;

        final List<Inter> stems = Inters.intersectedInters(systemStems, GeoOrder.NONE, luBox);

        for (Inter inter : stems) {
            final StemInter stem = (StemInter) inter;

            // dx
            final double dx = Math.abs(center.x - LineUtil.xAtY(stem.getMedian(), center.y));

            // dy
            final Line2D median = stem.getMedian(); // Defined from top to bottom
            final double t1 = tBox.y + yShift;
            final double t2 = tBox.y + tBox.height - 1 - yShift;
            final double y1 = Math.max(median.getY1(), t1);
            final double y2 = Math.min(median.getY2(), t2);
            final double dy = (y1 <= y2) ? 0 : y1 - y2;

            final TremoloStemRelation tRel = new TremoloStemRelation();
            tRel.setInOutGaps(scale.pixelsToFrac(dx), scale.pixelsToFrac(dy), profile);

            if ((bestRel == null) || (tRel.getGrade() > bestGrade)) {
                bestRel = tRel;
                bestStem = stem;
                bestGrade = tRel.getGrade();
            }
        }

        return (bestRel != null) ? new Link(bestStem, bestRel, true) : null;
    }

    //-----------------//
    // lookupWholeLink //
    //-----------------//
    /**
     * Lookup for a potential Tremolo-Whole link.
     *
     * @param systemHeads all heads in system, sorted by abscissa
     * @param system      containing system
     * @param profile     desired profile level
     * @return the best potential link if any, null otherwise
     */
    private Link lookupWholeLink (List<Inter> systemHeads,
                                  SystemInfo system,
                                  int profile)
    {
        if (systemHeads.isEmpty()) {
            return null;
        }

        if (isVip()) {
            logger.info("VIP lookupWholeLink for {}", this);
        }

        // Lookup area centered on tremolo
        final Scale scale = system.getSheet().getScale();
        final int xOut = scale.toPixels(TremoloWholeRelation.getCenterDxMaximum(profile));
        final int yGap = scale.toPixels(TremoloWholeRelation.getYGapMaximum(profile));
        final Rectangle tBox = getBounds();
        final double yShift = Math.abs(tBox.width * 0.5 * constants.slope.getValue());
        final Point center = getCenter();
        final Rectangle luBox = new Rectangle(center.x, center.y, 0, 0);
        luBox.grow(xOut, (int) Math.rint(tBox.height * 0.5 - yShift + yGap));

        double bestGrade = Double.MAX_VALUE;
        Relation bestRel = null;
        HeadInter bestWhole = null;

        final List<Inter> heads = Inters.intersectedInters(systemHeads, GeoOrder.NONE, luBox);

        for (Inter inter : heads) {
            if (!inter.getShape().isStemLessHead()) {
                continue;
            }

            final HeadInter whole = (HeadInter) inter;
            final Rectangle wBox = whole.getBounds();
            final Point wholeCenter = whole.getCenter();

            // dx
            final double dx = Math.abs(center.x - wholeCenter.x);

            // dy
            final double t1 = tBox.y + yShift;
            final double t2 = tBox.y + tBox.height - 1 - yShift;
            final double y1 = Math.min(wBox.y + wBox.height - 1, t2);
            final double y2 = Math.max(wBox.y, t1);
            final double dy = (y1 <= y2) ? 0 : y1 - y2;

            final TremoloWholeRelation tRel = new TremoloWholeRelation();
            tRel.setInOutGaps(scale.pixelsToFrac(dx), scale.pixelsToFrac(dy), profile);

            if ((bestRel == null) || (tRel.getGrade() > bestGrade)) {
                bestRel = tRel;
                bestWhole = whole;
                bestGrade = tRel.getGrade();
            }
        }

        return (bestRel != null) ? new Link(bestWhole, bestRel, true) : null;
    }

    //--------//
    // preAdd //
    //--------//
    @Override
    public List<? extends UITask> preAdd (WrappedBoolean cancel,
                                          Wrapper<Inter> toPublish)
    {
        // Standard addition task for this tremolo, keeping links apart
        final SystemInfo system = staff.getSystem();
        final List<UITask> tasks = new ArrayList<>();
        final Collection<Link> links = searchLinks(system);
        tasks.add(new AdditionTask(system.getSig(), this, getBounds(), links));

        // Link tremolo as a chord ornament
        for (Link link : links) {
            final HeadChordInter chord;

            if (link.relation instanceof TremoloStemRelation) {
                final StemInter stem = (StemInter) link.partner;
                chord = stem.getChords().get(0);
            } else if (link.relation instanceof TremoloWholeRelation) {
                final HeadInter head = (HeadInter) link.partner;
                chord = head.getChord();
            } else
                throw new IllegalStateException("Unexpected tremolo link: " + link);

            final ChordOrnamentRelation rel = new ChordOrnamentRelation();
            rel.setManual(true);

            tasks.add(new LinkTask(system.getSig(), chord, this, rel));
            break;
        }

        return tasks;
    }

    //-------------//
    // searchLinks //
    //-------------//
    /**
     * {@inheritDoc }
     * <p>
     * Specifically, look for stem or whole head to allow attachment.
     */
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final int profile = Math.max(getProfile(), system.getProfile());

        final List<Inter> systemStems = system.getSig().inters(StemInter.class);
        Collections.sort(systemStems, Inters.byAbscissa);
        Link link = lookupStemLink(systemStems, system, profile);

        if (link == null) {
            final List<Inter> systemHeads = system.getSig().inters(HeadInter.class);
            Collections.sort(systemHeads, Inters.byAbscissa);
            link = lookupWholeLink(systemHeads, system, profile);
        }

        return (link == null) ? Collections.emptyList() : Collections.singleton(link);
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, TremoloStemRelation.class, TremoloWholeRelation.class);
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-----------//
    // aggregate //
    //-----------//
    /**
     * Try to aggregate tremolos in the provided system.
     *
     * @param system the containing system
     */
    public static void aggregate (SystemInfo system)
    {
        final SIGraph sig = system.getSig();

        // Stems
        aggregateAlong(sig.inters(StemInter.class), TremoloStemRelation.class, sig);

        // Wholes
        final List<Inter> heads = sig.inters(HeadInter.class);
        heads.removeIf( (inter) -> !inter.getShape().isStemLessHead());
        aggregateAlong(heads, TremoloWholeRelation.class, sig);
    }

    //----------------//
    // aggregateAlong //
    //----------------//
    /**
     * Aggregate tremolos along the provided population of inters (stems / wholes).
     *
     * @param inters        either system stems or system wholes
     * @param relationClass either TremoloStemRelation or TremoloWholeRelation class
     * @param sig           the containing SIG
     */
    private static void aggregateAlong (List<Inter> inters,
                                        Class<? extends Relation> relationClass,
                                        SIGraph sig)
    {
        for (Inter inter : inters) {
            final Set<Relation> tremRels = sig.getRelations(inter, relationClass);
            if (tremRels.size() > 1) {
                // Tremolo aggregation needed
                final Set<TremoloInter> trems = new LinkedHashSet<>();
                int count = 0;
                double totalGrade = 0;
                Rectangle bounds = null;

                for (Relation rel : tremRels) {
                    final TremoloInter trem = (TremoloInter) sig.getOppositeInter(inter, rel);
                    trems.add(trem);
                    totalGrade += trem.getGrade();
                    count += trem.getTremoloValue();
                    if (bounds == null) {
                        bounds = trem.getBounds();
                    } else {
                        bounds = bounds.union(trem.getBounds());
                    }
                }

                try {
                    double grade = totalGrade / trems.size();
                    final Shape shape = TremoloInter.getTremoloShape(count);
                    final TremoloInter compound = new TremoloInter(null, bounds, shape, grade);
                    sig.addVertex(compound);
                    final Relation rel = relationClass.getDeclaredConstructor().newInstance();
                    sig.addEdge(compound, inter, rel);
                    compound.linkAsOrnament(inter);

                    // Clean up
                    for (TremoloInter trem : trems) {
                        trem.remove();
                    }
                } catch (Exception ex) {
                    logger.warn("Could not aggregate tremolos around " + inter, ex);
                }
            }
        }
    }

    //------------------//
    // createValidAdded //
    //------------------//
    /**
     * (Try to) create and add a valid TremoloInter.
     *
     * @param glyph       underlying glyph
     * @param shape       detected shape
     * @param grade       assigned grade
     * @param system      containing system
     * @param systemStems system stems, ordered by abscissa
     * @param systemHeads system heads, ordered by abscissa
     * @return the created tremolo or null
     */
    public static TremoloInter createValidAdded (Glyph glyph,
                                                 Shape shape,
                                                 double grade,
                                                 SystemInfo system,
                                                 List<Inter> systemStems,
                                                 List<Inter> systemHeads)
    {
        if (glyph.isVip()) {
            logger.info("VIP TremoloInter create {} as {}", glyph, shape);
        }

        final TremoloInter tremolo = new TremoloInter(glyph, glyph.getBounds(), shape, grade);

        Link link = tremolo.lookupStemLink(systemStems, system, system.getProfile());

        if (link != null) {
            return createWithLink(tremolo, link, system);
        }

        link = tremolo.lookupWholeLink(systemHeads, system, system.getProfile());

        if (link != null) {
            return createWithLink(tremolo, link, system);
        }

        return null;
    }

    //----------------//
    // createWithLink //
    //----------------//
    /**
     * Apply the provided link of the created tremolo.
     *
     * @param tremolo the tremolo just created
     * @param link    the link to apply
     * @param system  the containing system
     * @return the (properly linked) tremolo
     */
    private static TremoloInter createWithLink (TremoloInter tremolo,
                                                Link link,
                                                SystemInfo system)
    {
        final SIGraph sig = system.getSig();
        sig.addVertex(tremolo);
        link.applyTo(tremolo);
        tremolo.linkAsOrnament(link.partner);

        return tremolo;
    }

    //-----------------//
    // getTremoloShape //
    //-----------------//
    /**
     * Report the precise tremolo shape, knowing its number of lines.
     *
     * @param value number of lines
     * @return the corresponding tremolo shape
     */
    public static Shape getTremoloShape (int value)
    {
        return switch (value) {
            case 1 -> TREMOLO_1;
            case 2 -> TREMOLO_2;
            case 3 -> TREMOLO_3;
            default -> throw new IllegalArgumentException("Unsupported tremolo value " + value);
        };
    }

    //-----------------//
    // getTremoloValue //
    //-----------------//
    /**
     * Report the number of individual tremolo lines that corresponds to the tremolo shape.
     *
     * @param shape the given tremolo shape
     * @return the number of individual lines
     */
    public static int getTremoloValue (Shape shape)
    {
        return switch (shape) {
            case TREMOLO_1 -> 1;
            case TREMOLO_2 -> 2;
            case TREMOLO_3 -> 3;
            default -> throw new IllegalArgumentException("Unsupported tremolo shape " + shape);
        };
    }

    //----------------//
    // isTremoloWidth //
    //----------------//
    /**
     * Report whether the provided width (say of a beam) is close to typical tremolo width.
     *
     * @param width the provided width
     * @param scale scaling information
     * @return true if so
     */
    public static boolean isTremoloWidth (double width,
                                          Scale scale)
    {
        final double typicalWidth = scale.toPixelsDouble(constants.width);
        final double widthMargin = scale.toPixelsDouble(constants.widthMargin);

        return Math.abs(width - typicalWidth) <= widthMargin;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    public static class Constants
            extends ConstantSet
    {
        private final Scale.Fraction width = new Scale.Fraction(1.35, "Typical tremolo width");

        private final Scale.Fraction widthMargin = new Scale.Fraction(
                0.25,
                "Margin around tremolo width");

        private final Constant.Double slope = new Constant.Double(
                "tangent",
                -0.31,
                "Typical tremolo slope");
    }

    //--------//
    // Editor //
    //--------//
    /**
     * User editor for a tremolo.
     * <p>
     * The middle handle can move in any direction, but horizontally it tries to snap to a stem
     * or a whole head.
     */
    private static class Editor
            extends InterEditor
    {
        // Original data
        private final Rectangle originalBounds;

        // Latest data
        private final Rectangle latestBounds;

        public Editor (final TremoloInter tremolo)
        {
            super(tremolo);

            originalBounds = tremolo.getBounds();
            latestBounds = tremolo.getBounds();

            final double halfHeight = latestBounds.height / 2.0;
            final double halfWidth = latestBounds.width / 2.0;

            handles.add(selectedHandle = new Handle(tremolo.getCenter())
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    // Handle
                    PointUtil.add(center, dx, dy);

                    // Data
                    latestBounds.x = (int) Math.rint(center.getX() - halfWidth);
                    latestBounds.y = (int) Math.rint(center.getY() - halfHeight);
                    tremolo.setBounds(latestBounds);

                    final Double x = tremolo.getSnapAbscissa();

                    if (x != null) {
                        latestBounds.x = (int) Math.rint(x - halfWidth);
                    }

                    return true;
                }
            });
        }

        @Override
        protected void doit ()
        {
            getInter().setBounds(latestBounds);
            super.doit(); // No more glyph
        }

        @Override
        public void undo ()
        {
            getInter().setBounds(originalBounds);
            super.undo();
        }
    }
}
