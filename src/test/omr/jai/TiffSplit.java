/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package omr.jai;

import omr.util.StopWatch;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

/**
 * Class {@code TiffSplit}
 *
 * @author Hervé Bitteur
 */
public class TiffSplit
{
    //~ Methods ----------------------------------------------------------------

    public static void main (String... args)
        throws Exception
    {
        String                ext = "tif";
        StopWatch             watch = new StopWatch(
            "Reading tif / Writing " + ext);

        // Input file
        String                fileName = "D0394228.tif";
        File                  inputFile = new File(fileName);
        ImageInputStream      iis = ImageIO.createImageInputStream(inputFile);

        // Reader
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
        ImageReader           reader = readers.next();
        reader.setInput(iis);

        // Number of images
        int                   number = reader.getNumImages(true);

        // Writer
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(
            ext);
        ImageWriter           writer = writers.next();

        ImageOutputStream     ios = null;
        BufferedImage         img = null;
        File                  outputFile = null;

        for (int i = 0; i < number; i++) {
            watch.start("Reading " + i);
            img = reader.read(i);
            watch.stop();
            outputFile = new File("myimage" + i + "." + ext);
            ios = ImageIO.createImageOutputStream(outputFile);
            writer.setOutput(ios);
            watch.start("Writing " + i);
            writer.write(img);
            watch.stop();
            ios.flush();
            img.flush();
        }

        watch.print();
    }

    private TiffSplit ()
    {
    }
}
