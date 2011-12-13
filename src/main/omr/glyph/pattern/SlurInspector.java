//----------------------------------------------------------------------------//
//                                                                            //
//                         S l u r I n s p e c t o r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.CompoundBuilder;
import omr.glyph.Evaluation;
import omr.glyph.Grades;
import omr.glyph.Shape;
import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.lag.Section;
import omr.lag.Sections;

import omr.log.Logger;

import omr.math.Barycenter;
import omr.math.Circle;
import omr.math.PointsCollector;
import static omr.run.Orientation.*;

import omr.score.common.PixelRectangle;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Implement;
import omr.util.Wrapper;

import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code SlurInspector} encapsulates physical processing 
 * dedicated to inspection at system level of glyphs with SLUR shape.
 *
 * @author Hervé Bitteur
 */
public class SlurInspector
    extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SlurInspector.class);

    //~ Instance fields --------------------------------------------------------

    /** Adapter to extend slurs */
    private final SlurCompoundAdapter compoundAdapter;

    // Cached system-dependent constants
    private final int    interline;
    private final int    minChunkWeight;
    private final double maxChunkThickness;
    private final int    slurBoxDx;
    private final int    slurBoxDy;
    private final int    targetHypot;
    private final int    targetLineHypot;
    private final int    minSlurWidth;
    private final int    minExtensionHeight;
    private final int    largeSlurWidth;
    private final double maxCircleDistance;
    private final double minCircleRadius;
    private final double maxCircleRadius;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // SlurInspector //
    //---------------//
    /**
     * Creates a new SlurInspector object.
     * @param system The dedicated system
     */
    public SlurInspector (SystemInfo system)
    {
        super("Slur", system);

        compoundAdapter = new SlurCompoundAdapter(system);

        // Compute scale-dependent parameters
        interline = scale.getInterline();
        minChunkWeight = scale.toPixels(constants.minChunkWeight);
        maxChunkThickness = scale.toPixels(constants.maxChunkThickness);
        slurBoxDx = scale.toPixels(constants.slurBoxDx);
        slurBoxDy = scale.toPixels(constants.slurBoxDy);
        targetHypot = scale.toPixels(constants.slurBoxHypot);
        targetLineHypot = scale.toPixels(constants.slurLineBoxHypot);
        minSlurWidth = scale.toPixels(constants.minSlurWidth);
        minExtensionHeight = scale.toPixels(constants.minExtensionHeight);
        maxCircleDistance = scale.toPixelsDouble(constants.maxCircleDistance);
        minCircleRadius = scale.toPixels(constants.minCircleRadius);
        maxCircleRadius = scale.toPixels(constants.maxCircleRadius);
        largeSlurWidth = scale.toPixels(constants.largeSlurWidth);
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // computeCircle //
    //---------------//
    /**
     * Compute the Circle which best approximates the pixels of a given
     * glyph.
     * @param glyph The glyph to fit the circle on
     * @return The best circle possible
     */
    public Circle computeCircle (Glyph glyph)
    {
        return computeCircle(glyph.getMembers());
    }

    //---------------//
    // computeCircle //
    //---------------//
    /**
     * Compute the Circle which best approximates the pixels of a given
     * collection of sections.
     * @param sections The collection of sections to fit the circle upon
     * @return The best circle possible
     */
    public Circle computeCircle (Collection<?extends Section> sections)
    {
        final PixelRectangle box = Sections.getContourBox(sections);

        // Cumulate points from sections
        PointsCollector collector = new PointsCollector(box);

        for (Section section : sections) {
            section.cumulate(collector);
        }

        int[]    intXX = collector.getXValues();
        int[]    intYY = collector.getYValues();
        double[] xx = new double[collector.getSize()];
        double[] yy = new double[collector.getSize()];

        for (int i = 0; i < xx.length; i++) {
            xx[i] = intXX[i];
            yy[i] = intYY[i];
        }

        // We force 3 defining points
        Point2D left = getSlurPointNear(box.x, sections, box);
        Point2D right = getSlurPointNear(box.x + box.width, sections, box);
        Point2D middle = getSlurPointNear(
            box.x + (box.width / 2),
            sections,
            box);

        // Adjust middle abscissa according to slur orientation
        double  slope = (right.getY() - left.getY()) / (right.getX() -
                                                       left.getX());
        Point2D inter = new Point2D.Double(
            middle.getX(),
            left.getY() + ((middle.getX() - left.getX()) * slope));
        double  dy = middle.getY() - inter.getY();
        double  dx = -dy * slope;
        middle = getSlurPointNear(
            box.x + (box.width / 2) + (int) Math.rint(dx),
            sections,
            box);

        Circle circle = new Circle(left, middle, right, xx, yy);

        // Switch to points fitting, if needed
        if ((circle.getDistance() > maxCircleDistance) &&
            (box.width > largeSlurWidth)) {
            if (logger.isFineEnabled()) {
                logger.info("Using total fit for slur " + box);
            }

            circle = new Circle(xx, yy);
        }

        return circle;
    }

    //------------//
    // runPattern //
    //------------//
    /**
     * Check all the slur glyphs in the given system, and try to
     * correct the invalid ones if any.
     * @return the number of invalid slurs that are fixed
     *
     * <p><b>Synopsis:</b>
     * <pre>
     *      + extendSlur()      // attempt to get to a valid larger slur
     *      + isValid()
     *      + trimSlur()        // attempt to get to a valid smaller slur
     *          + findSectionsOnCircle()
     *          + removeIsolatedSections()
     *          + buildFinalSlur()
     * </pre>
     */
    @Implement(GlyphPattern.class)
    public int runPattern ()
    {
        // Make a list of all slur glyphs to be checked in this system
        // (So as to free the system glyphs list for on-the-fly modifications)
        List<Glyph> slurs = new ArrayList<Glyph>();
        int         modifs = 0;

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == Shape.SLUR) {
                if (glyph.isManualShape()) {
                    glyph.addAttachment("^", computeCircle(glyph).getCurve());
                } else {
                    slurs.add(glyph);
                }
            }
        }

        // First pass to extend existing slurs
        List<Glyph> toAdd = new ArrayList<Glyph>();

        for (Iterator<Glyph> it = slurs.iterator(); it.hasNext();) {
            Glyph slur = it.next();

            // Skip slurs just been 'merged' with another one
            if (!slur.isActive()) {
                continue;
            }

            // Extend this slur as much as possible
            try {
                Glyph largerSlur = extendSlur(slur);

                if (largerSlur != null) {
                    toAdd.add(largerSlur);
                    it.remove();
                    modifs++;
                }
            } catch (NoSlurCurveException ex) {
                if (logger.isFineEnabled()) {
                    logger.info(
                        system.getSheet().getLogPrefix() +
                        "Abnormal curve slur #" + slur.getId());
                }

                slur.setShape(null);
                it.remove();
                modifs++;
            } catch (Exception ex) {
                logger.warning("Error in extending slur", ex);
            }
        }

        slurs.addAll(toAdd);

        // Second pass to check each slur validity
        for (Glyph slur : slurs) {
            // Skip slurs just been 'merged' with another one
            if (!slur.isActive()) {
                continue;
            }

            // Check slur validity
            if (!isValid(slur)) {
                // Extension has already been tried to no avail
                // So, just try to trim the slur down
                try {
                    if (trimSlur(slur) == null) {
                        slur.setShape(null);
                    }
                } catch (Exception ex) {
                    logger.warning("Error in triming slur#" + slur.getId(), ex);
                }

                modifs++;
            } else if (logger.isFineEnabled()) {
                logger.finest("Valid slur " + slur.getId());
                slur.addAttachment("^", computeCircle(slur).getCurve());
            }
        }

        // Debug
        //        for (Glyph glyph : system.getGlyphs()) {
        //            if (glyph.isActive() && (glyph.getShape() == Shape.SLUR)) {
        //                logger.info(computeCircle(glyph) + " for G#" + glyph.getId());
        //            }
        //        }

        //
        return modifs;
    }

    //----------//
    // trimSlur //
    //----------//
    /**
     * For large glyphs, we suspect a slur with a stuck object,
     * so the strategy is to rebuild the true Slur portions from the
     * underlying sections.
     * @param oldSlur the spurious slur
     * @return the extracted slur glyph, if any
     */
    public Glyph trimSlur (Glyph oldSlur)
    {
        /**
         * Sections are first ordered by decreasing weight and 
         * continuously tested via the distance to the best 
         * approximating circle.  
         * Sections whose weight is under a given threshold are appended to the
         * slur only if the resulting circle distance gets lower.
         *
         * The "good" sections are put into the "kept" collection.
         * Sections left over are put into the "left" collection in order to be
         * used to rebuild the stuck object(s).
         */
        if (oldSlur.isVip() || logger.isFineEnabled()) {
            logger.info("Trimming slur #" + oldSlur.getId());
        }

        // Get a COPY of the member list, sorted by decreasing weight */
        List<Section> members = new ArrayList<Section>(oldSlur.getMembers());
        Collections.sort(members, Section.reverseWeightComparator);

        // Find the suitable seed
        Wrapper<Double> seedDist = new Wrapper<Double>();
        Section         seedSection = findSeedSection(members, seedDist);

        // If no significant section has been found, just give up
        if (seedSection == null) {
            oldSlur.setShape(null);

            return null;
        }

        List<Section> kept = new ArrayList<Section>();
        List<Section> left = new ArrayList<Section>();

        findSectionsOnCircle(members, seedSection, seedDist.value, kept, left);
        removeIsolatedSections(seedSection, kept, left);

        if (!kept.isEmpty()) {
            Glyph newSlur = null;

            try {
                // Make sure we do have a suitable slur
                newSlur = buildFinalSlur(kept);

                if (newSlur != null) {
                    if (oldSlur.isVip() || logger.isFineEnabled()) {
                        logger.info(
                            "Trimmed slur #" + oldSlur.getId() +
                            " as smaller #" + newSlur.getId());
                    }
                } else {
                    if (oldSlur.isVip() || logger.isFineEnabled()) {
                        logger.info(
                            "Giving up slur #" + oldSlur.getId() + " w/ " +
                            kept);
                    }

                    left.addAll(kept);
                }

                return newSlur;
            } catch (Exception ex) {
                left.addAll(kept);

                return null;
            } finally {
                // Remove former oldSlur glyph
                if (oldSlur != newSlur) {
                    oldSlur.setShape(null);
                    system.removeGlyph(oldSlur);

                    // Free the sections left over (useful???)
                    for (Section section : left) {
                        section.setGlyph(null);
                    }
                }
            }
        } else {
            logger.warning(
                system.getScoreSystem().getContextString() +
                " No section left when trimming slur #" + oldSlur.getId());

            return null;
        }
    }

    //------------------//
    // getSlurPointNear //
    //------------------//
    /**
     * Retrieve the best slur point near the provided abscissa.
     * @param x the provided abscissa
     * @param sections the slur sections
     * @param box the slur bounding box
     * @return the best approximating point
     */
    private Point2D.Double getSlurPointNear (int                          x,
                                             Collection<?extends Section> sections,
                                             PixelRectangle               box)
    {
        PixelRectangle roi = new PixelRectangle(x, box.y, 0, box.height);
        Barycenter     bary;

        do {
            bary = new Barycenter();
            roi.grow(1, 0);

            for (Section section : sections) {
                section.cumulate(bary, roi);
            }
        } while (bary.getWeight() == 0);

        return new Point2D.Double(bary.getX(), bary.getY());
    }

    //---------//
    // isValid //
    //---------//
    /**
     * Check validity of a glyph as a slur.
     * @param glyph the glyph to check
     * @return true if valid
     */
    private boolean isValid (Glyph slur)
    {
        if (slur.isVip()) {
            logger.info("Checking validity of slur#" + slur.getId());
        }

        return isValid(slur.getMembers());
    }

    //---------//
    // isValid //
    //---------//
    /**
     * Check validity of a collection of sections as a slur.
     * @param sections the provided sections
     * @return true if valid
     */
    private boolean isValid (Collection<Section> sections)
    {
        Circle circle = computeCircle(sections);

        // Check distance to circle
        if (!circle.isValid(maxCircleDistance)) {
            return false;
        }

        // Check radius 
        double radius = circle.getRadius();

        if (radius < minCircleRadius) {
            if (logger.isFineEnabled()) {
                logger.info(
                    "Too small radius " + (float) radius + " vs " +
                    minCircleRadius);
            }

            return false;
        }

        if (radius > maxCircleRadius) {
            if (logger.isFineEnabled()) {
                logger.info(
                    "Too large radius " + (float) radius + " vs " +
                    maxCircleRadius);
            }

            return false;
        }

        //        // Check curve bounds are rather close to slur box
        //        Rectangle curveBox = circle.getCurve()
        //                                   .getBounds();
        //
        //        double    heightRatio = (double) curveBox.height / contourBox.height;
        //
        //        if (heightRatio > constants.maxHeightRatio.getValue()) {
        //            if (logger.isFineEnabled()) {
        //                logger.info(
        //                    "Too high ratio: " + (float) heightRatio +
        //                    " for curve box " + curveBox);
        //            }
        //
        //            return false;
        //        }
        return true;
    }

    //----------------//
    // buildFinalSlur //
    //----------------//
    /**
     * Try to build a valid slur from the provided collection of sections
     * @param sections the slur sections
     * @return the valid slur if any, null otherwise
     */
    private Glyph buildFinalSlur (List<Section> sections)
    {
        if (isValid(sections)) {
            // Build new slur glyph with sections kept
            Glyph newGlyph = new BasicGlyph(interline);

            for (Section section : sections) {
                newGlyph.addSection(section, Glyph.Linking.LINK_BACK);
            }

            // Beware, the newGlyph may now belong to a different system
            SystemInfo newSystem = system.getSheet()
                                         .getSystemOf(newGlyph);
            newGlyph = newSystem.addGlyph(newGlyph);
            newGlyph.setShape(Shape.SLUR);

            newGlyph.addAttachment("^", computeCircle(newGlyph).getCurve());

            return newGlyph;
        } else {
            return null;
        }
    }

    //------------//
    // extendSlur //
    //------------//
    /**
     * Try to build a compound glyph with compatible neighboring glyphs,
     * and test the validity of the resulting slur.
     * @param root the slur glyph to extend
     * @return the extended slur glyph if any, or null. A non-null glyph
     * is returned IFF we have found a slur which is both larger than the
     * initial slur and valid.
     */
    private Glyph extendSlur (Glyph root)
    {
        // The best compound obtained so far
        Glyph bestSlur = null;

        // Loop on extensions
        while (true) {
            if (root.isVip() || logger.isFineEnabled()) {
                logger.info("Trying to extend slur #" + root.getId());
            }

            Glyph       compound = null;

            // Collect glyphs suitable for participating in compound building
            List<Glyph> suitables = new ArrayList<Glyph>(
                system.getGlyphs().size());

            for (Glyph g : system.getGlyphs()) {
                if ((g != root) && compoundAdapter.isCandidateSuitable(g)) {
                    suitables.add(g);
                }
            }

            // Sort suitable glyphs by decreasing weight
            Collections.sort(suitables, Glyph.reverseWeightComparator);

            // Process that slur, looking at neighbors
            compound = system.buildCompound(
                root,
                true, // include seed
                suitables,
                compoundAdapter);

            if (compound != null) {
                if (root.isVip() || logger.isFineEnabled()) {
                    logger.info(
                        "Slur #" + root.getId() + " extended as #" +
                        compound.getId());

                    if (root.isVip()) {
                        compound.setVip();
                    }
                }

                bestSlur = compound;
                root = compound;
            } else {
                return bestSlur;
            }
        }
    }

    //----------------------//
    // findSectionsOnCircle //
    //----------------------//
    /**
     * From the provided members, find all sections which are well
     * located on the slur circle.
     * @param members
     * @param seedSection
     * @param seedDist
     * @param kept
     * @param left
     */
    private void findSectionsOnCircle (List<Section> members,
                                       Section       seedSection,
                                       double        seedDist,
                                       List<Section> kept,
                                       List<Section> left)
    {
        // We impose the seed
        kept.add(seedSection);

        List<Section> tried = new ArrayList<Section>();
        double        distThreshold = seedDist;
        double        lastDistance = distThreshold;

        for (Section section : members) {
            section.setProcessed(false);
        }

        // First, let's grow the seed incrementally as much as possible
        PixelRectangle slurBox = seedSection.getContourBox();
        seedSection.setProcessed(true);

        boolean growing = true;

        while (growing) {
            growing = false;

            for (Section section : members) {
                if (section.isProcessed()) {
                    continue;
                }

                // Need connection
                PixelRectangle sctBox = section.getContourBox();
                sctBox.grow(1, 1);

                if (!sctBox.intersects(slurBox)) {
                    continue;
                }

                distThreshold = (lastDistance + maxCircleDistance) / 2;

                if (logger.isFineEnabled()) {
                    logger.fine("Trying " + section);
                }

                // Try a circle
                tried.clear();
                tried.addAll(kept);
                tried.add(section);

                try {
                    Circle circle = computeCircle(tried);
                    double distance = circle.getDistance();

                    if (logger.isFineEnabled()) {
                        logger.fine("dist=" + distance);
                    }

                    if (distance <= distThreshold) {
                        kept.add(section);
                        lastDistance = distance;
                        section.setProcessed(true);
                        slurBox.add(section.getContourBox());
                        growing = true;

                        if (logger.isFineEnabled()) {
                            logger.fine("Keep " + section);
                        }
                    } else {
                        if (logger.isFineEnabled()) {
                            logger.fine("Discard " + section);
                        }
                    }
                } catch (Exception ex) {
                    if (logger.isFineEnabled()) {
                        logger.fine(ex.getMessage() + " w/ " + section);
                    }
                }
            }
        }

        // Update left collection
        List<Section> list = new ArrayList<Section>(members);
        list.removeAll(kept);
        left.addAll(list);
    }

    //-----------------//
    // findSeedSection //
    //-----------------//
    /**
     * Find the best seed, which is chosen as the section with best circle
     * distance among the sections whose weight is significant.
     * @param sortedMembers the candidate sections, by decreasing weight
     * @param seedDist (output) the distance measured for chosen seed
     * @return the suitable seed, perhaps null
     */
    private Section findSeedSection (List<Section>   sortedMembers,
                                     Wrapper<Double> seedDist)
    {
        Section seedSection = null;
        seedDist.value = Double.MAX_VALUE;

        for (Section seed : sortedMembers) {
            // Check minimum weight
            int weight = seed.getWeight();

            if (weight < minChunkWeight) {
                break; // Since sections are sorted
            }

            // Check meanthickness
            double thickness = Math.min(
                seed.getMeanThickness(VERTICAL),
                seed.getMeanThickness(HORIZONTAL));

            if (thickness > maxChunkThickness) {
                continue;
            }

            Circle circle = computeCircle(Arrays.asList(seed));
            double dist = circle.getDistance();

            if ((dist <= maxCircleDistance) && (dist < seedDist.value)) {
                seedDist.value = dist;
                seedSection = seed;
            }
        }

        if (logger.isFineEnabled()) {
            if (seedSection == null) {
                logger.fine("No suitable seed section found");
            } else {
                logger.fine(
                    "Seed section is " + seedSection + " dist:" +
                    seedDist.value);
            }
        }

        return seedSection;
    }

    //------------------------//
    // removeIsolatedSections //
    //------------------------//
    /**
     * Remove any section which is too far from the other ones
     * @param seedSection
     * @param kept
     * @param left
     */
    private void removeIsolatedSections (Section       seedSection,
                                         List<Section> kept,
                                         List<Section> left)
    {
        PixelRectangle slurBox = seedSection.getContourBox();
        List<Section>  toCheck = new ArrayList<Section>(kept);
        toCheck.remove(seedSection);
        kept.clear();
        kept.add(seedSection);

        boolean makingProgress;

        do {
            makingProgress = false;

            for (Iterator<Section> it = toCheck.iterator(); it.hasNext();) {
                Section        section = it.next();
                PixelRectangle sectBox = section.getContourBox();
                sectBox.grow(slurBoxDx, slurBoxDy);

                if (sectBox.intersects(slurBox)) {
                    slurBox.add(sectBox);
                    it.remove();
                    kept.add(section);
                    makingProgress = true;
                }
            }
        } while (makingProgress);

        left.addAll(toCheck);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        //
        Scale.LineFraction maxCircleDistance = new Scale.LineFraction(
            0.04,
            "Maximum distance to approximating circle for a slur");

        //
        Scale.Fraction     minCircleRadius = new Scale.Fraction(
            1,
            "Minimum circle radius for a slur");

        //
        Scale.Fraction     maxCircleRadius = new Scale.Fraction(
            100,
            "Maximum circle radius for a slur");

        //
        Scale.Fraction     largeSlurWidth = new Scale.Fraction(
            30,
            "Minimum width to detect a really large slur");

        //
        Scale.AreaFraction minChunkWeight = new Scale.AreaFraction(
            0.3, //0.5,
            "Minimum weight of a chunk to be part of slur computation");

        //
        Scale.Fraction     slurBoxDx = new Scale.Fraction(
            0.7,
            "Extension abscissa when looking for slur compound");

        //
        Scale.Fraction     slurBoxDy = new Scale.Fraction(
            0.4,
            "Extension ordinate when looking for slur compound");

        //
        Scale.Fraction     slurBoxHypot = new Scale.Fraction(
            0.75,
            "Extension length when looking for line-free slur compound");

        //
        Scale.Fraction     slurLineBoxHypot = new Scale.Fraction(
            1.5,
            "Extension length when looking for line-touching slur compound");

        //
        Scale.Fraction     minSlurWidth = new Scale.Fraction(
            2,
            "Minimum width to use curve rather than line for extension");

        //
        Scale.LineFraction minExtensionHeight = new Scale.LineFraction(
            2,
            "Minimum height for extension box, specified as LineFraction");

        //
        Scale.LineFraction maxChunkThickness = new Scale.LineFraction(
            1.7,
            "Maximum mean thickness of a chunk to be part of slur computation");

        //
        Constant.Ratio maxHeightRatio = new Constant.Ratio(
            2.0,
            "Maximum height ratio between curve height and glyph height");
    }

    //----------------------//
    // NoSlurCurveException //
    //----------------------//
    /**
     * Used to signal an abnormal "slur" glyph, for which the curve cannot be
     * computed.
     */
    private static class NoSlurCurveException
        extends RuntimeException
    {
    }

    //---------------------//
    // SlurCompoundAdapter //
    //---------------------//
    /**
     * Class {@code SlurCompoundAdapter} is a CompoundAdapter meant to process
     * the extension of a slur.
     */
    private class SlurCompoundAdapter
        extends CompoundBuilder.AbstractAdapter
    {
        //~ Instance fields ----------------------------------------------------

        CubicCurve2D   curve;

        // Boxes at each slur end
        PixelRectangle startBox;
        PixelRectangle stopBox;

        //~ Constructors -------------------------------------------------------

        public SlurCompoundAdapter (SystemInfo system)
        {
            super(system, 0d); // Value is irrelevant
        }

        //~ Methods ------------------------------------------------------------

        @Implement(CompoundBuilder.CompoundAdapter.class)
        public boolean isCandidateSuitable (Glyph glyph)
        {
            if (!glyph.isActive()) {
                return false; // Safer
            }

            // Check meanthickness
            double thickness = Math.min(
                glyph.getMeanThickness(VERTICAL),
                glyph.getMeanThickness(HORIZONTAL));

            if (thickness > maxChunkThickness) {
                return false;
            }

            if (!glyph.isKnown()) {
                return true;
            }

            Shape shape = glyph.getShape();

            if ((shape == Shape.SLUR) && !glyph.isManualShape()) {
                return true;
            }

            return (!glyph.isManualShape() &&
                   ((shape == Shape.CLUTTER) || (shape == Shape.STRUCTURE))) ||
                   (glyph.getGrade() <= Grades.compoundPartMaxGrade);
        }

        @Implement(CompoundBuilder.CompoundAdapter.class)
        public boolean isCompoundValid (Glyph compound)
        {
            if (isValid(compound)) {
                chosenEvaluation = new Evaluation(
                    Shape.SLUR,
                    Evaluation.ALGORITHM);

                return true;
            }

            return false;
        }

        /**
         * Specific distance test, adapted at slur shape crossing staff lines
         * @param box unused, we use two boxes instead, one at start point and
         * one at stop point
         * @param glyph candidate glyph
         * @return true if close
         */
        @Override
        public boolean isGlyphClose (PixelRectangle box,
                                     Glyph          glyph)
        {
            PixelRectangle glyphBox = glyph.getContourBox();

            return startBox.intersects(glyphBox) ||
                   stopBox.intersects(glyphBox);
        }

        @Implement(CompoundBuilder.CompoundAdapter.class)
        public PixelRectangle getReferenceBox ()
        {
            return null; // void
        }

        @Override
        public void setSeed (Glyph seed)
        {
            super.setSeed(seed);

            final Circle circle = computeCircle(seed);
            curve = circle.getCurve();

            if (curve == null) {
                throw new NoSlurCurveException();
            } else {
                seed.addAttachment("^", curve);
            }

            startBox = getBox(-1);
            stopBox = getBox(+1);
        }

        /**
         * Compute the extension box in the provided direction.
         * Nota: for short glyphs (width less than 2 interline) circle/curve are
         * not reliable and we must use approximating line instead.
         * @param dir -1 for start, +1 for stop
         * @return the extension box
         */
        private PixelRectangle getBox (int dir)
        {
            PixelRectangle seedBox = seed.getContourBox();
            boolean        isShort = seedBox.width <= minSlurWidth;
            Point2D        p; // Ending point
            Point2D        c; // Control point

            if (isShort) {
                p = (dir < 0) ? seed.getStartPoint(HORIZONTAL)
                    : seed.getStopPoint(HORIZONTAL);
                c = (dir < 0) ? seed.getStopPoint(HORIZONTAL)
                    : seed.getStartPoint(HORIZONTAL);
            } else {
                p = (dir < 0) ? curve.getP1() : curve.getP2();
                c = (dir < 0) ? curve.getCtrlP1() : curve.getCtrlP2();
            }

            // Exact ending point (?)
            PixelRectangle roi = (dir > 0)
                                 ? new PixelRectangle(
                (seedBox.x + seedBox.width) - 1,
                seedBox.y,
                1,
                seedBox.height)
                                 : new PixelRectangle(
                seedBox.x,
                seedBox.y,
                1,
                seedBox.height);

            Point2D        ep = seed.getRectangleCentroid(roi);

            if (ep != null) {
                if (dir > 0) {
                    ep.setLocation(ep.getX() + 1, ep.getY());
                }
            } else {
                ep = p; // Better than nothing
            }

            final StaffInfo staff = system.getStaffAt(p);
            final double    pitch = staff.pitchPositionOf(p);
            final int       intPitch = (int) Math.rint(pitch);

            double          target;
            PixelRectangle  box;

            if ((Math.abs(intPitch) <= 4) && ((intPitch % 2) == 0)) {
                // This end touches a staff line, use larger margins
                target = targetLineHypot;
            } else {
                // No staff line is involved, use smaller margins
                target = targetHypot;
            }

            Point2D cp = new Point2D.Double(
                p.getX() - c.getX(),
                p.getY() - c.getY());
            double  hypot = Math.hypot(cp.getX(), cp.getY());
            double  lambda = target / hypot;
            Point2D ext = new Point2D.Double(
                ep.getX() + (lambda * cp.getX()),
                ep.getY() + (lambda * cp.getY()));

            box = new PixelRectangle(
                (int) Math.rint(Math.min(ext.getX(), ep.getX())),
                (int) Math.rint(Math.min(ext.getY(), ep.getY())),
                (int) Math.rint(Math.abs(ext.getX() - ep.getX())),
                (int) Math.rint(Math.abs(ext.getY() - ep.getY())));

            // Ensure minimum box height
            if (box.height < minExtensionHeight) {
                box.grow(
                    0,
                    (int) Math.rint((minExtensionHeight - box.height) / 2.0));
            }

            seed.addAttachment(((dir < 0) ? "e^" : "^e"), box);

            return box;
        }
    }
}
