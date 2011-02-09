//----------------------------------------------------------------------------//
//                                                                            //
//                       O c r T e x t V e r i f i e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import omr.util.WrappedBoolean;
import omr.util.XmlUtilities;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code OcrTextVerifier} gathers a set of tests to check whether a text
 * content as provided by the Ocr engine looks consistent with the original
 * glyph.
 *
 * @author Hervé Bitteur
 */
public class OcrTextVerifier
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        OcrTextVerifier.class);

    /** Needed for font size computation */
    private static final FontRenderContext frc = new FontRenderContext(
        null,
        true,
        true);

    //~ Methods ----------------------------------------------------------------

    //---------//
    // isValid //
    //---------//
    /**
     * Check the ocr line with respect to the input glyph
     * @param glyph the input glyph
     * @param ocrLine the ocr output
     * @return true if valid, false otherwise
     */
    public static boolean isValid (Glyph   glyph,
                                   OcrLine ocrLine)
    {
        // Always trust the user
        if ((glyph.getShape() == Shape.TEXT) && glyph.isManualShape()) {
            return true;
        }

        // Check this is not a tuplet
        if (ocrLine.value.equals("3") &&
            (glyph.getShape() == Shape.TUPLET_THREE)) {
            if (logger.isFineEnabled()) {
                logger.fine("This text is a tuplet 3 " + ocrLine + " " + glyph);
            }

            return false;
        }

        if (ocrLine.value.equals("6") &&
            (glyph.getShape() == Shape.TUPLET_SIX)) {
            if (logger.isFineEnabled()) {
                logger.fine("This text is a tuplet 6" + ocrLine + " " + glyph);
            }

            return false;
        }

        // Check for abnormal characters
        WrappedBoolean stripped = new WrappedBoolean(false);
        XmlUtilities.stripNonValidXMLCharacters(ocrLine.value, stripped);

        if (stripped.isSet()) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "This text contains invalid characters" + ocrLine + " " +
                    glyph);
            }

            return false;
        }

        // Check that aspect (height/width) is similar between ocr & glyph
        if (ocrLine.value.length() <= constants.maxCharCountForAspectCheck.getValue()) {
            PixelRectangle box = ocrLine.getContourBox();
            String         str = ocrLine.value;
            Font           font = TextFont.basicFont;
            TextLayout     layout = new TextLayout(str, font, frc);
            Rectangle2D    rect = layout.getBounds();
            double         xRatio = box.width / rect.getWidth();
            double         yRatio = box.height / rect.getHeight();
            double         aRatio = yRatio / xRatio;

            if (logger.isFineEnabled()) {
                logger.fine(
                    ocrLine.toString() + " xRatio:" + (float) xRatio +
                    " yRatio:" + (float) yRatio + " aRatio:" + aRatio);
            }

            // Sign of something wrong
            if ((aRatio < constants.minAspectRatio.getValue()) ||
                (aRatio > constants.maxAspectRatio.getValue())) {
                if (logger.isFineEnabled()) {
                    logger.fine("Invalid " + ocrLine + " " + glyph);
                }

                return false;
            }
        }

        // OK
        return true;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Integer maxCharCountForAspectCheck = new Constant.Integer(
            "CharCount",
            3,
            "Maximum character count to apply aspect check");
        Constant.Ratio   minAspectRatio = new Constant.Ratio(
            0.75,
            "Minimum ratio between ocr aspect and glyph aspect");
        Constant.Ratio   maxAspectRatio = new Constant.Ratio(
            1.2,
            "Maximum ratio between ocr aspect and glyph aspect");
    }
}
