//----------------------------------------------------------------------------//
//                                                                            //
//                         S l u r I n s p e c t o r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
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
import omr.glyph.ShapeSet;
import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.lag.Section;
import omr.lag.Sections;

import omr.math.Barycenter;
import omr.math.Circle;
import omr.math.PointsCollector;
import static omr.run.Orientation.*;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.Wrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
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

    /**
     * TODO:
     * - extendSlur() should extend glyph by glyph
     * - extendSlurSection() & collectMemberSections() should be factorized
     */
    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(SlurInspector.class);

    /** Shapes suitable for extensions. */
    private static final EnumSet<Shape> extShapes = EnumSet.copyOf(
            ShapeSet.shapesOf(
            ShapeSet.Dots,
            ShapeSet.shapesOf(Shape.CLUTTER, Shape.CHARACTER,
            Shape.LEDGER, Shape.TENUTO)));

    //~ Instance fields --------------------------------------------------------
    //
    /** Compound adapter to extend slurs */
    private final SlurCompoundAdapter adapter;

    /** Scale-dependent parameters. */
    private final Parameters params;

    //~ Constructors -----------------------------------------------------------
    //---------------//
    // SlurInspector //
    //---------------//
    /**
     * Creates a new SlurInspector object.
     *
     * @param system The dedicated system
     */
    public SlurInspector (SystemInfo system)
    {
        super("Slur", system);

        adapter = new SlurCompoundAdapter(system);

        params = new Parameters(system.getSheet().getScale());
    }

    //~ Methods ----------------------------------------------------------------
    //---------------//
    // computeCircle //
    //---------------//
    /**
     * Compute the circle which best approximates the pixels of a given
     * collection of sections.
     * We use a rather simple approach, based on 3 defining points (slur ending
     * points, plus a middle point) which gives good results.
     * If resulting distance is too high (and if slur width is large enough),
     * we fall back using plain fitting on all sections points.
     *
     * @param sections the collection of sections to fit the circle upon
     * @return the best circle possible
     */
    public Circle computeCircle (Collection<? extends Section> sections)
    {
        final Rectangle box = Sections.getBounds(sections);

        // Cumulate points from sections
        PointsCollector collector = new PointsCollector(box);

        for (Section section : sections) {
            section.cumulate(collector);
        }

        int[] intXX = collector.getXValues();
        int[] intYY = collector.getYValues();
        double[] xx = new double[collector.getSize()];
        double[] yy = new double[collector.getSize()];

        for (int i = 0; i < xx.length; i++) {
            xx[i] = intXX[i];
            yy[i] = intYY[i];
        }

        // We force 3 defining points
        Point2D left = getSlurPointNearX(box.x, sections, box);
        Point2D right = getSlurPointNearX(box.x + box.width, sections, box);
        Point2D middle = getSlurPointNearX(
                box.x + (box.width / 2),
                sections,
                box);

        // Adjust middle abscissa according to slur orientation
        double slope = (right.getY() - left.getY()) / (right.getX()
                                                       - left.getX());
        Point2D inter = new Point2D.Double(
                middle.getX(),
                left.getY() + ((middle.getX() - left.getX()) * slope));
        double dy = middle.getY() - inter.getY();
        double dx = -dy * (slope / (1 + slope * slope));
        middle = getSlurPointNearX(
                box.x + (box.width / 2) + (int) Math.rint(dx),
                sections,
                box);

        Circle circle = new Circle(left, middle, right, xx, yy);

        // Switch to points fitting, if needed
        if ((circle.getDistance() > params.maxCircleDistance)) {
            logger.debug("Using total fit for slur {}", box);
            circle = new Circle(xx, yy);
        }

        return circle;
    }

    //-----------//
    // getCircle //
    //-----------//
    /**
     * Report the circle which best approximates the pixels of a given
     * glyph.
     *
     * @param glyph The glyph to fit the circle on
     * @return The best circle possible
     */
    public Circle getCircle (Glyph glyph)
    {
        Circle circle = glyph.getCircle();

        if (circle == null) {
            circle = computeCircle(glyph.getMembers());
            glyph.setCircle(circle);
        }

        return circle;
    }

    //------------//
    // runPattern //
    //------------//
    /**
     * Check all the slur glyphs in the given system, and try to
     * correct the invalid ones if any.
     *
     * @return the number of invalid slurs that are fixed
     *
     * <p><b>Synopsis:</b>
     * <pre>
     *      + extendSlur()      // attempt to get to a valid larger slur
     *          + extendSlurSections()
     *      + isValid()
     *      + trimSlur()        // attempt to get to a valid smaller slur
     *          + collectMemberSections()
     *          + detectIsolatedSections()
     *          + buildFinalSlur()
     * </pre>
     */
    @Override
    public int runPattern ()
    {
        // Make a list of all slur glyphs to be checked in this system
        // (So as to free the system glyphs list for on-the-fly modifications)
        List<Glyph> slurs = new ArrayList<>();
        int modifs = 0;

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == Shape.SLUR) {
                if (glyph.isManualShape()) {
                    glyph.addAttachment("^", getCircle(glyph).getCurve());
                } else {
                    slurs.add(glyph);
                }
            }
        }

        // First pass to extend existing slurs
        List<Glyph> toAdd = new ArrayList<>();

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
                if (logger.isDebugEnabled()) {
                    logger.info("{}Abnormal curve slur#{}",
                            system.getSheet().getLogPrefix(), slur.getId());
                }

                slur.setShape(null);
                it.remove();
                modifs++;
            } catch (Exception ex) {
                logger.warn("Error in extending slur#" + slur.getId(), ex);
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
                    logger.warn(
                            "Error in trimming slur#" + slur.getId(),
                            ex);
                }

                modifs++;
            } else if (logger.isDebugEnabled()) {
                logger.debug("Valid slur {}", slur.getId());
                slur.addAttachment("^", getCircle(slur).getCurve());
            }
        }

        return modifs;
    }

    //----------//
    // trimSlur //
    //----------//
    /**
     * For large glyphs, we suspect a slur with a stuck object,
     * so the strategy is to rebuild the true Slur portions from the
     * underlying sections.
     *
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
        if (oldSlur.isVip() || logger.isDebugEnabled()) {
            logger.info("Trimming slur {}", oldSlur.idString());
        }

        // Get a COPY of the member list, sorted by decreasing weight */
        List<Section> members = new ArrayList<>(oldSlur.getMembers());
        Collections.sort(members, Section.reverseWeightComparator);

        // Find the suitable seed
        Wrapper<Double> seedDist = new Wrapper<>();
        Section seedSection = findSeedSection(members, seedDist);

        // If no significant section has been found, just give up
        if (seedSection == null) {
            if (oldSlur.getShape() == Shape.SLUR) {
                oldSlur.setShape(null);
            }

            return null;
        }

        // Sections collected (including seedSection)
        List<Section> collected = collectMemberSections(
                members,
                seedSection,
                seedDist.value);

        // Sections left over
        List<Section> left = new ArrayList<>(members);
        left.removeAll(collected);

        // Sections too far from the other ones
        List<Section> isolated = detectIsolatedSections(seedSection, collected);
        collected.removeAll(isolated);
        left.addAll(isolated);

        if (!collected.isEmpty()) {
            Glyph newSlur = null;

            try {
                // Make sure we do have a suitable slur
                newSlur = buildFinalSlur(collected);

                if (newSlur != null) {
                    if (oldSlur.isVip() || logger.isDebugEnabled()) {
                        logger.info("Trimmed slur #{} as smaller #{}",
                                oldSlur.getId(), newSlur.getId());
                    }
                } else {
                    if (oldSlur.isVip() || logger.isDebugEnabled()) {
                        logger.info("Giving up slur #{} w/ {}",
                                oldSlur.getId(), collected);
                    }

                    left.addAll(collected);
                }

                return newSlur;
            } catch (Exception ex) {
                left.addAll(collected);

                return null;
            } finally {
                // Remove former oldSlur glyph
                if (oldSlur != newSlur) {
                    oldSlur.setShape(null);

                    // Free the sections left over (useful???)
                    for (Section section : left) {
                        section.setGlyph(null);
                    }
                }
            }
        } else {
            logger.warn("{} No section left when trimming slur #{}",
                    system.getScoreSystem().getContextString(), oldSlur.getId());

            return null;
        }
    }

    //----------------//
    // buildFinalSlur //
    //----------------//
    /**
     * Try to build a valid slur from a collection of sections.
     *
     * @param sections the slur sections
     * @return the valid slur if any, null otherwise
     */
    private Glyph buildFinalSlur (List<Section> sections)
    {
        if (null == getInvalidity(sections, null)) {
            // Build new slur glyph with sections kept
            Glyph newGlyph = new BasicGlyph(params.interline);

            for (Section section : sections) {
                newGlyph.addSection(section, Glyph.Linking.LINK_BACK);
            }

            // Beware, the newGlyph may now belong to a different system
            SystemInfo newSystem = system.getSheet().getSystemOf(newGlyph);

            // Check whether SLUR is not forbidden for this glyph
            newGlyph = newSystem.registerGlyph(newGlyph);

            if (newGlyph.isShapeForbidden(Shape.SLUR)) {
                return null;
            }

            newGlyph = newSystem.addGlyph(newGlyph);
            newGlyph.setShape(Shape.SLUR);

            newGlyph.addAttachment("^", getCircle(newGlyph).getCurve());

            return newGlyph;
        } else {
            return null;
        }
    }

    //-----------------------//
    // collectMemberSections //
    //-----------------------//
    /**
     * From the provided members, find all sections well located
     * on the slur circle, including the seed section.
     * We start from the best seed section, then grow incrementally with
     * compatible sections, continuously checking distance to resulting circle.
     *
     * @param members      the glyph sections
     * @param seedSection  the starting seed section
     * @param lastDistance the fitting distance to current circle
     * @return the list of sections collected (including seed section)
     */
    private List<Section> collectMemberSections (List<Section> members,
                                                 Section seedSection,
                                                 double lastDistance)
    {
        List<Section> collected = new ArrayList<>();

        // We impose the seed
        collected.add(seedSection);

        for (Section section : members) {
            section.setProcessed(false);
        }

        // Let's grow the seed incrementally as much as possible
        Rectangle slurBox = seedSection.getBounds();
        seedSection.setProcessed(true);

        boolean growing = true;

        while (growing) {
            growing = false;

            for (Section section : members) {
                if (section.isProcessed()) {
                    continue;
                }

                // Need connection
                Rectangle sctBox = section.getBounds();
                sctBox.grow(1, 1);

                if (!sctBox.intersects(slurBox)) {
                    continue;
                }

                logger.debug("Trying {}", section);

                // Try a circle
                List<Section> config = new ArrayList<>(collected);
                config.add(section);

                try {
                    Circle circle = computeCircle(config);
                    double distance = circle.getDistance();
                    logger.debug("dist={}", distance);

                    if (distance <= extendedDistance(lastDistance)) {
                        collected.add(section);
                        lastDistance = distance;
                        section.setProcessed(true);
                        slurBox.add(section.getBounds());
                        growing = true;
                        logger.debug("Keep {}", section);
                    } else {
                        logger.debug("Discard {}", section);
                    }
                } catch (Exception ex) {
                    logger.debug("{} w/ {}", ex.getMessage(), section);
                }
            }
        }

        return collected;
    }

    //------------------------//
    // detectIsolatedSections //
    //------------------------//
    /**
     * Detect any section which is too far from the other ones.
     *
     * @param seedSection the initial seed section
     * @param collected   the sections collected, including seed section
     * @return the collection of isolated sections found
     */
    private List<Section> detectIsolatedSections (Section seedSection,
                                                  List<Section> collected)
    {
        final List<Section> isolated = new ArrayList<>(collected);
        final Rectangle slurBox = seedSection.getBounds();
        boolean makingProgress;

        do {
            makingProgress = false;

            for (Iterator<Section> it = isolated.iterator(); it.hasNext();) {
                Section section = it.next();
                Rectangle sectBox = section.getBounds();
                sectBox.grow(params.slurBoxDx, params.slurBoxDy);

                if (sectBox.intersects(slurBox)) {
                    slurBox.add(sectBox);
                    it.remove();
                    makingProgress = true;
                }
            }
        } while (makingProgress);

        return isolated;
    }

    //------------//
    // extendSlur //
    //------------//
    /**
     * Try to build a compound glyph with compatible neighboring
     * glyphs, and test the validity of the resulting slur.
     *
     * @param root the slur glyph to extend
     * @return the extended slur glyph if any, or null. A non-null glyph
     *         is returned IFF we have found a slur which is both larger than
     *         the initial slur and valid.
     */
    private Glyph extendSlur (Glyph root)
    {
        // The best compound obtained so far
        Glyph bestSlur = null;

        // Loop on extensions, left then right sides
        for (HorizontalSide side : HorizontalSide.values()) {
            // Extend as far as possible on the desired side
            adapter.setSide(side);

            SideLoop:
            while (true) {
                if (root.isVip() || logger.isDebugEnabled()) {
                    logger.info("Trying to {} extend slur #{}",
                            side, root.getId());
                }

                // Look at neighboring glyphs (TODO: should be incremental?)
                Glyph compound = system.buildCompound(
                        root,
                        true, // include seed
                        system.getGlyphs(),
                        adapter);

                if (compound != null) {
                    if (root.isVip() || logger.isDebugEnabled()) {
                        logger.info("Slur #{} {} extended as #{}",
                                root.getId(), side, compound.getId());

                        if (root.isVip()) {
                            compound.setVip();
                        }
                    }

                    bestSlur = compound;
                    root = compound;
                } else {
                    // Look at neighboring sections
                    Glyph sectSlur = extendSlurSections(root, side);

                    if (sectSlur != null) {
                        if (root.isVip() || logger.isDebugEnabled()) {
                            logger.info("sectSlur: {}", sectSlur);
                        }

                        bestSlur = sectSlur;
                    }

                    break SideLoop; // We are through on this side
                }
            }
        }

        return bestSlur;
    }

    //--------------------//
    // extendSlurSections //
    //--------------------//
    /**
     * Try to extend the provided slur with neighboring sections on
     * the provided side.
     * Starting from the slur seed, we incrementally aggregate compatible
     * sections, sorted according to their distance to slur ending point.
     * The process is stopped at the first failed attempt.
     *
     * @param root the slur glyph to extend
     * @return the extended slur glyph if any, or null. A non-null glyph
     *         is returned IFF we have found a slur which is both larger than the
     *         initial slur and valid.
     */
    private Glyph extendSlurSections (Glyph root,
                                      HorizontalSide side)
    {
        // The best compound obtained so far
        Glyph bestSlur = null;

        List<Section> sections = new ArrayList<>();
        sections.addAll(system.getHorizontalSections());
        sections.addAll(system.getVerticalSections());

        for (Section section : sections) {
            Glyph glyph = section.getGlyph();

            // Discard manual sections
            if ((glyph != null) && glyph.isManualShape()) {
                section.setProcessed(true);
            } else {
                section.setProcessed(false);
            }
        }

        for (Section section : root.getMembers()) {
            section.setProcessed(true);
        }

        // Initial conditions
        adapter.setSide(side);

        // Loop on extensions
        boolean growing = true;

        while (growing) {
            growing = false;

            if (root.isVip() || logger.isDebugEnabled()) {
                logger.info("Trying to section-extend slur #{}", root.getId());
            }

            // Process that slur, looking at neighboring sections
            if (adapter.setSeed(root) == null) {
                logger.warn("Null reference box");
            }

            // Retrieve good neighbors among the suitable sections
            List<Section> neighbors = new ArrayList<>();

            for (Section section : sections) {
                if (section.isVip()) {
                    logger.debug("Section {}", section);
                }

                if (!section.isProcessed()) {
                    if (adapter.isSectionClose(section)
                        && adapter.isSectionSuitable(section)) {
                        neighbors.add(section);
                        section.setProcessed(true);
                    }
                }
            }

            // Let's try neighbors incrementally
            if (!neighbors.isEmpty()) {
                // Sort neighbors according to their distance from slur ending
                Collections.sort(neighbors, adapter.sectionComparator);

                // Sections effectively added
                List<Section> added = new ArrayList<>();

                for (Section section : neighbors) {
                    added.add(section);

                    // slur config = seed sections + added sections
                    List<Section> config = new ArrayList<>(added);
                    config.addAll(root.getMembers());

                    boolean sectionOk = false;
                    double distance = computeCircle(config).getDistance();
                    logger.debug("dist={}", distance);

                    if (distance <= adapter.extendedDistance()) {
                        Glyph compound = system.buildTransientGlyph(config);

                        if (adapter.isCompoundValid(compound)) {
                            // Assign and insert into system & nest environments
                            compound = system.addGlyph(compound);
                            compound.setEvaluation(
                                    adapter.getChosenEvaluation());

                            if (root.isVip() || logger.isDebugEnabled()) {
                                logger.info(
                                        "Slur #{} extended as #{} with {}",
                                        root.getId(), compound.getId(),
                                        Sections.toString(added));

                                if (root.isVip()) {
                                    compound.setVip();
                                }
                            }

                            bestSlur = compound;
                            root = compound;
                            adapter.setSeed(root);
                            growing = true;
                            sectionOk = true;
                        }
                    }

                    if (!sectionOk) {
                        if (root.isVip() || logger.isDebugEnabled()) {
                            logger.info("Slur #{} excluding section#{}",
                                    root.getId(), section);
                        }

                        break;
                    }
                }
            } else {
                return bestSlur;
            }
        }

        return bestSlur;
    }

    //-----------------//
    // findSeedSection //
    //-----------------//
    /**
     * Find the best seed, which is chosen as the section with best
     * circle distance among the sections whose weight is significant.
     *
     * @param sortedMembers the candidate sections, by decreasing weight
     * @param seedDist      (output) the distance measured for chosen seed
     * @return the suitable seed, perhaps null
     */
    private Section findSeedSection (List<Section> sortedMembers,
                                     Wrapper<Double> seedDist)
    {
        Section seedSection = null;
        seedDist.value = Double.MAX_VALUE;

        for (Section seed : sortedMembers) {
            // Check minimum weight
            int weight = seed.getWeight();

            if (weight < params.minChunkWeight) {
                break; // Since sections are sorted
            }

            // Check meanthickness
            double thickness = Math.min(
                    seed.getMeanThickness(VERTICAL),
                    seed.getMeanThickness(HORIZONTAL));

            if (thickness > params.maxChunkThickness) {
                continue;
            }

            Circle circle = computeCircle(Arrays.asList(seed));
            double dist = circle.getDistance();

            if ((dist <= params.maxCircleDistance) && (dist < seedDist.value)) {
                seedDist.value = dist;
                seedSection = seed;
            }
        }

        if (logger.isDebugEnabled()) {
            if (seedSection == null) {
                logger.debug("No suitable seed section found");
            } else {
                logger.debug("Seed section is {} dist:{}",
                        seedSection, seedDist.value);
            }
        }

        return seedSection;
    }

    //---------------//
    // getInvalidity //
    //---------------//
    /**
     * Check validity of a collection of sections as a slur.
     *
     * @param sections  the provided sections
     * @param resulting circle if already known
     * @return null if OK, otherwise the cause of invalidity
     */
    private Object getInvalidity (Collection<Section> sections,
                                  Circle circle)
    {
        if (circle == null) {
            circle = computeCircle(sections);
        }

        // Check distance to circle
        double dist = circle.getDistance();

        if (dist > params.maxCircleDistance) {
            return "distance " + (float) dist + " vs " + params.maxCircleDistance;
        }

        // Check curve is computable
        if (circle.getCurve() == null) {
            return "no curve";
        }

        // Check radius 
        double radius = circle.getRadius();

        if (radius < params.minCircleRadius) {
            return "small radius " + (float) radius + " vs " + params.minCircleRadius;
        }

        if (radius > params.maxCircleRadius) {
            return "large radius " + (float) radius + " vs " + params.maxCircleRadius;
        }

        //        // Check curve bounds are rather close to slur box
        //        Rectangle curveBox = circle.getCurve()
        //                                   .getBounds();
        //
        //        double    heightRatio = (double) curveBox.height / contourBox.height;
        //
        //        if (heightRatio > constants.maxHeightRatio.getValue()) {
        //            if (logger.isDebugEnabled()) {
        //                logger.info(
        //                    "Too high ratio: " + (float) heightRatio +
        //                    " for curve box " + curveBox);
        //            }
        //
        //            return false;
        //        }
        return null;
    }

    //-------------------//
    // getSlurPointNearX //
    //-------------------//
    /**
     * Retrieve the best slur point near the provided abscissa.
     *
     * @param x        the provided abscissa
     * @param sections the slur sections
     * @param box      the slur bounding box
     * @return the best approximating point
     */
    private Point2D getSlurPointNearX (int x,
                                       Collection<? extends Section> sections,
                                       Rectangle box)
    {
        Rectangle roi = new Rectangle(x, box.y, 0, box.height);
        Barycenter bary;

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
     *
     * @param glyph the glyph to check
     * @return true if valid
     */
    private boolean isValid (Glyph slur)
    {
        // Make sure we are not trying to reassign a blacklisted shape
        if (slur.isShapeForbidden(Shape.SLUR)) {
            return false;
        }

        Object cause = getInvalidity(slur.getMembers(), slur.getCircle());

        if (slur.isVip()) {
            if (cause != null) {
                logger.info("Invalid slur #{} : {}", slur.getId(), cause);
            } else {
                logger.info("Valid slur #{}", slur.getId());
            }
        }

        return cause == null;
    }

    //------------------//
    // extendedDistance //
    //------------------//
    /**
     * Report the maximum extended circle distance, knowing the
     * circle distance of the current slur.
     *
     * @param lastDistance current slur fitting distance
     * @return extended maximum distance
     */
    private double extendedDistance (double lastDistance)
    {
        double ratio = constants.distanceExtensionRatio.getValue();
        return lastDistance + ratio * (params.maxCircleDistance - lastDistance);
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    //----------------------//
    // NoSlurCurveException //
    //----------------------//
    /**
     * Used to signal an abnormal "slur" glyph, for which the curve
     * cannot be computed or is degenerated to a straight line.
     */
    private static class NoSlurCurveException
            extends RuntimeException
    {
    }

    //---------------------//
    // SlurCompoundAdapter //
    //---------------------//
    /**
     * CompoundAdapter meant to process the extension of a slur.
     */
    private class SlurCompoundAdapter
            extends CompoundBuilder.AbstractAdapter
    {
        //~ Instance fields ----------------------------------------------------

        // Underlying slur curve 
        protected CubicCurve2D curve;

        // Current fitting distance
        protected double distance;

        // Current extension side
        protected HorizontalSide side;

        // Current slur ending point
        protected Point2D endPt;

        /** To sort sections according to the distance to slur end */
        public Comparator<Section> sectionComparator = new Comparator<Section>()
        {
            @Override
            public int compare (Section s1,
                                Section s2)
            {
                // We use distance from section to adapter end point
                return Double.compare(toEndSq(s1), toEndSq(s2));
            }
        };

        //~ Constructors -------------------------------------------------------
        public SlurCompoundAdapter (SystemInfo system)
        {
            // Note: minGrade value (0d) is irrelevant, since compound validity
            // will be checked against specific slur characteristics rather
            // than evaluation grade.
            super(system, 0d);
        }

        //~ Methods ------------------------------------------------------------
        /**
         * Compute the extension box on the provided side.
         *
         * @return the extension box
         * @see #setSide
         */
        @Override
        public Rectangle computeReferenceBox ()
        {
            Rectangle sBox = seed.getBounds(); // Seed box
            boolean isShort = sBox.width <= params.minSlurWidth;
            Point2D cp; // Related control point

            if (isShort) {
                // For short glyphs, circle/curve are not reliable
                // so we use approximating line instead.
                endPt = (side == LEFT) ? seed.getStartPoint(HORIZONTAL)
                        : seed.getStopPoint(HORIZONTAL);
                cp = (side == LEFT) ? seed.getStopPoint(HORIZONTAL)
                        : seed.getStartPoint(HORIZONTAL);
            } else {
                endPt = (side == LEFT) ? curve.getP1() : curve.getP2();
                cp = (side == LEFT) ? curve.getCtrlP1() : curve.getCtrlP2();
            }

            // Exact ending point (?)
            Rectangle roi = (side == LEFT)
                    ? new Rectangle(sBox.x, sBox.y, 1, sBox.height)
                    : new Rectangle((sBox.x + sBox.width) - 1, sBox.y, 1, sBox.height);
            Point2D ep = seed.getRectangleCentroid(roi);
            if (ep != null) {
                if (side == RIGHT) {
                    ep.setLocation(ep.getX() + 1, ep.getY());
                }
            } else {
                ep = endPt; // Better than nothing
            }

            StaffInfo staff = system.getStaffAt(endPt);
            if (staff == null) {
                // Weird case, where the slur crosses system boundaries
                Point2D otherEnd = (side == LEFT) ? curve.getP2() : curve.getP1();
                staff = system.getStaffAt(otherEnd);
            }

            // Is the slur end touching a staff line?
            final double pitch = staff.pitchPositionOf(endPt);
            final int intPitch = (int) Math.rint(pitch);
            double target;
            if ((Math.abs(intPitch) <= 4) && ((intPitch % 2) == 0)) {
                // TODO: beware of vertical 
                double slope = (ep.getY() - cp.getY()) / (ep.getX() - cp.getX());

                if (Math.abs(slope) <= params.maxTangentSlope) {
                    // This end touches a staff line, with horizontal tangent
                    target = params.targetLineTangentHypot;
                } else {
                    // This end touches a staff line not horizontally
                    target = params.targetLineHypot;
                }
            } else {
                // No staff line is involved, use smaller margins
                target = params.targetHypot;
            }

            Point2D cp2pt = new Point2D.Double(
                    endPt.getX() - cp.getX(),
                    endPt.getY() - cp.getY());
            double hypot = Math.hypot(cp2pt.getX(), cp2pt.getY());
            double lambda = target / hypot;
            Point2D ext = new Point2D.Double(
                    ep.getX() + (lambda * cp2pt.getX()),
                    ep.getY() + (lambda * cp2pt.getY()));

            Rectangle rect = new Rectangle(
                    (int) Math.rint(Math.min(ext.getX(), ep.getX())),
                    (int) Math.rint(Math.min(ext.getY(), ep.getY())),
                    (int) Math.rint(Math.abs(ext.getX() - ep.getX())),
                    (int) Math.rint(Math.abs(ext.getY() - ep.getY())));

            // Ensure minimum box height
            if (rect.height < params.minExtensionHeight) {
                rect.grow(
                        0,
                        1
                        + (int) Math.rint(
                        (params.minExtensionHeight - rect.height) / 2.0));
            }

            seed.addAttachment(((side == LEFT) ? "e^" : "^e"), rect);

            return rect;
        }

        @Override
        public boolean isCandidateSuitable (Glyph glyph)
        {
            if (!glyph.isActive()) {
                return false; // Safer
            }

            // Check mean thickness
            double thickness = Math.min(
                    glyph.getMeanThickness(VERTICAL),
                    glyph.getMeanThickness(HORIZONTAL));

            if (thickness > params.maxChunkThickness) {
                return false;
            }

            // Check minimum weight
            if (glyph.getWeight() < params.minExtensionWeight) {
                return false;
            }

            // Check shape
            if (!glyph.isKnown()) {
                return true;
            }

            Shape shape = glyph.getShape();

            if ((shape == Shape.SLUR) && !glyph.isManualShape()) {
                return true;
            }

            return (!glyph.isManualShape() && extShapes.contains(shape))
                   || (glyph.getGrade() <= Grades.compoundPartMaxGrade);
        }

        @Override
        public boolean isCompoundValid (Glyph compound)
        {
            if (isValid(compound)) {
                // Is distance still OK?
                double compoundDistance = getCircle(compound).getDistance();
                if (compoundDistance <= extendedDistance()) {
                    chosenEvaluation = new Evaluation(
                            Shape.SLUR,
                            Evaluation.ALGORITHM);

                    return true;
                } else {
                    logger.debug("{} Degrading distance {} vs {}",
                            seed, compoundDistance, extendedDistance());
                }
            }

            return false;
        }

        public boolean isSectionClose (Section section)
        {
            return box.intersects(section.getBounds());
        }

        public boolean isSectionSuitable (Section section)
        {
            Glyph glyph = section.getGlyph();

            if ((glyph != null) && !glyph.isActive()) {
                return false; // Safer
            }

            // Check meanthickness
            double thickness = Math.min(
                    section.getMeanThickness(VERTICAL),
                    section.getMeanThickness(HORIZONTAL));

            if (thickness > params.maxChunkThickness) {
                return false;
            }

            if ((glyph == null) || !glyph.isKnown()) {
                // Check section weight
                if (section.getWeight() >= params.minExtensionWeight) {
                    return true;
                } else {
                    return false;
                }
            }

            Shape shape = glyph.getShape();

            if (ShapeSet.Barlines.contains(shape) || (shape == Shape.SLUR)) {
                return false;
            }

            if (glyph.isManualShape()) {
                return false;
            }

            // Check shape grade
            if (glyph.getGrade() > Grades.compoundPartMaxGrade) {
                return false;
            }

            return true;
        }

        @Override
        public Rectangle setSeed (Glyph seed)
        {
            box = null;

            // Side-effect: compute underlying curve
            Circle circle = getCircle(seed);

            if (circle.getRadius().isInfinite()) {
                throw new NoSlurCurveException();
            }

            curve = circle.getCurve();

            if (curve == null) {
                throw new NoSlurCurveException();
            } else {
                seed.addAttachment("^", curve);
            }

            // Side-effect: compute current circle distance
            distance = circle.getDistance();

            return super.setSeed(seed);
        }

        /**
         * Remember the desired extension side.
         *
         * @param side the desired side
         */
        public void setSide (HorizontalSide side)
        {
            this.side = side;
        }

        /**
         * Report the maximum extended circle distance.
         *
         * @return extended maximum distance
         */
        private double extendedDistance ()
        {
            return SlurInspector.this.extendedDistance(distance);
        }

        /**
         * Report the (square) distance from the slur ending point to
         * the provided section, according to the current side.
         *
         * @param section the provided section
         * @return the square distance
         */
        private double toEndSq (Section section)
        {
            Rectangle b = section.getBounds();

            if (side == LEFT) {
                // Use box right vertical
                return new Line2D.Double(
                        b.x + b.width,
                        b.y,
                        b.x + b.width,
                        b.y + b.height).ptSegDistSq(endPt);
            } else {
                // Use box left vertical
                return new Line2D.Double(b.x, b.y, b.x, b.y + b.height).
                        ptSegDistSq(endPt);
            }
        }
    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        final int interline;

        final int minChunkWeight;

        final int minExtensionWeight;

        final double maxChunkThickness;

        final int slurBoxDx;

        final int slurBoxDy;

        final int targetHypot;

        final int targetLineHypot;

        final int targetLineTangentHypot;

        final int minSlurWidth;

        final int minExtensionHeight;

        final double maxCircleDistance;

        final double minCircleRadius;

        final double maxCircleRadius;

        final double maxTangentSlope;

        //~ Constructors -------------------------------------------------------
        public Parameters (Scale scale)
        {
            interline = scale.getInterline();
            minChunkWeight = scale.toPixels(constants.minChunkWeight);
            minExtensionWeight = scale.toPixels(constants.minExtensionWeight);
            maxChunkThickness = scale.toPixels(constants.maxChunkThickness);
            slurBoxDx = scale.toPixels(constants.slurBoxDx);
            slurBoxDy = scale.toPixels(constants.slurBoxDy);
            targetHypot = scale.toPixels(constants.slurBoxHypot);
            targetLineHypot = scale.toPixels(constants.slurLineBoxHypot);
            targetLineTangentHypot = scale.toPixels(
                    constants.slurLineTangentBoxHypot);
            minSlurWidth = scale.toPixels(constants.minSlurWidth);
            minExtensionHeight = scale.toPixels(constants.minExtensionHeight);
            maxCircleDistance = scale.toPixelsDouble(constants.maxCircleDistance);
            minCircleRadius = scale.toPixels(constants.minCircleRadius);
            maxCircleRadius = scale.toPixels(constants.maxCircleRadius);
            maxTangentSlope = constants.maxTangentSlope.getValue();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction maxCircleDistance = new Scale.Fraction(
                0.006,
                "Maximum distance to approximating circle for a slur");

        Scale.Fraction minCircleRadius = new Scale.Fraction(
                0.7,
                "Minimum circle radius for a slur");

        Scale.Fraction maxCircleRadius = new Scale.Fraction(
                100,
                "Maximum circle radius for a slur");

        Scale.AreaFraction minChunkWeight = new Scale.AreaFraction(
                0.3,
                "Minimum weight of a chunk to be part of slur computation");

        Scale.AreaFraction minExtensionWeight = new Scale.AreaFraction(
                0.01,
                "Minimum weight of a glyph to be considered for slur extension");

        Scale.Fraction slurBoxDx = new Scale.Fraction(
                0.7,
                "Extension abscissa when looking for slur compound");

        Scale.Fraction slurBoxDy = new Scale.Fraction(
                0.4,
                "Extension ordinate when looking for slur compound");

        Scale.Fraction slurBoxHypot = new Scale.Fraction(
                0.9,
                "Extension length when looking for line-free slur compound");

        Scale.Fraction slurLineBoxHypot = new Scale.Fraction(
                1.5,
                "Extension length when looking for line-touching slur compound");

        Scale.Fraction slurLineTangentBoxHypot = new Scale.Fraction(
                3.0,
                "Extension length when looking for line-tangent slur compound");

        Scale.Fraction minSlurWidth = new Scale.Fraction(
                2,
                "Minimum width to use curve rather than line for extension");

        Scale.LineFraction minExtensionHeight = new Scale.LineFraction(
                2,
                "Minimum height for extension box, specified as LineFraction");

        Scale.LineFraction maxChunkThickness = new Scale.LineFraction(
                2,
                "Maximum mean thickness of a chunk to be part of slur computation");

        Constant.Ratio maxHeightRatio = new Constant.Ratio(
                2.0,
                "Maximum height ratio between curve height and glyph height");

        Constant.Double maxTangentSlope = new Constant.Double(
                "tangent",
                0.05,
                "Maximum slope for staff line tangent");

        Constant.Ratio distanceExtensionRatio = new Constant.Ratio(
                0.67,
                "Acceptable distance extension to maximum limit");

    }
}
