//----------------------------------------------------------------------------//
//                                                                            //
//                   A b s t r a c t B l o b P a t t e r n                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.ConstantSet;

import omr.glyph.Glyphs;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;
import omr.glyph.text.TextBlob;

import omr.log.Logger;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Predicate;

import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code AbstractBlobPattern} is the basis for text patterns
 * that use underlying {@link TextBlob} instances.
 * The goal is to work on glyphs and to retrieve new TEXT-shaped glyphs, which
 * will later be gathered into {@link omr.glyph.text.Sentence} instances.
 *
 * <p>Typical sequence:<ol>
 * <li> Define a set of regions within the system</li>
 * <li> Retrieve and filter the set of glyphs for each region</li>
 * <li> Separate the glyphs into small and large instances</li>
 * <li> Aggregate large glyphs into blobs</li>
 * <li> Insert small glyphs into the blobs as appropriate</li>
 * <li> Check each blob for text</li>
 * </ol>
 *
 * @see TextBorderPattern
 * @see TextGreedyPattern
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractBlobPattern
        extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
            AbstractBlobPattern.class);

    /** Shapes not accepted for candidates */
    private static final EnumSet<Shape> excludedShapes = EnumSet.copyOf(
            ShapeSet.shapesOf(TUPLET_THREE, TUPLET_SIX, BRACE, BRACKET));

    //~ Instance fields --------------------------------------------------------
    /** Number of glyphs successfully modified */
    protected int successCount = 0;

    /** Minimum text glyph height, for not being a small glyph */
    protected final int minGlyphHeight;

    /** Maximum text glyph height */
    protected final int maxGlyphHeight;

    /** Minimum blob weight */
    protected final int minBlobWeight;

    //~ Constructors -----------------------------------------------------------
    //---------------------//
    // AbstractBlobPattern //
    //---------------------//
    /**
     * Creates a new AbstractBlobPattern object.
     *
     * @param name   Unique name for this pattern
     * @param system The related system
     */
    public AbstractBlobPattern (String name,
                                SystemInfo system)
    {
        super(name, system);

        minGlyphHeight = scale.toPixels(constants.minGlyphHeight);
        maxGlyphHeight = scale.toPixels(constants.maxGlyphHeight);
        minBlobWeight = scale.toPixels(constants.minBlobWeight);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // runPattern //
    //------------//
    @Override
    public int runPattern ()
    {
        // Browse a collection of regions within the system
        for (Region region : buildRegions()) {
            // Retrieve and filter the set of glyphs for each region
            region.process();
        }

        return successCount;
    }

    //--------------//
    // buildRegions //
    //--------------//
    /**
     * Define the sequence of regions to process.
     */
    protected abstract List<? extends Region> buildRegions ();

    //--------------//
    // buildPolygon //
    //--------------//
    protected Polygon buildPolygon (List<Point> list,
                                    Point... points)
    {
        List<Point> all = new ArrayList<>(list);
        all.addAll(Arrays.asList(points));

        return buildPolygon(all);
    }

    //--------------//
    // buildPolygon //
    //--------------//
    protected Polygon buildPolygon (List<Point> list)
    {
        Polygon polygon = new Polygon();

        for (Point point : list) {
            polygon.addPoint(point.x, point.y);
        }

        return polygon;
    }

    //--------------//
    // buildPolygon //
    //--------------//
    protected Polygon buildPolygon (Point... points)
    {
        return buildPolygon(Arrays.asList(points));
    }

    //~ Inner Classes ----------------------------------------------------------
    //--------//
    // Region //
    //--------//
    protected abstract class Region
    {
        //~ Instance fields ----------------------------------------------------

        /** Name of the region (for debug) */
        protected final String name;

        /** Limits that define the region */
        protected final Polygon polygon;

        /** Blobs which can still aggregate incoming glyphs */
        protected List<TextBlob> pendingBlobs = new ArrayList<>();

        /** Blobs not impacted by remaining glyphs (too far on right) */
        protected List<TextBlob> completedBlobs = new ArrayList<>();

        /** To filter the candidates (already limited to the region) */
        protected Predicate<Glyph> additionalFilter = new Predicate<Glyph>()
        {
            @Override
            public boolean check (Glyph glyph)
            {
                return checkCandidate(glyph);
            }
        };

        //~ Constructors -------------------------------------------------------
        //--------//
        // Region //
        //--------//
        /**
         * Create a region.
         *
         * @param name    a name for this region
         * @param polygon the limits within the system (a null polygon will be
         *                a pass-through for all system glyphs)
         */
        public Region (String name,
                       Polygon polygon)
        {
            this.name = name;
            this.polygon = polygon;
        }

        //~ Methods ------------------------------------------------------------
        //---------//
        // process //
        //---------//
        public void process ()
        {
            logger.fine(
                    "{0}Pattern {1} Processing region {2}",
                    system.getLogPrefix(), AbstractBlobPattern.this.name, name);

            /** Glyphs too small to be used for initial blob definition */
            List<Glyph> smallGlyphs = new ArrayList<>();

            /** For debug */
            int blobIndex = 0;

            // Separate the glyphs into small and large ones (and discard some)
            glyphLoop:
            for (Glyph glyph : retrieveGlyphs(additionalFilter)) {
                if (isSmall(glyph)) {
                    smallGlyphs.add(glyph);
                } else {
                    // Find the first compatible blob, if any, for this large
                    for (ListIterator<TextBlob> it = pendingBlobs.listIterator();
                            it.hasNext();) {
                        TextBlob blob = it.next();

                        if (blob.canInsertLargeGlyph(glyph)) {
                            blob.insertLargeGlyph(glyph);

                            // Done for this glyph
                            continue glyphLoop;
                        } else if ((glyph.getBounds().x - blob.getRight()) > blob.
                                getMaxWordGap()) {
                            // Since glyphs are sorted by abscissa, transfer
                            // this blob from "pending" to "completed".
                            it.remove();
                            completedBlobs.add(blob);
                            logger.fine("Ending {0}", blob);
                        }
                    }

                    // No compatible blob found, so let's create a brand new one
                    pendingBlobs.add(new TextBlob(++blobIndex, system, glyph));
                }
            }

            // Terminate all blobs
            if (logger.isFineEnabled()) {
                for (TextBlob blob : pendingBlobs) {
                    logger.fine("Completing {0}", blob);
                }
            }

            completedBlobs.addAll(pendingBlobs);

            // Get rid of vertical or too light blobs!
            purgeSpuriousBlobs(completedBlobs);

            // Re-insert small glyphs into proper blobs
            insertSmallGlyphs(smallGlyphs, completedBlobs);

            // Check each of these blobs
            for (TextBlob blob : completedBlobs) {
                Glyph compound = blob.getAllowedCompound();

                if ((compound != null) && checkBlob(blob, compound)) {
                    successCount++;
                }
            }
        }

        //----------------//
        // retrieveGlyphs //
        //----------------//
        /**
         * Retrieve among the system glyphs, the ones that belong to
         * this region.
         *
         * @param filter predicate to filter the glyphs candidate
         * @return the set of system glyphs within the region
         */
        public SortedSet<Glyph> retrieveGlyphs (final Predicate<Glyph> filter)
        {
            SortedSet<Glyph> glyphs = new TreeSet<>(
                    Glyph.abscissaComparator);

            glyphs.addAll(
                    Glyphs.lookupGlyphs(
                    system.getGlyphs(),
                    new Predicate<Glyph>()
                    {
                        @Override
                        public boolean check (Glyph glyph)
                        {
                            return ((polygon == null)
                                    || polygon.contains(glyph.getBounds()))
                                    && ((filter == null)
                                        || filter.check(glyph));
                        }
                    }));

            return glyphs;
        }

        //-----------//
        // checkBlob //
        //-----------//
        /**
         * Check whether the constructed blob is actually a line of text
         *
         * @param blob     the blob to check
         * @param compound the resulting allowed glyph
         * @return true if OK
         */
        protected abstract boolean checkBlob (TextBlob blob,
                                              Glyph compound);

        //----------------//
        // checkCandidate //
        //----------------//
        protected boolean checkCandidate (Glyph glyph)
        {
            // Respect user choice!
            if (glyph.isManualShape() && !glyph.isText()) {
                return false;
            }

            // Don't go with blacklisted text
            if (glyph.isShapeForbidden(Shape.TEXT)) {
                return false;
            }

            if (excludedShapes.contains(glyph.getShape())) {
                return false;
            }

            // Discard too tall glyphs
            if (glyph.getBounds().height > maxGlyphHeight) {
                return false;
            }

            return true;
        }

        //---------//
        // isSmall //
        //---------//
        /**
         * Check whether a glyph is considered as small
         *
         * @param glyph the glyph to check
         * @return true if small
         */
        protected boolean isSmall (Glyph glyph)
        {
            // Test on weight
            if (glyph.getWeight() < minBlobWeight) {
                return true;
            }

//            // Test on height
//            if (glyph.getBounds().height < minGlyphHeight) {
//                return true;
//            }

            return false;
        }

        //-------------------//
        // insertSmallGlyphs //
        //-------------------//
        /**
         * Re-insert the small glyphs that had been left aside when
         * initially building the blobs.
         *
         * @param smallGlyphs the small glyphs to insert
         * @param blobs       the collection of blobs to update
         */
        private void insertSmallGlyphs (List<Glyph> smallGlyphs,
                                        List<TextBlob> blobs)
        {
            for (Glyph glyph : smallGlyphs) {
                // Look for the best suitable blob, if any
                Double bestDistance = null;
                TextBlob bestBlob = null;

                for (TextBlob blob : blobs) {
                    Double dist = blob.distanceTo(glyph);

                    if ((dist != null)
                            && ((bestDistance == null) || (bestDistance > dist))) {
                        bestDistance = dist;
                        bestBlob = blob;
                    }
                }

                if (bestBlob != null) {
                    logger.fine("Small glyph inserted into {0}", bestBlob);
                    bestBlob.insertSmallGlyph(glyph);
                } else {
                    logger.fine("Could not insert small {0}", glyph.idString());
                }
            }
        }

        //--------------------//
        // purgeSpuriousBlobs //
        //--------------------//
        /**
         * Remove blobs that are vertical or too small
         *
         * @param blobs the population to purge
         */
        private void purgeSpuriousBlobs (List<TextBlob> blobs)
        {
            for (Iterator<TextBlob> it = blobs.iterator(); it.hasNext();) {
                TextBlob blob = it.next();

                int blobWeight = blob.getWeight();

                if (blobWeight < minBlobWeight) {
                    logger.fine(
                            "Purged {0} weight:{1}",
                            new Object[]{
                                blob, (float) scale.pixelsToAreaFrac(blobWeight)
                            });
                    it.remove();
                } else if (blob.getAverageLine().isVertical()) {
                    logger.fine("Purged vertical {0}", blob);

                    it.remove();
                }
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction minGlyphHeight = new Scale.Fraction(
                0.8,
                "Minimum height for characters");

        Scale.Fraction maxGlyphHeight = new Scale.Fraction(
                4,
                "Maximum height for a text glyph");

        Scale.AreaFraction minBlobWeight = new Scale.AreaFraction(
                0.1,
                "Minimum normalized weight for a blob character");
    }
}
