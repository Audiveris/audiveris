//----------------------------------------------------------------------------//
//                                                                            //
//                       W a t e r s h e d G r a y L e v e l                  //
//                                                                            //
//----------------------------------------------------------------------------//
package omr.image;

import java.util.LinkedList;

/**
 * Class {@code WatershedGrayLevel} implements Gray-Level Watershed
 * Segmentation
 *
 * @author Xavier Philippeau
 */
public class WatershedGrayLevel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Number of gray level values. */
    private static final int GRAYLEVEL = 256;

    /** Specific gray value to indicate a watershed pixel. */
    private static final int WATERSHED = -1;

    /** Abscissa offsets of the 8 neighbors, clockwise. */
    private static final int[] dx8 = new int[]{-1, 0, 1, 1, 1, 0, -1, -1};

    /** Ordinate offsets of the 8 neighbors, clockwise. */
    private static final int[] dy8 = new int[]{-1, -1, -1, 0, 1, 1, 1, 0};

    //~ Instance fields --------------------------------------------------------
    /** Original gray-level image, organized row per row. */
    private Table image;

    /** Image width. */
    private final int width;

    /** Image height. */
    private final int height;

    /**
     * Region map, parallel to original image.
     * For each pixel location (x,y), gives the region id counted from 1.
     * 0 means not assigned yet
     * -1 means part of a watershed line
     */
    private Table rmap;

    /** Maximum region id so far. */
    private int maxRegionId;

    /** List of pixels (one per level) to process. */
    private ListOfPixels[] exploreList;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new WatershedGrayLevel object.
     *
     * @param image          the image to process, organized row per row
     * @param isBrightOnDark true for bright foreground on dark background,
     *                       false otherwise.
     *                       Note that if isBrightOnDark is true, the original
     *                       image will be inverted in place.
     */
    public WatershedGrayLevel (Table image,
                               boolean isBrightOnDark)
    {
        this.image = image;
        width = image.getWidth();
        height = image.getHeight();

        // Invert image if needed
        if (isBrightOnDark) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    image.setValue(x, y, 255 - image.getValue(x, y));
                }
            }
        }
    }

    //~ Methods ----------------------------------------------------------------
    //----------------//
    // getRegionCount //
    //----------------//
    /**
     * Report the number of regions identified.
     *
     * @return the number of regions
     */
    public int getRegionCount ()
    {
        return maxRegionId;
    }

    //---------//
    // process //
    //---------//
    /**
     * Build the boolean watershed map.
     *
     * @param step number of levels to check
     * @return the watershed map (a true boolean value indicates a pixel which
     *         is part of watershed line)
     */
    public boolean[][] process (int step)
    {
        init();

        // Flooding level by level
        int level = 0;
        int yoffset = 0;

        while (level < GRAYLEVEL) {
            // Extend region by exploring neighbors of known pixels
            while (true) {
                Pixel p = nextPixel(level, step);

                if (p == null) {
                    break;
                }

                extend(p);
            }

            // Find a new seed for this level
            Pixel seed = findSeed(level, yoffset);

            if (seed != null) {
                ///System.out.println("seed = " + seed);
                // create and assign a new region to this seed
                rmap.setValue(seed.x, seed.y, ++maxRegionId);
                yoffset = seed.y;

                // add this seed to the list of pixel to explore
                exploreList[level].add(seed);
            } else {
                // no more seed for this level -> next level
                level++;
                yoffset = 0;
            }
        }

        // Build the watershed map
        ///dumpRmap();
        boolean[][] shedmap = new boolean[width][height];

        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                if (rmap.getValue(x, y) == WATERSHED) {
                    shedmap[x][y] = true;
                }
            }
        }

        // free memory
        clear();

        // return the watershed map
        return shedmap;
    }

    //-------//
    // clear //
    //-------//
    // free memory
    private void clear ()
    {
        rmap = null;
        exploreList = null;
    }

    private void dumpRmap ()
    {
        System.out.println("rmap:");

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                System.out.printf("%3d", rmap.getValue(x, y));
            }

            System.out.println();
        }
    }

    //--------//
    // extend //
    //--------//
    /**
     * Explore the 8 neighbors of a pixel and set its region
     * accordingly.
     *
     * @param p the pixel to explore
     */
    private void extend (Pixel p)
    {
        int region = rmap.getValue(p.x, p.y);

        // this pixel is a watershed => cannot extend it 
        if (region == WATERSHED) {
            return;
        }

        // for each neighbor pixel
        for (int k = 0; k < 8; k++) {
            int xk = p.x + dx8[k];
            int yk = p.y + dy8[k];

            if ((xk < 0) || (xk >= this.width)) {
                continue;
            }

            if ((yk < 0) || (yk >= this.height)) {
                continue;
            }

            // Level and region for this neighbor
            int vk = image.getValue(xk, yk);
            int rk = rmap.getValue(xk, yk);

            // Neighbor is a watershed => ignore
            if (rk == WATERSHED) {
                continue;
            }

            // Neighbor has no region assigned => set it
            if (rk == 0) {
                rmap.setValue(xk, yk, region);
                exploreList[vk].add(new Pixel(xk, yk, vk));

                continue;
            }

            // Neighbor is assigned to the same region => nothing to do
            if (rk == region) {
                continue;
            }

            // Neighbor is assigned to another region => it's a watershed
            rmap.setValue(xk, yk, WATERSHED);
        }
    }

    //----------//
    // findSeed //
    //----------//
    /**
     * Find a seed ( = unassigned pixel ) at the specified level.
     *
     * @param level the specified level
     * @param yoffset offset on ordinate
     * @return the first seed found, or null
     */
    private Pixel findSeed (int level,
                            int yoffset)
    {
        for (int y = yoffset; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((image.getValue(x, y) == level)
                    && (rmap.getValue(x, y) == 0)) {
                    return new Pixel(x, y, level);
                }
            }
        }

        return null;
    }

    //------//
    // init //
    //------//
    /** Allocate memory. */
    private void init ()
    {
        maxRegionId = 0;

        rmap = new Table.Short(width, height);

        exploreList = new ListOfPixels[GRAYLEVEL];

        for (int i = 0; i < GRAYLEVEL; i++) {
            exploreList[i] = new ListOfPixels();
        }
    }

    //-----------//
    // nextPixel //
    //-----------//
    /**
     * Retrieve the next pixel to explore.
     *
     * @param level
     * @param step
     * @return the next pixel to explore
     */
    private Pixel nextPixel (int level,
                             int step)
    {
        // Return the first pixel found in the exploreList
        for (int i = level; (i < (level + step)) && (i < GRAYLEVEL); i++) {
            if (!exploreList[i].isEmpty()) {
                return exploreList[i].remove(0);
            }
        }

        return null;
    }

    //~ Inner Classes ----------------------------------------------------------
    //--------------//
    // ListOfPixels //
    //--------------//
    /**
     * Collection of pixels.
     */
    private static class ListOfPixels
            extends LinkedList<Pixel>
    {
    }

    //-------//
    // Pixel //
    //-------//
    /**
     * Information (location, level) on a specific pixel.
     */
    private static class Pixel
    {
        //~ Instance fields ----------------------------------------------------

        int x;

        int y;

        int level;

        //~ Constructors -------------------------------------------------------
        public Pixel (int x,
                      int y,
                      int level)
        {
            this.x = x;
            this.y = y;
            this.level = level;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "{pixel x:" + x + " y:" + y + " level:" + level + "}";
        }
    }
}
/*
 * int[][] image; // le tableau contenant les valeurs (entre 0 et 255) de
 * l'image
 * int width, height; // les dimension du tableau ci-avant
 * boolean isBrightOnDark; // "true" si on cherche des objets clairs sur un fond
 * foncé, sinon "false"
 * // création de l'instance
 * WatershedGrayLevel ws = new
 * WatershedGrayLevel(image,width,height,isBrightOnDark);
 * // appel de l'algorithme
 * int step; // nombre des niveaux voisins a explorer à chaque remplissage
 * boolean shed[][] = ws.process(step);
 * // le tableau shed[][] contient "true" si le pixel correspondant est sur une
 * ligne de partage des eaux
 */
