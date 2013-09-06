//----------------------------------------------------------------------------//
//                                                                            //
//                         C h a m f e r D i s t a n c e                      //
//                                                                            //
//----------------------------------------------------------------------------//
package omr.image;

/**
 * Class {@code ChamferDistance} implements a Distance Transform
 * operation using chamfer masks.
 *
 * @author Code by Xavier Philippeau <br> Kernels by Verwer, Borgefors and Thiel
 */
public class ChamferDistance
{
    //~ Static fields/initializers ---------------------------------------------

    /** Chessboard mask. */
    public static final int[][] chessboard = new int[][]{
        new int[]{1, 0, 1},
        new int[]{1, 1, 1}
    };

    /** 3x3 mask. */
    public static final int[][] chamfer3 = new int[][]{
        new int[]{1, 0, 3},
        new int[]{1, 1, 4}
    };

    /** 5x5 mask. */
    public static final int[][] chamfer5 = new int[][]{
        new int[]{1, 0, 5},
        new int[]{1, 1, 7},
        new int[]{2, 1, 11}
    };

    /** 7x7 mask. */
    public static final int[][] chamfer7 = new int[][]{
        new int[]{1, 0, 14},
        new int[]{1, 1, 20},
        new int[]{2, 1, 31},
        new int[]{3, 1, 44}
    };

    /** 13x13 mask. */
    public static final int[][] chamfer13 = new int[][]{
        new int[]{1, 0, 68},
        new int[]{1, 1, 96},
        new int[]{2, 1, 152},
        new int[]{3, 1, 215},
        new int[]{3, 2, 245},
        new int[]{4, 1, 280},
        new int[]{4, 3, 340},
        new int[]{5, 1, 346},
        new int[]{6, 1, 413}
    };

    //~ Instance fields --------------------------------------------------------
    /** The local distance mask to apply. */
    private final int[][] chamfer;

    /** Mask normalizer. */
    private final int normalizer;

    //~ Constructors -----------------------------------------------------------
    //-----------------//
    // ChamferDistance //
    //-----------------//
    /**
     * Creates a new ChamferDistance object, with chamfer3 as default
     * mask.
     */
    public ChamferDistance ()
    {
        this(ChamferDistance.chamfer3);
    }

    //-----------------//
    // ChamferDistance //
    //-----------------//
    /**
     * Creates a new ChamferDistance object, using provided mask.
     *
     * @param chamfer the desired chamfer mask
     */
    public ChamferDistance (int[][] chamfer)
    {
        this.chamfer = chamfer;
        normalizer = chamfer[0][2];
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // compute //
    //---------//
    /**
     * Apply the chamfer mask to the input image and return the
     * distance transform.
     *
     * @param input the input image, where a true value indicates a reference
     *              pixel and a false value a non-reference pixel
     * @return the distance transform image, where each pixel value is the
     *         distance to the nearest reference pixel
     */
    public double[][] compute (boolean[][] input)
    {
        final int width = input.length;
        final int height = input[0].length;

        double[][] output = new double[width][height];

        // initialize distance
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (input[x][y]) {
                    output[x][y] = 0; // reference pixel -> distance=0
                } else {
                    output[x][y] = -1; // non-reference pixel -> to be computed
                }
            }
        }

        process(output);

        return output;
    }

    //---------//
    // compute //
    //---------//
    /**
     * Apply the chamfer mask to the input image and return the
     * distance transform.
     *
     * @param input the input image, where foreground pixels are taken as
     *              reference pixels
     * @return the distance transform image, where each pixel value is the
     *         distance to the nearest reference pixel
     */
    public double[][] compute (PixelBuffer input)
    {
        final int width = input.getWidth();
        final int height = input.getHeight();

        double[][] output = new double[width][height];

        // initialize distance
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (input.isFore(x, y)) {
                    output[x][y] = 0; // reference pixel -> distance=0
                } else {
                    output[x][y] = -1; // non-reference pixel -> to be computed
                }
            }
        }

        process(output);

        return output;
    }

    //---------//
    // process //
    //---------//
    /**
     * Run the forward and backward passes then normalize.
     *
     * @param output the output data to process
     */
    private void process (double[][] output)
    {
        final int width = output.length;
        final int height = output[0].length;

        // forward
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double v = output[x][y];

                if (v < 0) {
                    continue;
                }

                for (int k = 0; k < chamfer.length; k++) {
                    int dx = chamfer[k][0];
                    int dy = chamfer[k][1];
                    int dt = chamfer[k][2];

                    testAndSet(output, x + dx, y + dy, v + dt);

                    if (dy != 0) {
                        testAndSet(output, x - dx, y + dy, v + dt);
                    }

                    if (dx != dy) {
                        testAndSet(output, x + dy, y + dx, v + dt);

                        if (dy != 0) {
                            testAndSet(output, x - dy, y + dx, v + dt);
                        }
                    }
                }
            }
        }

        // backward
        for (int y = height - 1; y >= 0; y--) {
            for (int x = width - 1; x >= 0; x--) {
                double v = output[x][y];

                if (v < 0) {
                    continue;
                }

                for (int k = 0; k < chamfer.length; k++) {
                    int dx = chamfer[k][0];
                    int dy = chamfer[k][1];
                    int dt = chamfer[k][2];

                    testAndSet(output, x - dx, y - dy, v + dt);

                    if (dy != 0) {
                        testAndSet(output, x + dx, y - dy, v + dt);
                    }

                    if (dx != dy) {
                        testAndSet(output, x - dy, y - dx, v + dt);

                        if (dy != 0) {
                            testAndSet(output, x + dy, y - dx, v + dt);
                        }
                    }
                }
            }
        }

        // normalize
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                output[x][y] /= normalizer;
            }
        }
    }

    //------------//
    // testAndSet //
    //------------//
    private void testAndSet (double[][] output,
                             int x,
                             int y,
                             double newvalue)
    {
        if ((x < 0) || (x >= output.length)) {
            return;
        }

        if ((y < 0) || (y >= output[0].length)) {
            return;
        }

        double v = output[x][y];

        if ((v >= 0) && (v < newvalue)) {
            return;
        }

        output[x][y] = newvalue;
    }
}
