//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         LocalHistogram                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code LocalHistogram}
 *
 * @author Hervé Bitteur
 */
public class LocalHistogram
        implements MorphoConstants
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(LocalHistogram.class);

    private static final int MAX = 255;

    private static final int MIN = 0;

    //~ Instance fields ----------------------------------------------------------------------------
    //  private int[] hist=new int[256];
    private int[] counts = new int[256];

    private int min = MIN;

    private int max = MAX;

    private int binCount = 0;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new LocalHistogram object.
     *
     * @param cnt DOCUMENT ME!
     */
    public LocalHistogram (int[] cnt)
    {
        // this.hist=hist;
        this.counts = cnt;

        // this.hist=h;
        //count();
    }

    /**
     * Creates a new LocalHistogram object.
     */
    public LocalHistogram ()
    {
        //binCount=Math.min(size,256);
        //hist=new int[256];
        //counts=new int[256];
    }

    /** Creates a new instance of LocalHistogram */
    public LocalHistogram (int index,
                           int width,
                           int height,
                           byte[] pixels,
                           int[][] pg,
                           int type)
    {
        int binCount = Math.min(pg.length, 256);
        //hist=new int[256];
        counts = new int[256];
        init(index, width, height, pixels, pg, type);
    }

    //~ Methods ------------------------------------------------------------------------------------
    public void Log ()
    {
        //    IJ.log("min:  "+min + "  max: "+max);
        StringBuffer sb = new StringBuffer(200);

        for (int h = 0; h <= 255; h++) {
            if (counts[h] != 0) {
                sb.append(h + " " + counts[h] + " \n");
            }

            // sb.append(counts[h]+" ");
        }

        logger.info("histogram +\n" + sb.toString() + "\n");
    }

    public void add (LocalHistogram bh)
    {
        int u = Math.min(min, bh.min);
        int v = Math.max(max, bh.max);
        // int s=Math.min(max,bh.max);
        //int t=Math.max(min,bh.min);
        binCount = Math.max(this.binCount, bh.binCount);

        //   IJ.log("u "+u+" v "+v);
        //int tmin=u; int tmax=v;
        for (int i = u; i <= v; i++) {
            counts[i] = counts[i] + bh.counts[i];

            // if (counts[i]<0) IJ.log("less than zero: "+i);
        }

        /*
         * int tmin=u; int tmax=v;
         * while (counts[tmin]==0 && (tmin < u))
         * tmin++;
         *
         * while (counts[tmax]==0 && (tmax > tmin))
         * tmax--;
         * min=tmin;
         * max=tmax;
         */
        min = u;
        max = v;

        // setCounts(c);
    } // end add

    /*
     * public void setCounts(int[] c){
     * this.counts=c;
     * }
     */
    public void count ()
    {
        int counter = 0;

        for (int i = 0; i < 256; i++) {
            if (counts[i] >= 0) {
                counter++;
            }
        }

        this.binCount = counter;
    }

    public void doMaximum ()
    {
        int smax = this.max;

        while ((counts[smax] == 0) && (smax >= MIN)) {
            smax--;
        }

        this.max = smax;
    }

    public void doMinimum ()
    {
        int smin = this.min;

        //  try {
        while ((counts[smin] == 0) && (smin <= MAX)) {
            smin++;
        }

        // }
        //  catch  ( ArrayIndexOutOfBoundsException aiob) {
        //       IJ.log(" out min: "+smin);
        //  }
        this.min = smin;
    }

    public int getBinCount ()
    {
        return this.binCount;
    }

    public int[] getCounts ()
    {
        return counts;
    }

    public int getMaximum ()
    {
        return this.max;
    }

    public int getMinimum ()
    {
        return this.min;
    }

    public void init (int index,
                      int width,
                      int height,
                      byte[] pixels,
                      int[][] pg,
                      int type)
    {
        int[] histogram = new int[256];

        //boolean[] h=new boolean[256];
        //int pgsize=pg.length;
        //  int[] wnd=new int[pgsize];
        int k = 0;
        int x;
        int y = 0;
        int bin = 0;
        int i = index / width;
        int j = index % width;
        int tmin = 255;
        int tmax = 0;

        for (int g = 0; g < pg.length; g++) {
            y = i + pg[g][0];
            x = j + pg[g][1];

            try {
                if ((x >= width) || (y >= height) || (x < 0) || (y < 0)) {
                    if (type == 0) {
                        k = 0;
                    } else {
                        k = 255;
                    }
                } else {
                    k = pixels[x + (width * y)] & 0xFF;
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                k = x + (width * y);
                logger.warn("AIOB x: {} y: {} index: {}", x, y, k);
            }

            if (type == 0) {
                bin = (k + pg[g][2]) - 255;
            } else {
                bin = k - pg[g][2] + 255;
            }

            if (tmin > bin) {
                tmin = bin;
            }

            if (tmax < bin) {
                tmax = bin;
            }

            histogram[bin]++;
        }

        //   int count=0;
        this.min = tmin;
        this.max = tmax;

        counts = histogram;

        /*
         * int cnt=0;
         * for (int z=min; z<=max; z++) {
         * if (histogram[z]>0) {
         * hist[cnt]=z;
         * counts[cnt]=histogram[z];
         * cnt++;
         * }
         * }
         */
        //Log();
    }

    public void sub (LocalHistogram bh)
    {
        int u = Math.min(min, bh.min);
        int v = Math.max(max, bh.max);

        //int s=Math.min(max,bh.max);
        //int t=Math.max(min,bh.min);
        // IJ.log("u "+u+" v "+v);
        int cnt = 0;

        //int z=0;
        int tmin = u;
        int tmax = v;

        for (int i = u; i <= v; i++) {
            counts[i] = counts[i] - bh.counts[i];

            if (counts[i] < 0) {
                counts[i] = 0;

                // cnt++;
            }

            //  if (counts[i]==0) tmin++;
        }

        while ((counts[tmin] == 0) && (tmin < u)) {
            tmin++;
        }

        while ((counts[tmax] == 0) && (tmax > tmin)) {
            tmax--;
        }

        // for ( int j=t;j<=u; j++) {
        //     if(counts[j]==0) tmin++;
        //}
        //for (int j=v; j>=tmin; j--) {
        // if(counts[j]==0) tmax--;
        //}
        min = tmin;
        max = tmax;
    } // end sub
} // end LocalHistogram
