//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  A n n o t a t i o n s S t e p                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright ©  Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.step;

import ij.process.ByteProcessor;

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.classifier.Annotation;
import org.audiveris.omr.classifier.AnnotationIndex;
import org.audiveris.omr.classifier.ui.AnnotationBoard;
import org.audiveris.omr.classifier.ui.AnnotationService;
import org.audiveris.omr.classifier.ui.AnnotationView;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.ui.PixelBoard;
import org.audiveris.omr.sheet.ui.SheetTab;
import org.audiveris.omr.ui.BoardsPane;
import org.audiveris.omr.ui.view.ScrollView;
import org.audiveris.omr.util.FileUtil;
import org.audiveris.omr.util.MultipartUtility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Class {@code AnnotationsStep} implements <b>ANNOTATIONS</b>, which delegates to the
 * full-page classifier the detection and classification of most symbols of the sheet.
 * <p>
 * The full-page classifier is provided a sheet image properly scaled and returns a collection of
 * annotations (one per detected symbol).
 * <p>
 * For the time being, the classifier is accessed as a (local) web service.
 *
 * @author Hervé Bitteur
 */
public class AnnotationsStep
        extends AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AnnotationsStep.class);

    //~ Methods ------------------------------------------------------------------------------------
    //----------------------//
    // displayAnnotationTab //
    //----------------------//
    public static void displayAnnotationTab (Sheet sheet)
    {
        AnnotationService service = (AnnotationService) sheet.getAnnotationIndex().getEntityService();
        AnnotationView view = new AnnotationView(service, sheet);
        sheet.getStub().getAssembly().addViewTab(
                SheetTab.ANNOTATION_TAB,
                new ScrollView(view),
                new BoardsPane(
                        new PixelBoard(sheet),
                        new AnnotationBoard(sheet.getAnnotationIndex().getEntityService(), true)));
    }

    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Step step,
                           Sheet sheet)
    {
        if (constants.displayAnnotations.isSet()) {
            displayAnnotationTab(sheet);
        }
    }

    //------//
    // doit //
    //------//
    /**
     * {@inheritDoc}
     *
     * @param sheet the provided sheet
     * @throws StepException
     */
    @Override
    public void doit (Sheet sheet)
            throws StepException
    {
        try {
            // Scale image if different from expected interline
            RunTable binary = sheet.getPicture().getTable(Picture.TableKey.BINARY);
            ByteProcessor binBuffer = binary.getBuffer();
            int interline = sheet.getScale().getInterline();
            int expected = constants.expectedInterline.getValue();
            double ratio = (double) expected / interline;
            ByteProcessor buf = (ratio != 1.0) ? scaledBuffer(binBuffer, ratio) : binBuffer;
            BufferedImage img = buf.getBufferedImage();
            String name = sheet.getId();
            File file = WellKnowns.TEMP_FOLDER.resolve(name + ".png").toFile();
            ImageIO.write(img, "png", file);
            logger.info("Saved {}", file);

            //
            // Post image to web service
            // Receive annotations (json file)
            List<Annotation> annotations = Annotation.readAnnotations("Issue-87.json", ratio);
            ///List<Annotation> annotations = postRequest(file, ratio);
            //
            AnnotationIndex index = sheet.getAnnotationIndex();

            for (Annotation annotation : annotations) {
                index.register(annotation);
            }

            index.setModified(true);
        } catch (Exception ex) {
            logger.warn("Exception in Annotations step", ex);
        }
    }

    //-------------//
    // postRequest //
    //-------------//
    private List<Annotation> postRequest (File file,
                                          double ratio)
            throws Exception
    {
        MultipartUtility mu = new MultipartUtility(
                constants.webServiceUrl.getValue(),
                StandardCharsets.UTF_8);
        logger.info("Posting image {}", file);
        mu.addFilePart("image", file);
        logger.info("Waiting for response...");

        final long start = System.currentTimeMillis();
        List<String> answers = mu.finish();
        final long stop = System.currentTimeMillis();
        logger.info("Duration= {} ms", stop - start);
        logger.info("Answers= {}", answers);

        // Save json string into json file
        String radix = FileUtil.getNameSansExtension(file);
        Path jsonPath = file.toPath().resolveSibling(radix + ".json");
        byte[] bytes = answers.get(0).getBytes(StandardCharsets.UTF_8);
        Files.write(jsonPath, bytes);

        List<Annotation> annotations = Annotation.readAnnotations(jsonPath.toString(), ratio);

        return annotations;
    }

    //--------------//
    // scaledBuffer //
    //--------------//
    private ByteProcessor scaledBuffer (ByteProcessor binBuffer,
                                        double ratio)
    {
        final int scaledWidth = (int) Math.ceil(binBuffer.getWidth() * ratio);
        final int scaledHeight = (int) Math.ceil(binBuffer.getHeight() * ratio);
        final ByteProcessor scaledBuffer = (ByteProcessor) binBuffer.resize(
                scaledWidth,
                scaledHeight,
                true); // True => use averaging when down-scaling

        return scaledBuffer;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean displayAnnotations = new Constant.Boolean(
                true,
                "Should we display the view on annotations?");

        private final Constant.Integer expectedInterline = new Constant.Integer(
                "pixels",
                10,
                "Expected interline for classifier input image");

        private final Constant.String webServiceUrl = new Constant.String(
                "http://127.0.0.1:5000/classify",
                "URL of Detection Web Service");
    }
}
