//----------------------------------------------------------------------------//
//                                                                            //
//                   A b s t r a c t B l o b P a t t e r n                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.glyph.Glyphs;
import omr.glyph.facets.Glyph;
import omr.glyph.text.TextBlob;

import omr.log.Logger;

import omr.sheet.SystemInfo;

import omr.util.Predicate;

import java.awt.Point;
import java.awt.Polygon;
import java.util.*;

/**
 * Class {@code AbstractBlobPattern} is the basis for text patterns that use
 * underlying {@link TextBlob} instances. The goal is to work on glyphs and to
 * retrieve new TEXT-shaped glyphs, which will later be gathered into
 * {@link omr.glyph.text.Sentence} instances.
 *
 * <p>Typical sequence:<ol>
 * <li> Define a set of regions within the system</li>
 * <li> Retrieve and filter the set of glyphs for each region</li>
 * <li> Separate the glyphs into smalls and larges</li>
 * <li> Aggregate large glyphs into blobs</li>
 * <li> Try to insert small glyphs into the blobs</li>
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

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        AbstractBlobPattern.class);

    //~ Instance fields --------------------------------------------------------

    /** Number of glyphs successfully modified */
    protected int successCount = 0;

    //~ Constructors -----------------------------------------------------------

    //---------------------//
    // AbstractBlobPattern //
    //---------------------//
    /**
     * Creates a new AbstractBlobPattern object.
     *
     * @param name Unique name for this pattern
     * @param system The related system
     */
    public AbstractBlobPattern (String     name,
                                SystemInfo system)
    {
        super(name, system);
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
    // buildPolygon //
    //--------------//
    protected Polygon buildPolygon (List<Point> list,
                                    Point... points)
    {
        List<Point> all = new ArrayList<Point>(list);
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

    /** Define the sequence of regions to process */
    protected abstract List<Region> buildRegions ();

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
        protected List<TextBlob> pendingBlobs = new ArrayList<TextBlob>();

        /** Blobs not impacted by remaining glyphs (too far on right) */
        protected List<TextBlob> completedBlobs = new ArrayList<TextBlob>();

        /** Glyphs too small to be used for blob definition */
        protected List<Glyph> smallGlyphs = new ArrayList<Glyph>();

        /** To filter the candidates (already limited to the region)  */
        protected Predicate<Glyph> additionalFilter = new Predicate<Glyph>() {
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
         * @param name a name for this region
         * @param polygon the limits within the system (a null polygon will be
         * a pass-through for all system glyphs)
         */
        public Region (String  name,
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
            if (logger.isFineEnabled()) {
                logger.fine(
                    system.getLogPrefix() + "Pattern " +
                    AbstractBlobPattern.this.name + " Processing region " +
                    name);
            }

            /** For debug */
            int blobIndex = 0;

            // Separate the glyphs into small and large ones
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
                        } else if ((glyph.getContourBox().x - blob.getRight()) > blob.getMaxWordGap()) {
                            // Since glyphs are sorted by abscissa, transfer
                            // this blob from "pending" to "completed".
                            it.remove();
                            completedBlobs.add(blob);

                            if (logger.isFineEnabled()) {
                                logger.fine("Ending " + blob);
                            }
                        }
                    }

                    // No compatible blob found, so let's create a brand new one
                    pendingBlobs.add(new TextBlob(++blobIndex, system, glyph));
                }
            }

            // Terminate all blobs
            if (logger.isFineEnabled()) {
                for (TextBlob blob : pendingBlobs) {
                    logger.fine("Completing " + blob);
                }
            }

            completedBlobs.addAll(pendingBlobs);

            // Re-insert small glyphs into proper blobs
            insertSmallGlyphs(completedBlobs);

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
         * Retrieve among the system glyphs, the ones that belong to this region
         * @param filter predicate to filter the glyphs candidate
         * @return the set of system glyphs within the region
         */
        public SortedSet<Glyph> retrieveGlyphs (final Predicate filter)
        {
            SortedSet<Glyph> glyphs = new TreeSet<Glyph>(
                Glyph.globalComparator);
            glyphs.addAll(
                Glyphs.lookupGlyphs(
                    system.getGlyphs(),
                    new Predicate<Glyph>() {
                            public boolean check (Glyph glyph)
                            {
                                return ((polygon == null) ||
                                       polygon.contains(glyph.getContourBox())) &&
                                       ((filter == null) ||
                                       filter.check(glyph));
                            }
                        }));

            return glyphs;
        }

        //---------//
        // isSmall //
        //---------//
        /**
         * Check whether a glyph is considered as small
         * @param glyph the glyph to check
         * @return true if small
         */
        protected boolean isSmall (Glyph glyph)
        {
            return false; // By default, we do not put small glyphs apart
        }

        //-----------//
        // checkBlob //
        //-----------//
        /**
         * Check whether the constructed blob is actually a line of text
         * @param blob the blob to check
         * @param compound the resulting allowed glyph
         * @return true if OK
         */
        protected abstract boolean checkBlob (TextBlob blob,
                                              Glyph    compound);

        //----------------//
        // checkCandidate //
        //----------------//
        protected boolean checkCandidate (Glyph glyph)
        {
            return true;
        }

        //-------------------//
        // insertSmallGlyphs //
        //-------------------//
        /**
         * Re-insert the small glyphs that had been left aside when initially
         * building the blobs.
         * @param blobs the collection of blobs to update
         */
        private void insertSmallGlyphs (List<TextBlob> blobs)
        {
            glyphLoop: 
            for (Glyph glyph : smallGlyphs) {
                // Look for a suitable blob
                for (TextBlob blob : blobs) {
                    if (blob.tryToInsertSmallGlyph(glyph)) {
                        if (logger.isFineEnabled()) {
                            logger.fine("Small glyph inserted into " + blob);
                        }

                        continue glyphLoop;
                    }
                }

                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Could not insert small glyph#" + glyph.getId());
                }
            }
        }
    }
}
