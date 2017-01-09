//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  C h a m f e r D i s t a n c e                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
package org.audiveris.omr.image;

import ij.process.ByteProcessor;

/**
 * Class {@code ChamferDistance} implements a Distance Transform operation using
 * chamfer masks.
 *
 * @author Code by Xavier Philippeau <br> Kernels by Verwer, Borgefors and Thiel
 * @author Hervé Bitteur for interface and type-specific implementations
 */
public interface ChamferDistance
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Value when on target location. */
    public static final int VALUE_TARGET = 0;

    /** Value when on non-relevant/unknown location. */
    public static final int VALUE_UNKNOWN = -1;

    /** Chessboard mask. */
    public static final int[][] chessboard = new int[][]{
        new int[]{1, 0, 1}, new int[]{1, 1, 1}
    };

    /** 3x3 mask. */
    public static final int[][] chamfer3 = new int[][]{new int[]{1, 0, 3}, new int[]{1, 1, 4}};

    /** 5x5 mask. */
    public static final int[][] chamfer5 = new int[][]{
        new int[]{1, 0, 5}, new int[]{1, 1, 7},
        new int[]{2, 1, 11}
    };

    /** 7x7 mask. */
    public static final int[][] chamfer7 = new int[][]{
        new int[]{1, 0, 14}, new int[]{1, 1, 20},
        new int[]{2, 1, 31}, new int[]{3, 1, 44}
    };

    /** 13x13 mask. */
    public static final int[][] chamfer13 = new int[][]{
        new int[]{1, 0, 68}, new int[]{1, 1, 96},
        new int[]{2, 1, 152}, new int[]{3, 1, 215},
        new int[]{3, 2, 245}, new int[]{4, 1, 280},
        new int[]{4, 3, 340}, new int[]{5, 1, 346},
        new int[]{6, 1, 413}
    };

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // compute //
    //---------//
    /**
     * Apply the chamfer mask to the input image and return the distance transform.
     *
     * @param input the input image, where a true value indicates a reference pixel and a false
     *              value a non-reference pixel
     * @return the distance transform image, where each pixel value is the distance to the nearest
     *         reference pixel
     */
    public DistanceTable compute (boolean[][] input);

    //---------------//
    // computeToBack //
    //---------------//
    /**
     * Apply the chamfer mask to the input image and return the distance transform to
     * background pixels.
     *
     * @param input the input image, where background pixels are taken as reference pixels
     * @return the distance transform image, where each pixel value is the distance to the nearest
     *         reference pixel
     */
    DistanceTable computeToBack (ByteProcessor input);

    //---------------//
    // computeToFore //
    //---------------//
    /**
     * Apply the chamfer mask to the input image and return the distance transform to
     * foreground pixels.
     *
     * @param input the input image, where foreground pixels are taken as reference pixels
     * @return the distance transform image, where each pixel value is the distance to the nearest
     *         reference pixel
     */
    DistanceTable computeToFore (ByteProcessor input);

    //~ Inner Classes ------------------------------------------------------------------------------
    public abstract class Abstract
            implements ChamferDistance
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** The local distance mask to apply. */
        private final int[][] chamfer;

        /** Mask normalizer. */
        private final int normalizer;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new Abstract object, with chamfer3 as default mask.
         */
        public Abstract ()
        {
            this(chamfer3);
        }

        /**
         * Creates a new ChamferDistance object, using provided mask.
         *
         * @param chamfer the desired chamfer mask
         */
        public Abstract (int[][] chamfer)
        {
            this.chamfer = chamfer;
            normalizer = chamfer[0][2];
        }

        //~ Methods --------------------------------------------------------------------------------
        //---------//
        // compute //
        //---------//
        @Override
        public DistanceTable compute (boolean[][] input)
        {
            final int width = input.length;
            final int height = input[0].length;
            DistanceTable output = allocateOutput(width, height, normalizer);

            // initialize distance
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (input[x][y]) {
                        output.setValue(x, y, VALUE_TARGET); // reference pixel -> distance=0
                    } else {
                        output.setValue(x, y, VALUE_UNKNOWN); // non-reference pixel -> to be computed
                    }
                }
            }

            process(output);

            return output;
        }

        //---------------//
        // computeToBack //
        //---------------//
        @Override
        public DistanceTable computeToBack (ByteProcessor input)
        {
            DistanceTable output = allocateOutput(input.getWidth(), input.getHeight(), normalizer);
            initializeToBack(input, output);
            process(output);

            return output;
        }

        //---------------//
        // computeToFore //
        //---------------//
        @Override
        public DistanceTable computeToFore (ByteProcessor input)
        {
            DistanceTable output = allocateOutput(input.getWidth(), input.getHeight(), normalizer);
            initializeToFore(input, output);
            process(output);

            return output;
        }

        //---------//
        // process //
        //---------//
        /**
         * Run the forward and backward passes.
         *
         * @param output the output data to process
         */
        public void process (DistanceTable output)
        {
            final int width = output.getWidth();
            final int height = output.getHeight();

            // forward
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    final int v = output.getValue(x, y);

                    if (v == VALUE_UNKNOWN) {
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
                    final int v = output.getValue(x, y);

                    if (v == VALUE_UNKNOWN) {
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
        }

        /**
         * To get a Table instance of proper type and size.
         *
         * @param width  desired width
         * @param height desired height
         * @return the table of proper type and dimension
         */
        protected abstract DistanceTable allocateOutput (int width,
                                                         int height,
                                                         int normalizer);

        //------------------//
        // initializeToBack //
        //------------------//
        private void initializeToBack (ByteProcessor input,
                                       DistanceTable output)
        {
            for (int y = 0, h = input.getHeight(); y < h; y++) {
                for (int x = 0, w = input.getWidth(); x < w; x++) {
                    if (input.get(x, y) == 0) {
                        output.setValue(x, y, VALUE_UNKNOWN); // non-reference pixel -> to be computed
                    } else {
                        output.setValue(x, y, VALUE_TARGET); // reference pixel -> distance=0
                    }
                }
            }
        }

        //------------------//
        // initializeToFore //
        //------------------//
        private void initializeToFore (ByteProcessor input,
                                       DistanceTable output)
        {
            for (int i = (input.getWidth() * input.getHeight()) - 1; i >= 0; i--) {
                if (input.get(i) == 0) {
                    output.setValue(i, VALUE_TARGET); // reference pixel -> distance=0
                } else {
                    output.setValue(i, VALUE_UNKNOWN); // non-reference pixel -> to be computed
                }
            }
        }

        //------------//
        // testAndSet //
        //------------//
        private void testAndSet (DistanceTable output,
                                 int x,
                                 int y,
                                 int newvalue)
        {
            if ((x < 0) || (x >= output.getWidth())) {
                return;
            }

            if ((y < 0) || (y >= output.getHeight())) {
                return;
            }

            double v = output.getValue(x, y);

            if ((v >= 0) && (v < newvalue)) {
                return;
            }

            output.setValue(x, y, newvalue);
        }
    }

    //---------//
    // Integer //
    //---------//
    public class Integer
            extends Abstract
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        protected DistanceTable allocateOutput (int width,
                                                int height,
                                                int normalizer)
        {
            return new DistanceTable.Integer(width, height, normalizer);
        }
    }

    //-------//
    // Short //
    //-------//
    public class Short
            extends Abstract
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        protected DistanceTable allocateOutput (int width,
                                                int height,
                                                int normalizer)
        {
            return new DistanceTable.Short(width, height, normalizer);
        }
    }
}
