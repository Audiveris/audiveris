//----------------------------------------------------------------------------//
//                                                                            //
//                             I m a g e I n f o                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.jai;

import java.awt.Transparency;
import java.awt.image.*;
import java.io.File;
import javax.media.jai.*;

public class ImageInfo
{
    public static void main(String[] args)
    {
        // Open the image (using the name passed as a command line parameter)
        PlanarImage pi = JAI.create("fileload", args[0]);

        // Get the image file size (non-JAI related).
        File image = new File(args[0]);
        System.out.println("Image file size: "+image.length()+" bytes.");

        print(pi);
    }

    public static void print (PlanarImage pi)
    {
        // Show the image dimensions and coordinates.
        System.out.print("Image Dimensions: ");
        System.out.print(pi.getWidth()+"x"+pi.getHeight()+" pixels");

        // Remember getMaxX and getMaxY return the coordinate of the next point!
        System.out.println(" (from "+pi.getMinX()+","+pi.getMinY()+" to " +
                           (pi.getMaxX()-1)+","+(pi.getMaxY()-1)+")");
        if ((pi.getNumXTiles() != 1)||(pi.getNumYTiles() != 1)) { // Is it tiled?
            // Tiles number, dimensions and coordinates.
            System.out.print("Tiles: ");
            System.out.print(pi.getTileWidth()+"x"+pi.getTileHeight()+" pixels"+
                             " ("+pi.getNumXTiles()+"x"+pi.getNumYTiles()+" tiles)");
            System.out.print(" (from "+pi.getMinTileX()+","+pi.getMinTileY()+
                             " to "+pi.getMaxTileX()+","+pi.getMaxTileY()+")");
            System.out.println(" offset: "+pi.getTileGridXOffset()+","+
                               pi.getTileGridXOffset());
        }

        // Display info about the SampleModel of the image.
        SampleModel sm = pi.getSampleModel();
        System.out.println("Number of bands: "+sm.getNumBands());
        System.out.print("Data type: ");
        switch(sm.getDataType()) {
        case DataBuffer.TYPE_BYTE: System.out.println("byte"); break;
        case DataBuffer.TYPE_SHORT: System.out.println("short"); break;
        case DataBuffer.TYPE_USHORT: System.out.println("ushort"); break;
        case DataBuffer.TYPE_INT: System.out.println("int"); break;
        case DataBuffer.TYPE_FLOAT: System.out.println("float"); break;
        case DataBuffer.TYPE_DOUBLE: System.out.println("double"); break;
        case DataBuffer.TYPE_UNDEFINED:System.out.println("undefined"); break;
        }

        // Display info about the ColorModel of the image.
        ColorModel cm = pi.getColorModel();
        if (cm != null)
            {
                System.out.println("Number of color components: "+
                                   cm.getNumComponents());
                System.out.println("Bits per pixel: "+cm.getPixelSize());
                System.out.print("Image Transparency: ");
                switch(cm.getTransparency()) {
                case Transparency.OPAQUE: System.out.println("opaque"); break;
                case Transparency.BITMASK: System.out.println("bitmask"); break;
                case Transparency.TRANSLUCENT:
                    System.out.println("translucent"); break;
                }
            }
        else System.out.println("No color model.");
    }
}
