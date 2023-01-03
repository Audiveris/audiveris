//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     T r e m o l o I n t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.sig.ui.LinkTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.util.WrappedBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>TremoloInter</code> represents a tremolo, perhaps with several lines,
 * as indicated by the precise shape (TREMOLO_1, TREMOLO_3 or TREMOLO_3).
 * <p>
 * We don't support line numbers higher than 3 for the time being.
 * <p>
 * These tremolo signs are of "single" type, linked to a single stem, as opposed to "double" type
 * located between two stems.
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
     * Creates a new TremoloInter object.
     *
     * @param glyph  the glyph to interpret
     * @param bounds object bounds
     * @param shape  TREMOLO_1, TREMOLO_3 or TREMOLO_3
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
     * @param shape TREMOLO_1, TREMOLO_3 or TREMOLO_3
     * @param grade quality grade
     */
    public TremoloInter (Shape shape,
                         Double grade)
    {
        this(null, null, shape, grade);
    }

    /**
     * Meant for JAXB.
     */
    @SuppressWarnings("unchecked")
    private TremoloInter ()
    {
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

    //-------------------//
    // aggregateTremolos //
    //-------------------//
    /**
     * Aggregate tremolos along the same stem.
     *
     * @param system the containing system
     */
    public static void aggregateTremolos (SystemInfo system)
    {
        final SIGraph sig = system.getSig();

        for (Inter inter : sig.inters(StemInter.class)) {
            final StemInter stem = (StemInter) inter;

            final Set<Relation> tremRels = sig.getRelations(stem, TremoloStemRelation.class);
            if (tremRels.size() > 1) {
                // Tremolo aggregation needed
                final Set<TremoloInter> trems = new LinkedHashSet<>();
                int count = 0;
                double totalGrade = 0;
                Rectangle bounds = null;

                for (Relation rel : tremRels) {
                    final TremoloInter trem = (TremoloInter) sig.getOppositeInter(stem, rel);
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
                    sig.addEdge(compound, stem, new TremoloStemRelation());
                    compound.linkAsOrnament(stem);

                    // Clean up
                    for (TremoloInter trem : trems) {
                        trem.remove();
                    }
                } catch (Exception ex) {
                    logger.warn("Could not aggregate tremolos around " + stem, ex);
                }
            }
        }
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        // Check if a stem is connected on tremolo abscissa center
        setAbnormal(!sig.hasRelation(this, TremoloStemRelation.class));

        return isAbnormal();
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
     * @return the created tremolo or null
     */
    public static TremoloInter createValidAdded (Glyph glyph,
                                                 Shape shape,
                                                 double grade,
                                                 SystemInfo system,
                                                 List<Inter> systemStems)
    {
        if (glyph.isVip()) {
            logger.info("VIP TremoloInter create {} as {}", glyph, shape);
        }

        TremoloInter tremolo = new TremoloInter(glyph, glyph.getBounds(), shape, grade);
        Link link = tremolo.lookupLink(systemStems, system, system.getProfile());

        if (link != null) {
            final SIGraph sig = system.getSig();
            sig.addVertex(tremolo);
            link.applyTo(tremolo);
            tremolo.linkAsOrnament((StemInter) link.partner);

            return tremolo;
        }

        return null;
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

        // For a tremolo, we snap center abscissa to nearby stem if any
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
     * with a suitable stem in its middle.
     *
     * @return the proper abscissa if any, null otherwise
     */
    private Double getSnapAbscissa ()
    {

        if (staff == null) {
            return null;
        }

        // Best stem nearby (zero or one stem)
        for (Link link : searchLinks(staff.getSystem())) {
            final StemInter stem = (StemInter) link.partner;

            return LineUtil.xAtY(stem.getMedian(), getCenter().y);
        }

        return null;
    }

    //---------//
    // getStem //
    //---------//
    /**
     * Report the stem connected to this tremolo.
     *
     * @return the connected stem, perhaps null
     */
    public StemInter getStem ()
    {
        for (Relation bs : sig.getRelations(this, TremoloStemRelation.class)) {
            return (StemInter) sig.getOppositeInter(this, bs);
        }

        return null;
    }

    //-----------------//
    // getTremoloShape //
    //-----------------//
    public static Shape getTremoloShape (int value)
    {
        return switch (value) {
            case 1 ->
                TREMOLO_1;
            case 2 ->
                TREMOLO_2;
            case 3 ->
                TREMOLO_3;
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
            case TREMOLO_1 ->
                1;
            case TREMOLO_2 ->
                2;
            case TREMOLO_3 ->
                3;
            default -> throw new IllegalArgumentException("Unsupported tremolo shape " + shape);
        };
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
        } else {
            return null;
        }
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
        return !(width < typicalWidth - widthMargin || width > typicalWidth + widthMargin);
    }

    //----------------//
    // linkAsOrnament //
    //----------------//
    /**
     * TremoloInter is an ornament and as such must be linked via a ChordOrnamentRelation
     * with the stem chord.
     *
     * @param stem the tremolo stem
     */
    private void linkAsOrnament (StemInter stem)
    {
        for (HeadChordInter headChord : stem.getChords()) {
            sig.addEdge(headChord, this, new ChordOrnamentRelation());
            return;
        }
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Lookup for a potential Tremolo-Stem link.
     *
     * @param systemStems all stems in system, sorted by abscissa
     * @param system      containing system
     * @param profile     desired profile level
     * @return the best potential link if any, null otherwise
     */
    private Link lookupLink (List<Inter> systemStems,
                             SystemInfo system,
                             int profile)
    {
        if (systemStems.isEmpty()) {
            return null;
        }

        if (isVip()) {
            logger.info("VIP lookupLink for {}", this);
        }

        // Lookup area centered on tremolo
        final Scale scale = system.getSheet().getScale();
        final int xOut = scale.toPixels(TremoloStemRelation.getCenterDxMaximum(profile));
        final int yGap = scale.toPixels(TremoloStemRelation.getYGapMaximum(profile));
        final Rectangle tBox = getBounds();
        final double yShift = Math.abs(tBox.width * 0.5 * constants.slope.getValue());
        final Point center = getCenter();
        final Rectangle luBox = new Rectangle(center.x, center.y, 0, 0);
        luBox.grow(xOut,
                   (int) Math.rint(tBox.height * 0.5 - yShift + yGap));

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
            // Vertical overlap?
            final double yMin = Math.max(median.getY1(), t1);
            final double yMax = Math.min(median.getY2(), t2);
            final double dy = (yMax >= yMin) ? 0 : yMin - yMax;

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

    //--------//
    // preAdd //
    //--------//
    @Override
    public List<? extends UITask> preAdd (WrappedBoolean cancel)
    {
        // Standard addition task for this tremolo
        final SystemInfo system = staff.getSystem();
        final List<UITask> tasks = new ArrayList<>();
        final Collection<Link> links = searchLinks(system);
        tasks.add(new AdditionTask(system.getSig(), this, getBounds(), links));

        // Link tremolo as a chord ornament
        for (Link link : links) {
            final StemInter stem = (StemInter) link.partner;
            final HeadChordInter chord = stem.getChords().get(0);
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
     * {@inheritDoc}
     * <p>
     * Specifically, look for stem to allow center attachment.
     *
     * @return stem link, perhaps empty
     */
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {

        final List<Inter> systemStems = system.getSig().inters(StemInter.class);
        Collections.sort(systemStems, Inters.byAbscissa);

        final int profile = Math.max(getProfile(), system.getProfile());
        final Link link = lookupLink(systemStems, system, profile);

        return (link == null) ? Collections.emptyList() : Collections.singleton(link);
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, TremoloStemRelation.class);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    public static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction width = new Scale.Fraction(
                1.35,
                "Typical tremolo width");

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
     * The middle handle can move in any direction, but horizontally it tries to snap to a stem.
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
                    PointUtil.add(selectedHandle.getPoint(), dx, dy);

                    // Data
                    Point2D center = selectedHandle.getPoint();
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
