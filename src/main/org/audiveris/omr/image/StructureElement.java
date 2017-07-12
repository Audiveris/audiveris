//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S t r u c t u r e E l e m e n t                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import java.util.StringTokenizer;

/**
 * Class {@code StructureElement}
 *
 * @author Hervé Bitteur
 */
public class StructureElement
        implements MorphoConstants
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(StructureElement.class);

    static final String EOL = System.getProperty("line.separator");

    //~ Instance fields ----------------------------------------------------------------------------
    private int[] mask;

    private int width = 1;

    private int height = 1;

    private double radius = 0;

    private int[][] vect;

    private int[] offset = OFFSET0;

    private int shift = 1;

    public int type = FREE;

    public boolean offsetmodified = false;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new StructureElement object.
     *
     * @param amask DOCUMENT ME!
     * @param width DOCUMENT ME!
     */
    public StructureElement (int[] amask,
                             int width)
    {
        // this.width=(int) Math.sqrt(amask.length);
        this.width = width;
        this.height = amask.length / width;
        this.shift = 0;
        setMask(amask);
        vect = calcVect(mask, width);
    }

    /**
     * Creates a new StructureElement object.
     *
     * @param tokenString DOCUMENT ME!
     */
    public StructureElement (String tokenString)
    {
        this.shift = 0;
        this.type = FREE;
        mask = inputMask(tokenString);
        vect = calcVect(mask, width);
    }

    /** Creates a new instance of a StructureElement */
    public StructureElement (int type,
                             int shift,
                             float radius,
                             int[] offset)
    {
        // this.width=(int)(2*radius+2*shift);
        int r = 0;
        //   this.width= ((int)(radius+shift+0.5))*2 + 1;
        //  this.height=width;
        this.shift = shift;
        this.radius = radius;

        if (((offset[0] * offset[0]) + (offset[1] * offset[1])) >= (radius / 2)) {
            offset = OFFSET0;
        }

        this.offset = offset;
        this.type = type;

        switch (type) {
        case CIRCLE: {
            mask = createCircularMask(shift, radius, offset);

            break;
        }

        case DIAMOND: {
            mask = createDiamondMask(shift, radius, offset);

            break;
        }

        case SQARE: {
            mask = createSquareMask(shift, 2 * radius, offset);

            break;
        }

        case HLINE: {
            //this.height=1;
            this.shift = 1;
            mask = createLineMask(shift, 2 * radius, 0, offset);

            break;
        }

        case VLINE: {
            //this.height=width;
            // this.width=1;
            this.shift = 1;
            mask = createLineMask(shift, 2 * radius, Math.PI / 2, offset);

            break;
        }

        case HPOINTS:
            r = (int) radius;
            this.width = r + 2;
            this.height = 1;
            mask = new int[width];
            mask[1] = 255;
            mask[width - 1] = 255;

            break;

        case VPOINTS:
            r = (int) radius;
            this.height = r + 2;
            this.width = 1;
            mask = new int[height];
            mask[1] = 255;
            mask[height - 1] = 255;

            break;

        case RING:
        default:
            ; //mask=inputStrEl();
        }

        vect = calcVect(mask, width);
    }

    //~ Methods ------------------------------------------------------------------------------------
    public int[] Delta (int[] offset)
    {
        int[] astrel = this.T(offset);

        //int index=0;
        // for (int i=0; i<this.width-1;i++){
        // for (int j=0;j<this.width;j++){
        for (int index = 0; index < mask.length; index++) {
            //        for (int i=0;i<height-1;i++){
            //            for  (int j=0; j<width;j++){
            // int i=c/width;
            //int j=c%width;
            //  index=j+this.width*i;
            astrel[index] = mask[index] - astrel[index];

            //}
        }

        return astrel;
    }

    public int[] H (int[] strel,
                    int sign)
    {
        // int[] strel=new int[width*width];
        for (int i = 0; i < strel.length; i++) {
            if ((strel[i] * sign) >= 0) {
                strel[i] = 0;
            } else {
                strel[i] = 255;
            }
        }

        return strel;
    }

    public int[] T (int[] h)
    {
        int m;
        int n;
        int index;
        int ind;

        // int[] strel=new int[width*width];
        int[] strel = new int[mask.length];

        for (int i = 0; i < (height - 1); i++) {
            for (int j = 0; j < width; j++) {
                m = i + h[0]; // y - direction
                n = j + h[1]; // x - direction

                if (m < 0) {
                    m = 0;
                }

                if (n < 0) {
                    n = 0;
                }

                if (n > width) {
                    n = width;
                }

                if (m > (width - 1)) {
                    m = width - 1;
                }

                index = n + (width * m);
                ind = j + (width * i);

                try {
                    // if (index>width*width) index=width*width;
                    strel[ind] = mask[index];
                } catch (ArrayIndexOutOfBoundsException ex) {
                    logger.warn("mask: " + mask.length);
                    logger.warn("index2: " + index);
                    logger.warn("index1: " + ind);
                }
            }
        }

        return strel;
    }

    public StructureElement Tr (int[] h)
    {
        return new StructureElement(T(h), this.width);
    }

    public int[][] VectTransform (int opt)
    {
        if (opt == PERIM) {
            return calcVect(getBorder(), this.width);
        } else {
            return calcVect(this.mask, this.width);
        }
    }

    public int getArea ()
    {
        int maskSize = 0;

        for (int i = 0; i < mask.length; i++) {
            if (mask[i] != 0) {
                maskSize++;
            }
        }

        return maskSize;
    }

    public int[] getBorder ()
    {
        int sz = this.mask.length;
        int[] perim = new int[sz];
        int k;
        int l;
        int m;
        int n = -1;
        int i;
        int j = 0;

        for (int c = 0; c < sz; c++) {
            //        for (int i=0;i<height-1;i++){
            //            for  (int j=0; j<width;j++){
            i = c / width;
            j = c % width;

            //System.out.println("i: "+i+ " j: "+j+"index: "+c);
            if ((j + 1) > (width - 1)) {
                k = 0;
            } else {
                k = mask[(i * width) + j + 1];
            }

            if ((i + 1) > (height - 1)) {
                l = 0;
            } else {
                l = mask[((i + 1) * width) + j];
            }

            if (j == 0) {
                m = 0;
            } else {
                m = mask[((i * width) + j) - 1];
            }

            if (i == 0) {
                n = 0;
            } else {
                n = mask[((i - 1) * width) + j];
            }

            //  if (i*width+j<=mask.length)
            //  if (mask[i*width+j]>k || mask[i*width+j]>m || mask[i*width+j]>l || mask[i*width+j]>n)
            if ((mask[c] > k) || (mask[c] > m) || (mask[c] > l) || (mask[c] > n)) {
                // perim[i*width+j]= mask[i*width+j];
                perim[c] = mask[c];

                //c++;
            }

            //System.out.println(perim[c]);
        }

        return perim;
    }

    public double getDistance (int[] X,
                               int metrics)
    {
        double d = 0d;
        double g = 0;

        //System.out.println("type: " +metrics);
        switch (metrics) {
        case CIRCLE: {
            g = (X[0] * X[0]) + (X[1] * X[1]);
            d = Math.sqrt(g + 1) + cor3;

            // if (g==2) d=2.0d;
            break;
        }

        case DIAMOND: {
            d = (double) (Math.abs(X[0]) + Math.abs(X[1]));

            break;
        }

        case SQARE: {
            d = Math.max(Math.abs(X[0]), Math.abs(X[1]));

            break;
        }

        default:
            d = Math.sqrt((X[0] * X[0]) + (X[1] * X[1]));
        }

        return d;
    }

    public int getHeight ()
    {
        return height;
    }

    public int[] getMask ()
    {
        return mask;
    }

    public int getMaskAt (int index)
    {
        if (index <= mask.length) {
            return mask[index];
        } else {
            return -1;
        }
    }

    public int getMaskAt (int x,
                          int y)
    {
        if (x <= 0) {
            x = 0;
        }

        if (x > height) {
            x = height;
        }

        if (y <= 0) {
            y = 0;
        }

        if (y >= width) {
            y = width - 1;
        }

        int index = x + (this.width * y);

        //      if (index<=this.width*this.width) {
        return mask[index];

        //     }
        //  else return -1;
    }

    public int[] getOffset ()
    {
        return this.offset;
    }

    public double getR ()
    {
        return radius;
    }

    public int getShift ()
    {
        return shift;
    }

    public int getType ()
    {
        return type;
    }

    public int[][] getVect ()
    {
        return vect; //calcVect(this.mask , this.width);
    }

    public int getWidth ()
    {
        return width;
    }

    public void setMask (int[] amask)
    {
        this.mask = amask;
    }

    public void setOffset (int[] offset)
    {
        this.offset = offset;
        offsetmodified = true;
    }

    public void setType (int type)
    {
        this.type = type;
    }

    double getNum (StringTokenizer st)
    {
        Double d;
        String token = st.nextToken();

        try {
            d = new Double(token);
        } catch (NumberFormatException e) {
            d = null;
        }

        if (d != null) {
            return (d.doubleValue());
        } else {
            return 0.0;
        }
    }

    private int[] createDiamondMask (int shift,
                                     double radius,
                                     int[] offset)
    {
        this.width = (((int) (radius + shift + 0.5)) * 2) + 1;
        this.height = width;

        int[] mask = new int[this.width * this.width];

        // int[] mask = new int[width*width];
        int r = width / 2;

        //  double r = width/2.0-0.5;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                if ((Math.abs(x - offset[0]) + Math.abs(y - offset[1])) <= radius) {
                    mask[r + x + ((r + y) * width)] = 255;
                }
            }
        }

        return mask;
    }

    private int[] createLineMask (int shift,
                                  double l,
                                  double alpha,
                                  int[] offset)
    {
        //int width=(int)(2*radius+2*shift);
        int r = (int) ((l / 2.0f) - 1);

        if (validate((float) (alpha / Math.PI), 1)) {
            alpha = 0;
        }

        //this.width=(int)(2*radius*Math.cos(alpha)+2*shift);
        //this.height=(int)(2*radius*Math.sin(alpha)+2*shift);
        this.width = (int) ((l + (2 * shift)) * Math.cos(alpha));
        this.height = (int) ((l + (2 * shift)) * Math.sin(alpha));

        if (width == 0) {
            width++;
        }

        if (height == 0) {
            height++;
        }

        int sz = this.width * this.height;
        int[] mask = new int[sz];

        if ((alpha == 0) || (alpha == Math.PI)) {
            //for (int i=0;i<height-1;i++)
            // IJ.log("shift "+shift);
            for (int j = shift; j < (width - shift); j++) {
                mask[j] = 255;
            }
        } else if (alpha == (Math.PI / 2)) {
            for (int i = shift; i < (height - shift); i++) {
                //for (int j=0;j<width;j++)
                mask[i] = 255;
            }
        } else {
            for (int x = r - shift; x <= (r - shift); x++) {
                for (int y = r - shift; y <= (r - shift); y++) {
                    if (Math.abs(alpha) < (Math.PI / 2)) {
                        if ((y - (Math.tan(alpha) * x)) == 0) {
                            mask[(r + x) - offset[0] + (((r + y) - offset[1]) * width)] = 255;
                        }
                    } else if (((Math.tan((Math.PI / 2) - alpha) * y) - x) == 0) {
                        mask[(r + x) - offset[0] + (((r + y) - offset[1]) * width)] = 255;
                    }
                }
            }
        }

        return mask;
    }

    private int[] createSquareMask (int shift,
                                    double radius,
                                    int[] offset)
    {
        if (radius == 0) {
            radius = 0.5;
        }

        int r;
        int c = 0;
        this.width = (int) ((2 * radius) + (2 * shift));
        this.height = width;

        int sz = this.width * this.height;
        int[] mask = new int[sz];

        // for (int r=shift;r<height-shift-1;r++)
        //    for (int c=shift;c<width-shift;c++) {
        for (int counter = 0; counter < sz; counter++) {
            r = counter / width;
            c = counter % width;

            if ((r > shift) || (r < (height - shift)) || (c > shift) || (c < (width - shift))) {
                //  try {
                mask[counter] = 255;
            }

            //       }
            //       catch (Exception ex) {
            //           System.out.println("mask: "+mask.length);
            //           System.out.println("r: "+r);
            //           System.out.println("c: "+ c);
            //      }
        }

        return mask;
    }

    private int[] inputMask (String tokenString)
    {
        StringTokenizer st = new StringTokenizer(tokenString);
        int n = st.countTokens();

        // System.out.println("tokens: "+n);
        int kw = (int) Math.sqrt(n);
        int kh = kw;
        int sz = kw * kh;

        //System.out.println("size: "+sz);
        int[] k = new int[sz];

        for (int i = 0; i < sz; i++) {
            k[i] = (int) getNum(st);
        }

        //this.mask=k;
        this.width = kw;
        this.height = width;

        return k;
    }

    private static boolean validate (float var,
                                     int k)
    {
        float a = k * var;
        int b = (int) (k * var);

        if (((a - b) == 0) || (var < 0)) {
            return true;
        } else {
            return false;
        }
    }

    private int[][] calcVect (int[] perim,
                              int w)
    {
        int N = 0;
        int sz = perim.length;

        for (int i = 0; i < perim.length; i++) {
            if (perim[i] > 0) {
                N++;
            }
        }

        //System.out.println("nnz: "+N);
        int h = sz / w;
        int p = (int) Math.floor(h / 2);
        int q = (int) Math.floor(w / 2);

        // System.out.println("p: "+p);
        //  System.out.println("q: "+q);
        int[][] pg = new int[N][4];
        int i;
        int j;
        int counter = 0;

        // System.out.println("size:"+sz);
        for (int c = 0; c < sz; c++) {
            //        for (int  i=0;i<h-1;i++) {
            //            for (int j=0;j<w;j++){
            i = c / w;
            j = c % w;

            if (perim[c] > 0) {
                pg[counter][0] = i - p;
                pg[counter][1] = j - q;
                pg[counter][2] = perim[c];

                int[] a = {pg[counter][1], pg[counter][0]};
                double d = getDistance(a, this.type);
                //System.out.println("i: "+pg[counter][0]+ " j: "+pg[counter][1] +"d: " +d);
                pg[counter][3] = (int) Math.round(d);
                // System.out.println("i: "+pg[counter][0]+ " j: "+pg[counter][1]+"index: "+counter);
                counter++;

                //int pp=i-p;
                // int qq=j-q ;
            }

            //System.out.println("i: "+i+ " j: "+j+"index: "+c);
            // }
        }

        return pg;
    }

    private int[] createCircularMask (int shift,
                                      double radius,
                                      int[] offset)
    {
        //  if (radius<=2.0)  {
        // this.width= ((int)(radius+shift+0.5))*2 ;
        //offset= SWGRAD;
        //}
        //else
        this.width = (((int) (radius + shift + 0.5)) * 2) + 1;
        // this.width= ((int)(radius+shift))*2 + 1;
        this.height = width;

        // IJ.log("w="+width);
        int[] mask = new int[this.width * this.width];
        double r = (width / 2.0) - 0.5;

        //if ((radius==1.5) && (r==3)) r=3.5;
        double r2 = (radius * radius) + 1;

        // IJ.log("radius "+radius+" r "+r +" r2 "+r2);
        int index = 0;

        for (double x = -r; x <= r; x++) {
            for (double y = -r; y <= r; y++) {
                //int index= (int)(r+x+width*(r+y));
                //   if (x*x+y*y<r2){
                if ((((x - offset[0]) * (x - offset[0])) + ((y - offset[1]) * (y - offset[1]))) < r2) {
                    mask[index] = 255;
                }

                index++;
            }
        }

        // return mask;
        return mask;
    }
}
