//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    G r a y M o r p h o l o g y                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import omr.lag.Roi;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code GrayMorphology}
 *
 * @author Hervé Bitteur
 */
public class GrayMorphology
        implements MorphoConstants
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GrayMorphology.class);

    private static final String R = "SE_r";

    private static final String SHOW = "show_SE";

    private static final String SETYPE = "SE_type";

    private static final String OPER = "MOper";

    private static final int[] offset = OFFSET0;

    private static float radius = 5.0f;

    private static int options = 0; // Circle

    private static boolean showoptions = false;

    private static int morphoptions = 3; // Close

    public static final String[] strelitems = {
        "circle", "diamond", "square", "hor line",
        "ver line", "2p h", "2p v", "free form"
    };

    public static final int[] constitems = {
        CIRCLE, DIAMOND, SQARE, HLINE, VLINE, HPOINTS,
        VPOINTS, FREE
    };

    public static final String[] morphitems = {
        "erode", "dilate", "open", "close", "fast erode",
        "fast dilate", "fast open", "fast close"
    };

    public static final int ERODE = 0;

    public static final int DILATE = 1;

    public static final int OPEN = 2;

    public static final int CLOSE = 3;

    public static final int FERODE = 4;

    public static final int FDILATE = 5;

    public static final int FOPEN = 6;

    public static final int FCLOSE = 7;

    //~ Instance fields ----------------------------------------------------------------------------
    ///ImagePlus imp;
    public String kernelText = " 0 0 0 0 0\n 0 0 255 0 0\n 0 255 255 255 0\n 0 0 255 0 0\n 0 0 0 0 0\n";

    boolean canceled = true;

    public StructureElement se;

    public StructureElement minus_se;

    public StructureElement plus_se;

    public StructureElement down_se;

    public StructureElement up_se;

    MorphoProcessor mp;

    ///ImageWindow win;
    private Roi roi;

    boolean isLineRoi;

    int slice = 0;

    private boolean seshown = false;

    //~ Methods ------------------------------------------------------------------------------------
    //    /* Extracts the ByteProcessor within a rectangular roi
    //     *
    //     * @param ip
    //     * @param r
    //     * @return ByteProcessor
    //     */
    //    public ByteProcessor getMask (ByteProcessor ip,
    //                                  Rectangle r)
    //    {
    //        //Rectangle r=ip.getRoi();
    //        int width = ip.getWidth();
    //
    //        //int height = ip.getHeight();
    //        byte[] pixels = (byte[]) ip.getPixels();
    //        int xloc = (int) r.getX();
    //        int yloc = (int) r.getY();
    //        int w = (int) r.getWidth();
    //        int h = (int) r.getHeight();
    //        byte[] mask = new byte[w * h];
    //
    //        for (int cnt = 0; cnt < mask.length; cnt++) {
    //            int index = xloc + (cnt % w) + ((cnt / w) * width)
    //                        + (yloc * width);
    //            mask[cnt] = (byte) (pixels[index] & 0xFF);
    //        }
    //
    //        return new ByteProcessor(w, h, mask, ip.getColorModel());
    //    }
    /** principal method of the plugin
     * does the actual job
     *
     * @param ip the ImageProcessor
     */
    public void run (ByteProcessor ip)
    {
        //IJ.log( "options SE "+strelitems[options]+ " "+ constitems[options]);
        int eltype = constitems[options];

        se = new StructureElement(eltype, 1, radius, offset);
        mp = new MorphoProcessor(se);

        //            if ((showoptions) && (!seshown)) {
        //                minus_se = mp.getSE(-1);
        //                plus_se = mp.getSE(1);
        //                showStrEl(se, "SE r=" + radius);
        //                showStrEl(minus_se, "minus SE r=" + radius);
        //                showStrEl(plus_se, "plus SE r=" + radius);
        //            }
        slice++;

        //IJ.showStatus("Doing slice " + slice);
        //            if (slice > 1) {
        //                logger.info(
        //                        imp.getTitle() + " : " + slice + "/" + imp.getStackSize());
        //            }
        // ip.snapshot();
        //            Rectangle r = ip.getRoi();
        //
        //            if (r == null) {
        doOptions(ip, mp, morphoptions);

        //            } // end if
        //            else if (!isLineRoi) {
        //                ImageProcessor ipmask = getMask((ByteProcessor) ip, r);
        //                doOptions(ipmask, mp, morphoptions);
        //                ip.insert(ipmask, r.x, r.y);
        //            } // end if
        //            if (slice == imp.getImageStackSize()) {
        //                imp.updateAndDraw();
        //            }
    }

    //    public int setup (String arg,
    //                      ImagePlus imp)
    //    {
    //        this.imp = imp;
    //
    //        IJ.register(GrayMorphology_.class);
    //
    //        if (arg.equals("about")) {
    //            showAbout();
    //
    //            return DONE;
    //        } else {
    //            if (imp != null) {
    //                win = imp.getWindow();
    //                win.running = true;
    //
    //                roi = imp.getRoi();
    //                isLineRoi = ((roi != null) && (roi.getType() == Roi.LINE));
    //            }
    //
    //            if (IJ.versionLessThan("1.35") || !showDialog(imp)) {
    //                return DONE;
    //            } else {
    //                return DOES_8G + DOES_STACKS;
    //            }
    //        }
    //    }
    //
    //    /** displays the StructureElement */
    //    /**
    //     * @param strel
    //     * @param Title
    //     */
    //    public void showStrEl (StructureElement strel,
    //                           String Title)
    //    {
    //        int wh = strel.getWidth();
    //        int hh = strel.getHeight();
    //
    //        //IJ.log("width: "+wh+" height: "+hh);
    //        //Log(strel.getMask());
    //        //Log(se.getVect());
    //        ImageProcessor fp = new FloatProcessor(wh, hh, strel.getMask()).convertToByte(
    //                false);
    //        new ImagePlus(Title, fp).show();
    //        seshown = true;
    //    }

    /* ------------------------------------------------------------------ */
    //    void showAbout ()
    //    {
    //        IJ.showMessage(
    //                "Gray Morphology version  2.3",
    //                "This plugin performs the basic morphologic operations on grayscale images \n  "
    //                + "erosion, dilation, opening and closing with several types of structuring elements.\n"
    //                + "It is build upon the StructureElement class. \n"
    //                + "The develpoment of this alogorithm was inspired by the book of Jean Serra \n"
    //                + "\"Image Analysis and Mathematical Morphology\"");
    //    } /* showAbout */
    //    boolean showDialog (ImagePlus imp)
    //    {
    //        if (imp == null) {
    //            return true;
    //        }
    //
    //        GenericDialog gd = new GenericDialog("Parameters");
    //
    //        // Dialog box for user input
    //        gd.addMessage(
    //                "This plugin performs morphology operators on graylevel images\n");
    //
    //        gd.addNumericField(
    //                "Radius of the structure element (pixels):",
    //                radius,
    //                1);
    //
    //        gd.addChoice(
    //                "Type of structure element",
    //                strelitems,
    //                strelitems[options]);
    //        gd.addCheckbox("Show mask", showoptions);
    //        gd.addChoice("Operator", morphitems, morphitems[morphoptions]);
    //
    //        gd.showDialog();
    //        radius = (float) gd.getNextNumber();
    //        options = gd.getNextChoiceIndex();
    //
    //        showoptions = gd.getNextBoolean();
    //        morphoptions = gd.getNextChoiceIndex();
    //
    //        if (gd.wasCanceled()) {
    //            return false;
    //        }
    //
    //        if (!validate(radius, 2)) {
    //            logger.warn("Invalid Numbers!\n" + "Enter floats 0.5 or 1");
    //
    //            return false;
    //        }
    //
    //        return true;
    //    }
    private void doOptions (ByteProcessor ip,
                            MorphoProcessor mp,
                            int morphoptions)
    {
        switch (morphoptions) {
        case ERODE: {
            mp.erode(ip);

            break;
        }

        case DILATE: {
            mp.dilate(ip);

            break;
        }

        case OPEN: {
            mp.open(ip);

            break;
        }

        case CLOSE: {
            mp.close(ip);

            break;
        }

        //        case FERODE: {
        //            if ((se.getType() == HLINE) || (se.getType() == VLINE)) {
        //                mp.LineErode(ip);
        //            } else {
        //                mp.fastErode(ip);
        //            }
        //
        //            break;
        //        }
        //        case FDILATE: {
        //            if ((se.getType() == HLINE) || (se.getType() == VLINE)) {
        //                mp.LineDilate(ip);
        //            } else {
        //                mp.fastDilate(ip);
        //            }
        //
        //            break;
        //        }
        //        case FOPEN: {
        //            if ((se.getType() == HLINE) || (se.getType() == VLINE)) {
        //                mp.LineErode(ip);
        //                mp.LineDilate(ip);
        //            } else {
        //                mp.fopen(ip);
        //            }
        //
        //            break;
        //        }
        //        case FCLOSE: {
        //            if ((se.getType() == HLINE) || (se.getType() == VLINE)) {
        //                mp.LineDilate(ip);
        //                mp.LineErode(ip);
        //            } else {
        //                mp.fclose(ip);
        //            }
        //
        //            break;
        //        }
        default:
        } // switch
    }

    //    /* Creates a StructureElement
    //     * from text input; must be delimited
    //     *
    //     * @return StructureElement
    //     */
    //    private StructureElement inputSE ()
    //    {
    //        GenericDialog gd = new GenericDialog("Input Mask", IJ.getInstance());
    //        gd.addTextAreas(kernelText, null, 10, 30);
    //        gd.showDialog();
    //
    //        if (gd.wasCanceled()) {
    //            canceled = true;
    //
    //            return null;
    //        }
    //
    //        kernelText = gd.getNextText();
    //
    //        return new StructureElement(kernelText);
    //    }
//
//    /* validates the input value
//     * only n/2 floats are accepted
//     */
//    private boolean validate (float var,
//                              int k)
//    {
//        float a = k * var;
//        int b = (int) (k * var);
//
//        // IJ.log(IJ.d2s(a-b));
//        if (((a - b) == 0) || (var < 0)) {
//            return true;
//        } else {
//            return false;
//        }
//    }
}
