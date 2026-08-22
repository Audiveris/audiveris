//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    M o r p h o P r o c e s s o r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
package org.audiveris.omr.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

import ij.process.ByteProcessor;

/**
 * Class <code>MorphoProcessor</code>
 *
 * @author ?
 */
public class MorphoProcessor
        implements MorphoConstants
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(MorphoProcessor.class);

    public static final int BINF = -256;

    private static final int ORIG = 0;

    private static final int PLUS = 1;

    private static final int MINUS = -1;

    private static final String LIBRARY_PROPERTY = "audiveris.morpho.library";

    private static final String WORKER_PROPERTY = "audiveris.morpho.worker";

    private static final String NATIVE_SYMBOL = "musicspace_morpho_close_v1";

    private static final int PROBE_RECORD_BYTES = 12;

    private static final long MAX_NATIVE_BYTES = 64L * 1024 * 1024;

    private static final Arena NATIVE_ARENA = Arena.ofShared();

    private static final NativeState NATIVE_STATE = loadNativeState();

    private static final AtomicBoolean NATIVE_CIRCUIT_OPEN =
            new AtomicBoolean(!NATIVE_STATE.available());

    private static final LongAdder NATIVE_CALLS = new LongAdder();

    private static final LongAdder FALLBACK_CALLS = new LongAdder();

    private static final ThreadLocal<NativeBuffers> NATIVE_BUFFERS =
            ThreadLocal.withInitial(NativeBuffers::new);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(MorphoProcessor::reportNativeSummary,
                                                         "morpho-native-summary"));
    }

    //~ Instance fields ----------------------------------------------------------------------------

    private final StructureElement se; //, down_se, up_se;

    private final StructureElement minus_se; //, down_se, up_se;

    private final StructureElement plus_se; //, down_se, up_se;

    private final LocalHistogram bh;

    private final LocalHistogram p_h;

    private final LocalHistogram m_h;

    private final int[][] pg;

    private final int[][] pg_plus;

    private final int[][] pg_minus;

    private final MemorySegment flattenedProbe;

    private final long flattenedProbeLength;

    int width;

    int height;

    private static NativeState loadNativeState ()
    {
        try {
            String library = System.getProperty(LIBRARY_PROPERTY);
            if (library == null || library.isBlank()) {
                throw new IOException("library_property_missing");
            }

            SymbolLookup lookup = SymbolLookup.libraryLookup(Path.of(library), NATIVE_ARENA);
            MemorySegment symbol = lookup.findOrThrow(NATIVE_SYMBOL);
            MethodHandle handle = Linker.nativeLinker().downcallHandle(
                    symbol,
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG,
                            ValueLayout.JAVA_LONG,
                            ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG));
            return new NativeState(handle, null);
        } catch (Throwable ex) {
            return new NativeState(null, ex.getClass().getSimpleName());
        }
    }

    private static MemorySegment flattenProbe (int[][] probe)
    {
        if (probe == null || probe.length == 0) {
            throw new IllegalArgumentException("probe_empty");
        }

        long bytes = Math.multiplyExact((long) probe.length, PROBE_RECORD_BYTES);
        MemorySegment flattened = NATIVE_ARENA.allocate(bytes, Integer.BYTES);
        for (int index = 0; index < probe.length; index++) {
            int[] row = probe[index];
            if (row == null || row.length < 3) {
                throw new IllegalArgumentException("probe_row_short_" + index);
            }

            long offset = (long) index * PROBE_RECORD_BYTES;
            flattened.set(ValueLayout.JAVA_INT, offset, row[0]);
            flattened.set(ValueLayout.JAVA_INT, offset + Integer.BYTES, row[1]);
            flattened.set(ValueLayout.JAVA_INT, offset + (2L * Integer.BYTES), row[2]);
        }
        return flattened.asReadOnly();
    }

    private static void reportNativeSummary ()
    {
        System.err.println("MORPHO_NATIVE_SUMMARY native_calls=" + NATIVE_CALLS.sum()
                                   + " fallback_calls=" + FALLBACK_CALLS.sum()
                                   + " circuit_open=" + NATIVE_CIRCUIT_OPEN.get());
    }

    private static final class NativeState
    {
        private final MethodHandle handle;

        private final String failure;

        private NativeState (MethodHandle handle,
                             String failure)
        {
            this.handle = handle;
            this.failure = failure;
        }

        private boolean available ()
        {
            return handle != null;
        }
    }

    private static final class NativeBuffers
    {
        private Arena arena;

        private MemorySegment input;

        private MemorySegment scratch;

        private MemorySegment output;

        private long capacity;

        private void ensure (long bytes)
                throws IOException
        {
            if (bytes <= 0 || bytes > MAX_NATIVE_BYTES) {
                throw new IOException("native_plane_too_large");
            }
            if (capacity >= bytes) {
                return;
            }

            Arena replacement = Arena.ofConfined();
            try {
                MemorySegment replacementInput = replacement.allocate(bytes, 1);
                MemorySegment replacementScratch = replacement.allocate(bytes, 1);
                MemorySegment replacementOutput = replacement.allocate(bytes, 1);
                Arena previous = arena;
                arena = replacement;
                input = replacementInput;
                scratch = replacementScratch;
                output = replacementOutput;
                capacity = bytes;
                if (previous != null) {
                    previous.close();
                }
            } catch (Throwable ex) {
                replacement.close();
                if (ex instanceof IOException io) {
                    throw io;
                }
                throw new IOException("native_buffer_allocation_failed", ex);
            }
        }
    }

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new instance of MorphoProcessor.
     *
     * @param se the structuring element for processing
     */
    public MorphoProcessor (StructureElement se)
    {
        this.se = se;
        width = se.getWidth();
        height = se.getHeight();
        minus_se = new StructureElement(se.H(se.Delta(SGRAD), HMINUS), width);
        plus_se = new StructureElement(se.H(se.Delta(NGRAD), HMINUS), width);
        bh = new LocalHistogram();
        p_h = new LocalHistogram();
        m_h = new LocalHistogram();
        pg = se.getVect();
        pg_plus = plus_se.getVect();
        pg_minus = minus_se.getVect();
        flattenedProbe = flattenProbe(pg);
        flattenedProbeLength = pg.length;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------//
    // close //
    //-------//
    /**
     * Performs graylevel dilation followed by graylevel erosion
     * with arbitrary structural element
     *
     * @param ip the ImageProcessor
     */
    public void close (ByteProcessor ip)
    {
        try {
            byte[] pixels = (byte[]) ip.getPixels();
            closeWithWorker(ip.getWidth(), ip.getHeight(), pixels);
            NATIVE_CALLS.increment();
            return;
        } catch (Exception ex) {
            FALLBACK_CALLS.increment();
        }

        closeJavaFallback(ip);
    }

    /**
     * The original Java close implementation, retained as a fail-closed fallback.
     *
     * @param ip the ImageProcessor
     */
    private void closeJavaFallback (ByteProcessor ip)
    {
        int width = ip.getWidth();
        int height = ip.getHeight();
        int w = this.width; //se.getWidth();
        int h = this.height; //se.getHeight();
        int min = 0; //,k=0,x=0,y=0;
        int max = 255; //,k=0,x=0,y=0;

        //  IJ.log("pg: "+pg.length);
        int sz = pg.length; //se.getWidth()*se.getHeight();

        byte[] pixels = (byte[]) ip.getPixels();
        byte[] newpix = new byte[pixels.length];
        byte[] newpix2 = new byte[pixels.length];
        int[] wnd = new int[sz];

        for (int row = 1; row <= height; row++) {
            for (int col = 0; col < width; col++) {
                int index = ((row - 1) * width) + col; //dilation step

                if (index < pixels.length) {
                    wnd = getMinMax(index, width, height, pixels, pg, DILATE);
                    max = wnd[1] - 255;
                    newpix[index] = (byte) (max & 0xFF);
                }

                int index2 = (((row - h - 1) * width) + col) - w; //erosion step

                if ((index2 >= 0) && (index2 < pixels.length)) {
                    wnd = getMinMax(index2, width, height, newpix, pg, ERODE);
                    min = wnd[0] + 255;
                    newpix2[index2] = (byte) (min & 0xFF);
                }
            }
        }

        for (int row = height; row <= (height + h); row++) {
            for (int col = 0; col < (width + w); col++) {
                int index2 = (((row - h - 1) * width) + col) - w; //erosion step

                if ((index2 >= 0) && (index2 < pixels.length)) {
                    wnd = getMinMax(index2, width, height, newpix, pg, ERODE);
                    min = wnd[0] + 255;
                    newpix2[index2] = (byte) (min & 0xFF);
                }
            }
        }

        System.arraycopy(newpix2, 0, pixels, 0, pixels.length);
    }

    private void closeWithWorker (int imageWidth,
                                  int imageHeight,
                                  byte[] pixels)
            throws IOException
    {
        if (imageWidth <= 0 || imageHeight <= 0) {
            throw new IOException("invalid_dimensions");
        }

        long expectedLength = (long) imageWidth * imageHeight;
        if (expectedLength != pixels.length || expectedLength > Integer.MAX_VALUE) {
            throw new IOException("pixel_length_mismatch");
        }

        if (NATIVE_CIRCUIT_OPEN.get()) {
            throw new IOException("native_circuit_open");
        }

        NativeBuffers buffers = NATIVE_BUFFERS.get();
        MemorySegment heapPixels = MemorySegment.ofArray(pixels);
        try {
            buffers.ensure(expectedLength);
            MemorySegment.copy(heapPixels, 0, buffers.input, 0, expectedLength);
            int status = (int) NATIVE_STATE.handle.invokeExact(
                    buffers.input,
                    expectedLength,
                    buffers.scratch,
                    expectedLength,
                    buffers.output,
                    expectedLength,
                    (long) imageWidth,
                    (long) imageHeight,
                    flattenedProbe,
                    flattenedProbeLength);
            if (status != 0) {
                throw new IOException("native_status_" + status);
            }

            MemorySegment.copy(buffers.output, 0, heapPixels, 0, expectedLength);
        } catch (Throwable ex) {
            NATIVE_CIRCUIT_OPEN.set(true);
            if (ex instanceof IOException io) {
                throw io;
            }
            throw new IOException("native_call_failed", ex);
        }
    }

    private static byte[] encodeProbe (int[][] probe)
    {
        if (probe == null || probe.length == 0) {
            throw new IllegalArgumentException("probe_empty");
        }

        byte[] encoded = new byte[Math.multiplyExact(probe.length, PROBE_RECORD_BYTES)];
        for (int index = 0; index < probe.length; index++) {
            int[] row = probe[index];
            if (row == null || row.length < 3) {
                throw new IllegalArgumentException("probe_row_short_" + index);
            }

            int offset = index * PROBE_RECORD_BYTES;
            writeLittleEndian(encoded, offset, row[0]);
            writeLittleEndian(encoded, offset + 4, row[1]);
            writeLittleEndian(encoded, offset + 8, row[2]);
        }
        return encoded;
    }

    private static void writeLittleEndian (byte[] target,
                                           int offset,
                                           int value)
    {
        target[offset] = (byte) value;
        target[offset + 1] = (byte) (value >>> 8);
        target[offset + 2] = (byte) (value >>> 16);
        target[offset + 3] = (byte) (value >>> 24);
    }

    private static void reportNative (String status,
                                      long started,
                                      int imageWidth,
                                      int imageHeight,
                                      int bytes,
                                      String detail)
    {
        long elapsed = System.nanoTime() - started;
        System.err.println("MORPHO_NATIVE status=" + status
                                   + " elapsed_ns=" + elapsed
                                   + " width=" + imageWidth
                                   + " height=" + imageHeight
                                   + " bytes=" + bytes
                                   + " detail=" + detail);
    }

    private static String sanitize (Exception ex)
    {
        String detail = ex.getClass().getSimpleName();
        if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            detail += "_" + ex.getMessage().replaceAll("[^A-Za-z0-9_.-]", "_");
        }
        return detail;
    }

    //--------//
    // dilate //
    //--------//
    /**
     * Performs gray level dilation
     *
     * @param ip the ImageProcessor
     */
    public void dilate (ByteProcessor ip)
    {
        int width = ip.getWidth();
        int height = ip.getHeight();
        int max = 32_768; //,k=0,x=0,y=0;

        //int[][]pg=se.getVect();
        //  IJ.log("pg: "+pg.length);
        int sz = pg.length; //se.getWidth()*se.getHeight();

        byte[] pixels = (byte[]) ip.getPixels();
        int[] wnd = new int[sz];

        byte[] newpix = new byte[pixels.length];

        //int i,j=0;
        for (int c = 0; c < pixels.length; c++) {
            //i=c/width;
            //j=c%width;
            wnd = getMinMax(c, width, height, pixels, pg, DILATE);

            max = wnd[1] - 255;
            newpix[c] = (byte) (max & 0xFF);
        }

        System.arraycopy(newpix, 0, pixels, 0, pixels.length);
    }

    //-------//
    // erode //
    //-------//
    /**
     * Performs gray level erosion
     *
     * @param ip the ImageProcessor
     */
    public void erode (ByteProcessor ip)
    {
        int width = ip.getWidth();
        int height = ip.getHeight();
        int min = -32_767; //,k=0,x=0,y=0;

        int sz = pg.length; //se.getWidth()*se.getHeight();
        // byte[] p=(byte[])ip.convertToByte(false).getValues();

        byte[] pixels = (byte[]) ip.getPixels();

        int[] wnd = new int[sz];

        byte[] newpix = new byte[pixels.length];

        //int i,j=0;
        for (int c = 0; c < pixels.length; c++) {
            // i=c/width;
            // j=c%width;
            wnd = getMinMax(c, width, height, pixels, pg, ERODE);
            min = wnd[0] + 255;
            newpix[c] = (byte) (min & 0xFF);
        }

        System.arraycopy(newpix, 0, pixels, 0, pixels.length);
    }

    /**
     * Performs fast graylevel dilation followed by fast graylevel erosion
     * with arbitrary structural element
     *
     * @param ip the ImageProcessor
     */
    public void fclose (ByteProcessor ip)
    {
        //fastDilate(ip,se);
        //fastErode(ip,se);
        int width = ip.getWidth();
        int height = ip.getHeight();
        int max = 32_767; //,k=0,x=0,y=0;

        //int pgzise=pg.length;
        byte[] pixels = (byte[]) ip.getPixels();
        byte[] newpix = new byte[pixels.length];

        //String s="", s2="";
        int row = 0;

        //String s="", s2="";
        int z = 0;
        int index = 0;

        // Dilation loop
        for (row = 1; row <= height; row++) {
            z = (row - 1) * width;
            //    IJ.log("odd index  "+ z);
            bh.init(z, width, height, pixels, pg, 0);
            //  bh.doMaximum();
            max = bh.getMaximum();
            newpix[z] = (byte) (max & 0xFF);

            for (int col = 1; col < width; col++) {
                index = z + col;

                //          s2+=" "+index+"\r\n";
                try {
                    p_h.init(index, width, height, pixels, pg_plus, 0);
                    m_h.init(index - 1, width, height, pixels, pg_minus, 0);
                    bh.sub(m_h);
                    bh.add(p_h);
                    bh.doMaximum();
                    max = bh.getMaximum();
                    newpix[index] = (byte) (max & 0xFF);
                } catch (ArrayIndexOutOfBoundsException aiob) {
                    logger.warn(" out index: " + index);
                }
            } //odd loop
        }

        int min = -32_767; //,k=0,x=0,y=0;

        byte[] newpix2 = new byte[pixels.length];

        // Erosion loop
        //boolean changed=false;
        for (row = 1; row <= height; row++) {
            z = (row - 1) * width;
            //                                //    IJ.log("odd index  "+ z);
            bh.init(z, width, height, newpix, pg, 1);

            // bh.Log();
            //bh.doMinimum();
            min = bh.getMinimum();
            newpix2[z] = (byte) (min & 0xFF);

            for (int col = 1; col < width; col++) {
                index = z + col;

                try {
                    p_h.init(index, width, height, newpix, pg_plus, 1);
                    m_h.init(index - 1, width, height, newpix, pg_minus, 1);
                    bh.sub(m_h);
                    bh.add(p_h);

                    bh.doMinimum();

                    min = bh.getMinimum();
                    newpix2[index] = (byte) (min & 0xFF);
                } catch (ArrayIndexOutOfBoundsException aiob) {
                    logger.warn(" out index: {} min {}", index, min);
                }
            } //odd loop
        }

        System.arraycopy(newpix2, 0, pixels, 0, pixels.length);
    }

    private int[] getMinMax (int index,
                             int width,
                             int height,
                             byte[] pixels,
                             int[][] pg,
                             int type)
    {
        //  int[][]pg=se.getVectTransform(mType);
        int pgzise = pg.length;

        int[] wnd = new int[2];
        int i;
        int j;
        int k = 0;
        int x;
        int y = 0;
        int min = 255;
        int max = 0;

        i = index / width;
        j = index % width;

        for (int g = 0; g < pgzise; g++) {
            y = i + pg[g][0];
            x = j + pg[g][1];

            try {
                if ((x >= width) || (y >= height) || (x < 0) || (y < 0)) {
                    if (type == DILATE) {
                        k = 0;
                    }

                    if (type == ERODE) {
                        k = 255;
                    }
                } else {
                    k = pixels[x + (width * y)] & 0xFF;
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                k = x + (width * y);
                logger.warn("AIOB x: {} y: {} index: {}", x, y, k);
            }

            if (type == DILATE) {
                k += pg[g][2];
            }

            if (type == ERODE) {
                k -= pg[g][2];
            }

            if (k < min) {
                min = k;
            }

            if (k > max) {
                max = k;
            }
        }

        wnd[0] = min & 0xFF;
        wnd[1] = max & 0xFF;

        return wnd;
    }

    //------//
    // open //
    //------//
    /**
     * Performs graylevel erosion followed by graylevel dilation
     * with arbitrary structural element se
     *
     * @param ip the ImageProcessor
     */
    public void open (ByteProcessor ip)
    {
        int width = ip.getWidth();
        int height = ip.getHeight();
        int min = -32_767; //,k=0,x=0,y=0;
        int max = 32_768;
        int w = this.width; //se.getWidth();
        int h = this.height; //se.getHeight();
        // int[][] pg=se.getVect();

        int sz = pg.length;

        byte[] pixels = (byte[]) ip.getPixels();
        byte[] newpix = new byte[pixels.length];
        byte[] newpix2 = new byte[pixels.length];
        int[] wnd = new int[sz];

        //  int i,j=0;
        for (int row = 1; row <= height; row++) {
            for (int col = 0; col < width; col++) {
                int index = ((row - 1) * width) + col; //erosion step

                if (index < pixels.length) {
                    wnd = getMinMax(index, width, height, pixels, pg, ERODE);
                    min = wnd[0] + 255;
                    newpix[index] = (byte) (min & 0xFF);
                }

                int index2 = (((row - h - 1) * width) + col) - w; //dilation step

                if ((index2 >= 0) && (index2 < pixels.length)) {
                    wnd = getMinMax(index2, width, height, newpix, pg, DILATE);
                    max = wnd[1] - 255;
                    newpix2[index2] = (byte) (max & 0xFF);
                }
            }
        }

        for (int row = height; row <= (height + h); row++) {
            for (int col = 0; col < (width + w); col++) {
                int index2 = (((row - h - 1) * width) + col) - w; //dilation step

                if ((index2 >= 0) && (index2 < pixels.length)) {
                    wnd = getMinMax(index2, width, height, newpix, pg, DILATE);
                    max = wnd[1] - 255;
                    newpix2[index2] = (byte) (max & 0xFF);
                }
            }
        }

        System.arraycopy(newpix2, 0, pixels, 0, pixels.length);
    }
}
