//-----------------------------------------------------------------------//
//                                                                       //
//                        R G B T o B i l e v e l                        //
//                                                                       //
//-----------------------------------------------------------------------//
/*
 * Copyright (c) 2002 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * -Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that Software is not designed,licensed or intended for use in
 * the design, construction, operation or maintenance of any nuclear facility.
 */

package omr.jai;

import java.awt.*;
import java.awt.color.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import java.util.*;
import javax.media.jai.*;
import javax.media.jai.operator.*;
import javax.media.jai.widget.*;

/**
 * Demo code for dithering a 24-bit RGB image to a monochrome (1-bit
 * or bilevel) image. The source image must be an 24-bit RGB image.
 * The result is displayed.
 *
 * Usage: java RGBToBilevel filename [true]
 *
 * If the second argument is present and equal to "true" then error diffusion
 * will be used; otherwise ordered dithering will be used.
 */
public class RGBToBilevel extends Frame {
    public static void main(String[] args) {
        new RGBToBilevel(args[0],
                         args.length > 1 ? args[1].equals("true") : false);
    }

    RGBToBilevel(final String fileName,
                 boolean isErrorDiffusion)
    {

        // Load the file.
        PlanarImage src = JAI.create("fileload", fileName);

        // Load the ParameterBlock for the dithering operation
        // and set the operation name.
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(src);
        String opName = null;
        if(isErrorDiffusion) {
            opName = "errordiffusion";
            LookupTableJAI lut =
                new LookupTableJAI(new byte[][] {{(byte)0x00, (byte)0xff},
                                                 {(byte)0x00, (byte)0xff},
                                                 {(byte)0x00, (byte)0xff}});
            pb.add(lut);
            pb.add(KernelJAI.ERROR_FILTER_FLOYD_STEINBERG);
        } else {
            opName = "ordereddither";
            ColorCube cube = ColorCube.createColorCube(DataBuffer.TYPE_BYTE,
                                                       0, new int[] {2, 2, 2});
            pb.add(cube);
            pb.add(KernelJAI.DITHER_MASK_443);
        }

        // Create a layout containing an IndexColorModel which maps
        // zero to zero and unity to 255; force SampleModel to be bilevel.
        ImageLayout layout = new ImageLayout();
        byte[] map = new byte[] {(byte)0x00, (byte)0xff};
        ColorModel cm = new IndexColorModel(1, 2, map, map, map);
        layout.setColorModel(cm);
        SampleModel sm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE,
                                                         src.getWidth(),
                                                         src.getHeight(),
                                                         1);
        layout.setSampleModel(sm);

        // Create a hint containing the layout.
        RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT,
                                                  layout);

        // Dither the image.
        final PlanarImage dst = JAI.create(opName, pb, hints);

        // Exit on window closing.
        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    JAI.create("filestore", dst, fileName + ".out", "PNG", null);
                    System.exit(0);
                }
            });


        // Display the result.
        //// ATTENTION A REMPLACER : add(new ScrollingImagePanel(dst, dst.getWidth(), dst.getHeight()));
        pack();
        setVisible(true);
    }
}
