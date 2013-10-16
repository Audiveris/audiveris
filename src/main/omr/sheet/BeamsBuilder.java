//----------------------------------------------------------------------------//
//                                                                            //
//                            B e a m s B u i l d e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.image.AreaMask;
import omr.image.PixelFilter;

import omr.math.AreaUtil;
import omr.math.GeoOrder;
import omr.math.Line;
import omr.math.LineUtil;

import omr.run.Orientation;

import omr.sig.BeamInter;
import omr.sig.BeamInter.Impacts;
import omr.sig.Grades;
import omr.sig.Inter;
import omr.sig.SIGraph;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.Navigable;
import omr.util.WrappedInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code BeamsBuilder} is in charge, at system level, of
 * retrieving the possible beam interpretations.
 * <p>
 * The retrieval is performed on the collection of spots produced by closing
 * the blurred initial image with a disk-shape structure element whose diameter
 * is just slightly smaller than the typical beam height.
 *
 * @author Hervé Bitteur
 */
public class BeamsBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            BeamsBuilder.class);

    //~ Instance fields --------------------------------------------------------
    /** Scale-dependent constants. */
    private final Parameters params;

    /** Beams for this system, NOT sorted. */
    private List<Inter> rawSystemBeams;

    /** The related sheet */
    @Navigable(false)
    private final Sheet sheet;

    /** The related SIG. */
    private final SIGraph sig;

    /** Vertical stem seeds for this system, sorted by abscissa. */
    private List<Glyph> sortedSystemSeeds;

    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // BeamsBuilder //
    //--------------//
    /**
     * Creates a new BeamsBuilder object.
     *
     * @param system the dedicated system
     */
    public BeamsBuilder (SystemInfo system)
    {
        this.system = system;

        sig = system.getSig();
        sheet = system.getSheet();
        params = new Parameters(sheet.getScale());

        if (system.getId() == 1) {
            Main.dumping.dump(params);
        }
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // buildBeams //
    //------------//
    /**
     * Find possible interpretations of beams among system spots.
     */
    public void buildBeams ()
    {
        // First, retrieve beams from spots
        for (Glyph glyph : getSpots()) {
            final String failure = checkBeamGlyph(glyph);

            if (failure != null) {
                if (glyph.isVip()) {
                    logger.info("VIP beam#{} {}", glyph.getId(), failure);
                }
            }
        }

        // Second, extend beams as needed
        extendBeams();
    }

    //-----------------//
    // maxStemBeamGapX //
    //-----------------//
    public int maxStemBeamGapX ()
    {
        return params.maxStemBeamGapX;
    }

    //-----------------//
    // maxStemBeamGapY //
    //-----------------//
    public int maxStemBeamGapY ()
    {
        return params.maxStemBeamGapY;
    }

    //---------//
    // mergeOf //
    //---------//
    /**
     * (Try to) create a new BeamInter instance that represents a merge
     * of the provided beams.
     *
     * @param one a beam
     * @param two another beam
     * @return the resulting beam, or null if failed
     */
    public BeamInter mergeOf (BeamInter one,
                              BeamInter two)
    {
        final Line2D oneMedian = one.getMedian();
        final Line2D twoMedian = two.getMedian();

        // Height
        double oneWidth = oneMedian.getX2() - oneMedian.getX1();
        double twoWidth = twoMedian.getX2() - twoMedian.getX1();
        double height = ((one.getHeight() * oneWidth)
                         + (two.getHeight() * twoWidth)) / (oneWidth
                                                            + twoWidth);

        // Median & width
        final Line2D median;

        if (oneMedian.getX1() < twoMedian.getX1()) {
            median = new Line2D.Double(oneMedian.getP1(), twoMedian.getP2());
        } else {
            median = new Line2D.Double(twoMedian.getP1(), oneMedian.getP2());
        }

        BeamItem newItem = new BeamItem(median, height);

        if (one.isVip() || two.isVip()) {
            newItem.setVip();
        }

        Impacts impacts = computeImpacts(newItem, true, true);

        if (impacts != null) {
            return new BeamInter(null, impacts, median, height);
        } else {
            return null;
        }
    }

    //----------------//
    // checkBeamGlyph //
    //----------------//
    /**
     * Check the provided glyph as a beam candidate.
     *
     * @param glyph the glyph to check
     * @return the failure description if not successful, null otherwise
     */
    private String checkBeamGlyph (Glyph glyph)
    {
        final Rectangle box = glyph.getBounds();
        final Line glyphLine = glyph.getLine();

        if (glyph.isVip()) {
            logger.info("VIP checkBeamGlyph {}", glyph);
        }

        // Minimum width
        if (box.width < params.minBeamWidth) {
            return "too narrow";
        }

        // Minimum mean height
        final double meanHeight = glyph.getMeanThickness(
                Orientation.HORIZONTAL);

        if (meanHeight < params.minBeamHeight) {
            return "too slim";
        }

        // Maximum slope
        try {
            if (Math.abs(glyphLine.getSlope()) > params.maxBeamSlope) {
                return "too steep";
            }
        } catch (Exception ignored) {
            return "vertical";
        }

        // Check straight lines of north and south borders
        final BeamItems items = new BeamItems(
                system,
                glyph,
                params.minBeamWidth);
        final Double meanStraight = items.computeLines();

        if ((meanStraight == null)
            || (meanStraight > params.maxDistanceToBorder)) {
            return "wavy or inconsistent borders";
        }

        // Check structure width
        final double structWidth = items.getWidth();

        if (structWidth < params.minBeamWidth) {
            return "too narrow borders";
        }

        // Check that all items of the glyph are rather parallel
        double itemSlopeGap = items.compareSlopes();

        if (itemSlopeGap > params.maxBeamSlopeGap) {
            return "diverging beams";
        }

        // Adjust horizontal sides
        items.adjustSides();

        // Adjust middle lines if necessary
        items.extendMiddleLines();

        // Check stuck beams and split them if necessary
        items.splitItems();

        // Compute items grade and create BeamInter instances when acceptable
        if (createBeamInters(items)) {
            glyph.setShape(Shape.BEAM); // For visual check

            return null; // Mean: no failure
        } else {
            return "no good item";
        }
    }

    //----------------//
    // computeImpacts //
    //----------------//
    /**
     * Compute the grade details for the provided BeamItem.
     *
     * @param item  the isolated beam item
     * @param above true to check above beam item
     * @param below true to check below beam item
     * @return the impacts if successful, null otherwise
     */
    private Impacts computeImpacts (BeamItem item,
                                    boolean above,
                                    boolean below)
    {
        PixelFilter distances = sheet.getDistanceFilter();
        Area coreArea = item.getCoreArea();
        AreaMask coreMask = new AreaMask(coreArea);
        WrappedInteger core = new WrappedInteger(0);
        int coreCount = item.applyMask(coreMask, core, distances);
        double coreRatio = (double) core.value / coreCount;

        int dx = params.beltMarginDx;
        int topDy = above ? params.beltMarginDy : 0;
        int botDy = below ? params.beltMarginDy : 0;

        Area beltArea = item.getBeltArea(coreArea, dx, topDy, botDy);
        AreaMask beltMask = new AreaMask(beltArea);
        WrappedInteger belt = new WrappedInteger(0);
        int beltCount = item.applyMask(beltMask, belt, distances);
        double beltRatio = (double) belt.value / beltCount;
        int width = (int) Math.rint(
                item.median.getX2() - item.median.getX1() + 1);

        if ((width < params.minBeamWidth)
            || (coreRatio < params.minCoreBlackRatio)
            || (beltRatio > params.maxBeltBlackRatio)) {
            if (item.isVip() || logger.isDebugEnabled()) {
                logger.info(
                        "Rejected {} width:{} %core:{} %belt:{}",
                        item,
                        width,
                        String.format("%.2f", coreRatio),
                        String.format("%.2f", beltRatio));
            }

            return null;
        }

        double widthImpact = Grades.clamp(
                (width - params.minBeamWidth) / params.minLargeBeamWidth);
        double coreImpact = Grades.clamp(
                (coreRatio - params.minCoreBlackRatio) / (1
                                                          - params.minCoreBlackRatio));
        double beltImpact = Grades.clamp(
                1 - (beltRatio / params.maxBeltBlackRatio));

        return new Impacts(widthImpact, coreImpact, beltImpact);
    }

    //------------------//
    // createBeamInters //
    //------------------//
    /**
     * Create the resulting BeamInter instances, one for each good item.
     *
     * @param beamItems the items retrieved (from a glyph)
     * @return true if at least one good item was found
     */
    private boolean createBeamInters (BeamItems beamItems)
    {
        boolean success = false;
        SIGraph sig = system.getSig();

        List<BeamItem> items = beamItems.getItems();

        for (BeamItem item : items) {
            final int idx = items.indexOf(item);
            Impacts impacts = computeImpacts(
                    item,
                    idx == 0, // Check above only for first item
                    idx == (items.size() - 1)); // Check below only for last item

            if (impacts != null) {
                success = true;

                BeamInter beam = new BeamInter(
                        null,
                        impacts,
                        item.median,
                        item.height);

                if (item.isVip()) {
                    beam.setVip();
                }

                sig.addVertex(beam);
            }
        }

        return success;
    }

    //-------------//
    // extendBeams //
    //-------------//
    /**
     * Now that individual beams candidates have been extracted, try to
     * improve beam geometry (merge, extension) and detect beam groups.
     * Check whether both beam ends have a stem (seed) nearby.
     * If not, this may indicate a broken beam, so try to extend it to either
     * another beam (merge) or a stem seed (extension).
     */
    private void extendBeams ()
    {
        // The stem seeds for this system, sorted by abscissa
        sortedSystemSeeds = getSystemSeeds();

        // The beam inters for this system, NOT sorted by abscissa
        // We may add to this list, but not remove elements
        rawSystemBeams = sig.inters(Shape.BEAM);

        // Extend each orphan beam as much as possible
        for (int i = 0; i < rawSystemBeams.size(); i++) {
            final Inter inter = rawSystemBeams.get(i);

            if (inter.isDeleted()) {
                continue;
            }

            final BeamInter beam = (BeamInter) inter;

            for (HorizontalSide side : HorizontalSide.values()) {
                if (noStem(beam, side)) {
                    logger.debug("Orphan {} on {}", beam, side);

                    // This may create new beam instance.
                    if (extendToBeam(beam, side) || extendToStem(beam, side)) {
                        break;
                    }
                }
            }
        }
    }

    //--------------//
    // extendToBeam //
    //--------------//
    /**
     * Try to extend the provided beam on the desired side to another
     * beam within reach.
     *
     * @param beam the beam to extend
     * @param side the horizontal side
     * @return true if extension was done, false otherwise
     */
    private boolean extendToBeam (BeamInter beam,
                                  HorizontalSide side)
    {
        Area area = sideAreaOf(beam, side, 0, params.maxBeamsGapX, 0);
        List<Inter> others = sig.intersectedInters(
                rawSystemBeams,
                GeoOrder.NONE,
                area);

        if (!others.isEmpty()) {
            others.remove(beam); // Safer
        }

        if (!others.isEmpty()) {
            // Use a closer look, using colinearity
            final Line2D median = beam.getMedian();
            final Point2D endPt = (side == LEFT) ? median.getP1() : median.getP2();

            for (Inter ib : others) {
                BeamInter other = (BeamInter) ib;
                double dt = other.getMedian()
                        .ptLineDist(endPt);

                if (dt <= params.maxBeamsGapY) {
                    logger.debug("{} continues with {} dt:{}", beam, other, dt);

                    BeamInter newBeam = mergeOf(beam, other);

                    if (newBeam != null) {
                        sig.addVertex(newBeam);
                        rawSystemBeams.add(newBeam);
                        beam.delete();
                        other.delete();

                        return true;
                    }
                }
            }
        }

        return false;
    }

    //--------------//
    // extendToStem //
    //--------------//
    /**
     * Try to extend the provided beam on the desired side to a stem
     * seed within reach.
     *
     * @param beam the beam to extend
     * @param side the horizontal side
     * @return true if extension was done, false otherwise
     */
    private boolean extendToStem (BeamInter beam,
                                  HorizontalSide side)
    {
        Area area = sideAreaOf(
                beam,
                side,
                params.maxStemBeamGapY, // dy
                params.maxExtensionToStem, // extDx
                0); // intDx

        List<Glyph> seeds = sig.intersectedGlyphs(
                sortedSystemSeeds,
                true,
                area);

        if (!seeds.isEmpty()) {
            // Pick up the nearest stem seed
            Glyph seed = (side == LEFT) ? seeds.get(seeds.size() - 1)
                    : seeds.get(0);
            logger.debug(
                    "{} {} found stem#{} on {}",
                    beam,
                    beam.getImpacts(),
                    seed.getId(),
                    side);

            // Try to extend the beam to this stem seed
            Line2D median = beam.getMedian();
            double height = beam.getHeight();
            Line2D seedLine = new Line2D.Double(
                    seed.getStartPoint(Orientation.VERTICAL),
                    seed.getStopPoint(Orientation.VERTICAL));
            Point2D extPt = LineUtil.intersection(median, seedLine);
            final Line2D newMedian;

            if (side == LEFT) {
                newMedian = new Line2D.Double(extPt, median.getP2());
            } else {
                newMedian = new Line2D.Double(median.getP1(), extPt);
            }

            // Impacts
            BeamItem newItem = new BeamItem(newMedian, height);

            if (beam.isVip()) {
                newItem.setVip();
            }

            Impacts impacts = computeImpacts(newItem, true, true);

            if (impacts != null) {
                BeamInter newBeam = new BeamInter(
                        null,
                        impacts,
                        newMedian,
                        height);

                if (beam.isVip()) {
                    newBeam.setVip();
                }

                sig.addVertex(newBeam);
                rawSystemBeams.add(newBeam);
                beam.delete();
                logger.debug("{} {} created", newBeam, newBeam.getImpacts());

                return true;
            }
        }

        return false;
    }

    //----------//
    // getSpots //
    //----------//
    private List<Glyph> getSpots ()
    {
        // Spots for this system
        final List<Glyph> spots = new ArrayList<Glyph>();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == Shape.BEAM_SPOT) {
                spots.add(glyph);
            }
        }

        return spots;
    }

    //----------------//
    // getSystemSeeds //
    //----------------//
    /**
     * Retrieves the vertical stem seeds for the system
     *
     * @return the abscissa-ordered sequence of stem seeds in the system
     */
    private List<Glyph> getSystemSeeds ()
    {
        List<Glyph> seeds = new ArrayList<Glyph>();

        // Within a system, glyphs are sorted by abscissa
        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == Shape.VERTICAL_SEED) {
                seeds.add(glyph);
            }
        }

        return seeds;
    }

    //--------//
    // noStem //
    //--------//
    /**
     * Check whether the provided beam lacks stem seed near the
     * desired end.
     *
     * @param beam provided beam
     * @param side side of end to check
     * @return true if end is orphan
     */
    private boolean noStem (BeamInter beam,
                            HorizontalSide side)
    {
        // Define a precise check area on desired beam side and check for seed
        Area area = sideAreaOf(
                beam,
                side,
                params.maxStemBeamGapY,
                params.maxStemBeamGapX,
                params.maxStemBeamGapX);
        List<Glyph> seeds = sig.intersectedGlyphs(
                sortedSystemSeeds,
                true,
                area);

        return seeds.isEmpty();
    }

    //------------//
    // sideAreaOf //
    //------------//
    /**
     * Define an area on desired horizontal side of the beam.
     *
     * @param inter  the beam inter
     * @param side   desired side
     * @param double extDy ordinate extension
     * @param double extDx abscissa extension
     * @param double intDx abscissa offset towards beam interior
     * @return the area
     */
    private Area sideAreaOf (BeamInter inter,
                             HorizontalSide side,
                             double extDy,
                             double extDx,
                             double intDx)
    {
        final Line2D median = inter.getMedian();
        final double height = inter.getHeight() + (2 * extDy);
        final double intX = (side == LEFT) ? (median.getX1() - 1 + intDx)
                : ((median.getX2() + 1) - intDx);
        final Point2D intPt = LineUtil.intersectionAtX(median, intX);
        final double extX = (side == LEFT) ? (median.getX1() - extDx)
                : (median.getX2() + extDx);
        final Point2D extPt = LineUtil.intersectionAtX(median, extX);
        Area area = (side == LEFT)
                ? AreaUtil.horizontalParallelogram(
                        extPt,
                        intPt,
                        height) : AreaUtil.horizontalParallelogram(intPt, extPt, height);

        //TODO: we should allow attachments on Inter class!
        if (inter.getGlyph() != null) {
            inter.getGlyph()
                    .addAttachment(inter.getId() + "s" + side.ordinal(), area);
        }

        return area;
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        final Scale.Fraction minBeamWidth = new Scale.Fraction(
                1.5,
                "Minimum width for a beam");

        final Constant.Ratio minBeamHeightRatio = new Constant.Ratio(
                0.75,
                "Minimum height for a beam, specified as ratio of typical beam");

        final Scale.Fraction maxBeamsGapX = new Scale.Fraction(
                1.0,
                "Maximum abscissa gap to merge aligned beams");

        final Scale.Fraction maxBeamsGapY = new Scale.Fraction(
                0.25,
                "Maximum ordinate mismatch to merge aligned beams");

        final Scale.Fraction maxStemBeamGapX = new Scale.Fraction(
                0.2,
                "Maximum abscissa gap between stem and beam");

        final Scale.Fraction maxStemBeamGapY = new Scale.Fraction(
                0.8,
                "Maximum ordinate gap between stem and beam");

        final Scale.Fraction maxExtensionToStem = new Scale.Fraction(
                2.0,
                "Maximum beam horizontal extension to stem seed");

        final Scale.Fraction beltMarginDx = new Scale.Fraction(
                0.25,
                "Horizontal belt margin checked around beam");

        final Scale.Fraction beltMarginDy = new Scale.Fraction(
                0.15,
                "Vertical belt margin checked around beam");

        final Scale.Fraction minLargeBeamWidth = new Scale.Fraction(
                4.0,
                "Minimum width for a large beam");

        final Constant.Double maxBeamSlope = new Constant.Double(
                "tangent",
                1.0,
                "Maximum absolute tangent value for a beam angle");

        final Constant.Double maxBorderSlopeGap = new Constant.Double(
                "tangent",
                0.15,
                "Maximum delta slope between top and bottom borders of a beam");

        final Constant.Double maxBeamSlopeGap = new Constant.Double(
                "tangent",
                0.07,
                "Maximum delta slope between beams of a group");

        final Scale.Fraction maxDistanceToBorder = new Scale.Fraction(
                0.15,
                "Maximum mean distance to average beam border");

        final Constant.Ratio maxBeltBlackRatio = new Constant.Ratio(
                0.5,
                "Maximum ratio of black pixels around beam");

        final Constant.Ratio minCoreBlackRatio = new Constant.Ratio(
                0.7,
                "Minimum ratio of black pixels inside beam");

    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all pre-scaled constants.
     */
    private static class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        final int minBeamWidth;

        final double minBeamHeight;

        final int maxBeamsGapX;

        final int maxBeamsGapY;

        final int maxStemBeamGapX;

        final int maxStemBeamGapY;

        final int maxExtensionToStem;

        final int beltMarginDx;

        final int beltMarginDy;

        final double minLargeBeamWidth;

        final double maxBeamSlope;

        final double maxBorderSlopeGap;

        final double maxBeamSlopeGap;

        final double maxDistanceToBorder;

        final double maxBeltBlackRatio;

        final double minCoreBlackRatio;

        //~ Constructors -------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            minBeamWidth = scale.toPixels(constants.minBeamWidth);
            minBeamHeight = scale.getMainBeam() * constants.minBeamHeightRatio.getValue();
            maxBeamsGapX = scale.toPixels(constants.maxBeamsGapX);
            maxBeamsGapY = scale.toPixels(constants.maxBeamsGapY);
            maxStemBeamGapX = scale.toPixels(constants.maxStemBeamGapX);
            maxStemBeamGapY = scale.toPixels(constants.maxStemBeamGapY);
            maxExtensionToStem = scale.toPixels(constants.maxExtensionToStem);
            beltMarginDx = scale.toPixels(constants.beltMarginDx);
            beltMarginDy = scale.toPixels(constants.beltMarginDy);
            minLargeBeamWidth = scale.toPixels(constants.minLargeBeamWidth);
            maxBeamSlope = constants.maxBeamSlope.getValue();
            maxBorderSlopeGap = constants.maxBorderSlopeGap.getValue();
            maxBeamSlopeGap = constants.maxBeamSlopeGap.getValue();
            maxDistanceToBorder = scale.toPixelsDouble(
                    constants.maxDistanceToBorder);
            maxBeltBlackRatio = constants.maxBeltBlackRatio.getValue();
            minCoreBlackRatio = constants.minCoreBlackRatio.getValue();

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
            }
        }
    }
}
