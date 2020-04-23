//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   H e a d C l a s s i f i e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.classifier;

import ij.process.ByteProcessor;
import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Picture.SourceKey;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.util.UriUtil;
import org.audiveris.omr.util.Wrapper;
import org.audiveris.omrdataset.api.OmrShape;

import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import org.audiveris.omrdataset.api.Context;
import static org.audiveris.omrdataset.api.Context.BACKGROUND;
import org.audiveris.omrdataset.api.HeadContext;
import org.audiveris.omrdataset.api.HeadEvaluation;
import org.audiveris.omrdataset.api.HeadShape;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;

/**
 * Class {@code HeadClassifier} is a patch classifier focused of head shapes.
 * <p>
 * As opposed to a glyph classifier which expects a glyph as input, a patch classifier expects
 * a sub-image (a "patch" of fixed size) centered on a given image location.
 *
 * @author Hervé Bitteur
 * @author Raphael Emberger
 */
public class HeadClassifier
{

    private static final Logger logger = LoggerFactory.getLogger(HeadClassifier.class);

    private static final HeadContext context = HeadContext.INSTANCE;

    /** Predefined interline value: {@value}. */
    public static final int INTERLINE = Context.INTERLINE; // was 10;

    /** Height of patch, within scaled image: {@value}. */
    public static final int CONTEXT_HEIGHT = context.getContextHeight(); // was 160;

    /** Width of patch, within scaled image: {@value}. */
    public static final int CONTEXT_WIDTH = context.getContextWidth();// was 40;

    /**
     * Name of file containing the trained model.
     * To be searched in user location first, then in default location second.
     */
    public static final String FILE_NAME = "head-classifier.zip";

    /** The singleton. */
    private static volatile HeadClassifier INSTANCE;

    /** The underlying network.
     * To my knowledge, the ComputationGraph would better fit our needs.
     */
    ///private final ComputationGraph model;
    private final MultiLayerNetwork model;

    /**
     * Private constructor to create a classifier from a existing model.
     */
    private HeadClassifier ()
    {
        // Unmarshal from user or default data, if compatible
        model = load(FILE_NAME);
        logger.info(model.summary());
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of HeadClassifier in the application.
     *
     * @return the instance
     */
    public static HeadClassifier getInstance ()
    {
        if (INSTANCE == null) {
            synchronized (HeadClassifier.class) {
                if (INSTANCE == null) {
                    INSTANCE = new HeadClassifier();
                }
            }
        }

        return INSTANCE;
    }

    //-------------------//
    // getPatchInterline //
    //-------------------//
    /**
     * Report the interline value expected by the patch classifier.
     *
     * @return patch classifier expected interline
     */
    public static int getPatchInterline ()
    {
        return INTERLINE;
    }

    //--------------------//
    // getHeadEvaluations //
    //--------------------//
    /**
     * Report, by decreasing quality, the shapes found at provided location,
     * using the provided interline scaling value.
     *
     * @param sheet        the sheet at hand
     * @param location     provided location in sheet
     * @param interline    relevant interline (staff or system)
     * @param imageWrapper (output) if not null, to be populated by patch image
     * @return the top shapes found
     */
    public HeadEvaluation[] getHeadEvaluations (Sheet sheet,
                                                Point location,
                                                int interline,
                                                Wrapper<BufferedImage> imageWrapper)
    {
        logger.debug("Patching sheet at ({}, {}).", location.x, location.y);

        final Scale scale = sheet.getScale();

        // Select properly scaled source
        final ByteProcessor bp;

        if (interline == scale.getInterline()) {
            bp = sheet.getPicture().getSource(SourceKey.LARGE_TARGET);
        } else if (interline == scale.getSmallInterline()) {
            // Support for hybrid sheet with 2 staff heights
            bp = sheet.getPicture().getSource(SourceKey.SMALL_TARGET);
        } else {
            logger.error("Interline {} is not known by {}", interline, scale);

            return null;
        }

        final int imgWidth = bp.getWidth();
        final int imgHeight = bp.getHeight();

        // From provided location in original image, derive pt in scaled image
        final double ratio = (double) INTERLINE / interline;
        final Point pt = PointUtil.rounded(PointUtil.times(location, ratio));
        final int xMin = pt.x - (CONTEXT_WIDTH / 2);
        final int yMin = pt.y - (CONTEXT_HEIGHT / 2);

        // Extract patch pixels from scaled image
        final int numPixels = context.getNumPixels();
        final double[] pixels = new double[numPixels];

        int idx = 0;
        for (int y = 0; y < CONTEXT_HEIGHT; y++) {
            int ay = yMin + y; // Absolute y

            if ((ay < 0) || (ay >= imgHeight)) {
                // Fill row with background value
                for (int x = 0; x < CONTEXT_WIDTH; x++) {
                    pixels[idx++] = BACKGROUND;
                }
            } else {
                for (int x = 0; x < CONTEXT_WIDTH; x++) {
                    int ax = xMin + x; // Absolute x
                    int val = ((ax < 0) || (ax >= imgWidth)) ? BACKGROUND
                            : (255 - bp.get(idx)); // Inversion!
                    pixels[idx++] = val;
                }
            }
        }
//
//        for (int w = 0; w < CONTEXT_WIDTH; w++) {
//            for (int h = 0; h < CONTEXT_HEIGHT; h++) {
//                int x = xMin + w;
//                int y = yMin + h;
//
//                if ((x < 0) || (x >= bp.getWidth())) {
//                    pixels[h][w] = BACKGROUND;
//                } else if ((y < 0) || (y >= bp.getHeight())) {
//                    pixels[h][w] = BACKGROUND;
//                } else {
//                    pixels[h][w] = bp.get(x, y);
//                }
//            }
//        }

        storePixels(location, pixels);
        //
        // Output patch image if so asked for
        if (imageWrapper != null) {
            imageWrapper.value = imageOf(pixels);
        }

        INDArray features = Nd4j.create(pixels)
                .reshape(1, 1, CONTEXT_HEIGHT, CONTEXT_WIDTH);

        // Network inference
        INDArray output = model.output(features); // TODO: verify this!

        // Extract and sort evaluations
        List<HeadEvaluation> evalList = new ArrayList<>();
        HeadShape[] values = context.getLabels();

        for (int i = 0; i < output.length(); i++) {
            HeadShape shape = values[i];
            double grade = output.getDouble(i);

            evalList.add(new HeadEvaluation(shape, grade));
        }

        Collections.sort(evalList);

        return evalList.toArray(new HeadEvaluation[evalList.size()]);
    }
//
//    //------//
//    // load //
//    //------//
//    /**
//     * Load model from the most suitable classifier data files.
//     * If user files do not exist or cannot be unmarshalled, the default files are used.
//     *
//     * @param fileName file name for classifier data
//     * @return the model loaded
//     */
//    protected ComputationGraph load (String fileName)
//    {
//        ComputationGraph model = null;
//        Path path = WellKnowns.TRAIN_FOLDER.resolve(fileName);
//        logger.debug("Searching for patch classifier model...");
//
//        if (!Files.exists(path)) {
//            logger.info("No model found at {}. Searching in resource folder...", path);
//            path = Paths.get(UriUtil.toURI(WellKnowns.RES_URI, fileName));
//        }
//
//        if (Files.exists(path)) {
//            logger.debug("Found model at {}", path);
//            logger.debug("Loading Model...");
//
//            try {
//                model = KerasModelImport.importKerasModelAndWeights(path.toString());
//            } catch (Exception ex) {
//                logger.warn("Load error {}", ex.toString(), ex);
//            }
//
//            logger.debug("Model loaded.");
//
//            if (!isCompatible(model)) {
//                logger.error("Classifier is not compatible!");
//            } else {
//                logger.debug("Classifier data successfully loaded.");
//            }
//        } else {
//            logger.warn("Couldn't find patch classifier model. Expected: {}", path);
//        }
//
//        return model;
//    }

    //------//
    // load //
    //------//
    /**
     * Load model from the most suitable classifier data files.
     * If user files do not exist or cannot be unmarshalled, the default files are used.
     *
     * @param fileName file name for classifier data
     * @return the model loaded
     */
    protected MultiLayerNetwork load (String fileName)
    {
        MultiLayerNetwork model = null;
        Path path = WellKnowns.TRAIN_FOLDER.resolve(fileName);
        logger.debug("Searching for patch classifier model...");

        if (!Files.exists(path)) {
            logger.info("No model found at {}. Searching in resource folder...", path);
            path = Paths.get(UriUtil.toURI(WellKnowns.RES_URI, fileName));
        }

        if (Files.exists(path)) {
            logger.info("Found head model at {}", path);
            logger.debug("Loading Model...");

            try {
                ///model = KerasModelImport.importKerasModelAndWeights(path.toString());
                model = ModelSerializer.restoreMultiLayerNetwork(path.toFile(), false);

            } catch (Exception ex) {
                logger.warn("Load error {}", ex.toString(), ex);
            }

            logger.info("Model loaded.");
//
//            if (!isCompatible(model)) {
//                logger.error("Classifier is not compatible!");
//            } else {
//                logger.debug("Classifier data successfully loaded.");
//            }
        } else {
            logger.warn("Couldn't find patch classifier model. Expected: {}", path);
        }

        return model;
    }

    //---------//
    // imageOf //
    //---------//
    /**
     * Generate a BufferedImage out of the populated pixels
     *
     * @param pixels populated pixels
     * @return suitable BufferedImage
     */
    private BufferedImage imageOf (double[] pixels)
    {
        final ByteProcessor patch = new ByteProcessor(CONTEXT_WIDTH, CONTEXT_HEIGHT);

        int idx = 0;
        for (int h = 0; h < CONTEXT_HEIGHT; h++) {
            for (int w = 0; w < CONTEXT_WIDTH; w++) {
                patch.set(idx, (int) pixels[idx]);
                idx++;
            }
        }

        return patch.getBufferedImage();
    }

    //--------------//
    // isCompatible //
    //--------------//
    /**
     * Check that the provided model is actually compatible with current Audiveris
     * expectations.
     * <p>
     * Especially the number, order (and names?) of supported classes must match {@link OmrShape}
     * class.
     *
     * @param model the model just loaded
     * @return true if compatible
     */
    private boolean isCompatible (ComputationGraph model)
    {
        //TODO: implement this method
        ComputationGraphConfiguration conf = model.getConfiguration();
        List<String> inputs = conf.getNetworkInputs();
        List<String> outputs = conf.getNetworkOutputs();
        boolean compatability = true;
        compatability &= (inputs.size() == 1);
        compatability &= inputs.get(0).equals("input_1");
        compatability &= (outputs.size() == 1);
        compatability &= outputs.get(0).equals("dense_1_loss");

        return compatability;
    }
//
//    //-----------------//
//    // getOmrShapeList //
//    //-----------------//
//    /**
//     * Get a list of OMR shapes to map the probabilities of the NN to.
//     *
//     * @return Correctly ordered list of OMR shapes to map the probabilities to.
//     */
//    private ArrayList<OmrShape> getOmrShapeList ()
//    {
//        ArrayList<OmrShape> shapes = new ArrayList<>();
//        shapes.add(OmrShape.none);
//        shapes.add(OmrShape.brace);
//        shapes.add(OmrShape.repeatDot);
//        shapes.add(OmrShape.segno);
//        shapes.add(OmrShape.coda);
//        shapes.add(OmrShape.gClef);
//        shapes.add(OmrShape.cClefAlto);
//        shapes.add(OmrShape.cClefTenor);
//        shapes.add(OmrShape.fClef);
//        shapes.add(OmrShape.unpitchedPercussionClef1);
//        shapes.add(OmrShape.gClefChange);
//        shapes.add(OmrShape.cClefAltoChange);
//        shapes.add(OmrShape.cClefTenorChange);
//        shapes.add(OmrShape.fClefChange);
//        shapes.add(OmrShape.clef8);
//        shapes.add(OmrShape.clef15);
//        shapes.add(OmrShape.timeSig0);
//        shapes.add(OmrShape.timeSig1);
//        shapes.add(OmrShape.timeSig2);
//        shapes.add(OmrShape.timeSig3);
//        shapes.add(OmrShape.timeSig4);
//        shapes.add(OmrShape.timeSig5);
//        shapes.add(OmrShape.timeSig6);
//        shapes.add(OmrShape.timeSig7);
//        shapes.add(OmrShape.timeSig8);
//        shapes.add(OmrShape.timeSig9);
//        shapes.add(OmrShape.timeSigCommon);
//        shapes.add(OmrShape.timeSigCutCommon);
//        shapes.add(OmrShape.noteheadBlack);
//        shapes.add(OmrShape.noteheadBlackSmall);
//        shapes.add(OmrShape.noteheadHalf);
//        shapes.add(OmrShape.noteheadHalfSmall);
//        shapes.add(OmrShape.noteheadWhole);
//        shapes.add(OmrShape.noteheadWholeSmall);
//        shapes.add(OmrShape.noteheadDoubleWhole);
//        shapes.add(OmrShape.augmentationDot);
//        shapes.add(OmrShape.flag8thUp);
//        shapes.add(OmrShape.flag16thUp);
//        shapes.add(OmrShape.flag32ndUp);
//        shapes.add(OmrShape.flag64thUp);
//        shapes.add(OmrShape.flag128thUp);
//        shapes.add(OmrShape.flag8thDown);
//        shapes.add(OmrShape.flag16thDown);
//        shapes.add(OmrShape.flag32ndDown);
//        shapes.add(OmrShape.flag64thDown);
//        shapes.add(OmrShape.flag128thDown);
//        shapes.add(OmrShape.accidentalFlat);
//        shapes.add(OmrShape.accidentalFlatSmall);
//        shapes.add(OmrShape.accidentalNatural);
//        shapes.add(OmrShape.accidentalNaturalSmall);
//        shapes.add(OmrShape.accidentalSharp);
//        shapes.add(OmrShape.accidentalSharpSmall);
//        shapes.add(OmrShape.accidentalDoubleSharp);
//        shapes.add(OmrShape.accidentalDoubleFlat);
//        shapes.add(OmrShape.keyFlat);
//        shapes.add(OmrShape.keyNatural);
//        shapes.add(OmrShape.keySharp);
//        shapes.add(OmrShape.articAccentAbove);
//        shapes.add(OmrShape.articAccentBelow);
//        shapes.add(OmrShape.articStaccatoAbove);
//        shapes.add(OmrShape.articStaccatoBelow);
//        shapes.add(OmrShape.articTenutoAbove);
//        shapes.add(OmrShape.articTenutoBelow);
//        shapes.add(OmrShape.articStaccatissimoAbove);
//        shapes.add(OmrShape.articStaccatissimoBelow);
//        shapes.add(OmrShape.articMarcatoAbove);
//        shapes.add(OmrShape.articMarcatoBelow);
//        shapes.add(OmrShape.fermataAbove);
//        shapes.add(OmrShape.fermataBelow);
//        shapes.add(OmrShape.caesura);
//        shapes.add(OmrShape.restLonga);
//        shapes.add(OmrShape.restDoubleWhole);
//        shapes.add(OmrShape.restWhole);
//        shapes.add(OmrShape.restHalf);
//        shapes.add(OmrShape.restQuarter);
//        shapes.add(OmrShape.rest8th);
//        shapes.add(OmrShape.rest16th);
//        shapes.add(OmrShape.rest32nd);
//        shapes.add(OmrShape.rest64th);
//        shapes.add(OmrShape.rest128th);
//        shapes.add(OmrShape.restHBar);
//        shapes.add(OmrShape.dynamicPiano);
//        shapes.add(OmrShape.dynamicMezzo);
//        shapes.add(OmrShape.dynamicForte);
//        shapes.add(OmrShape.dynamicPPPPP);
//        shapes.add(OmrShape.dynamicPPPP);
//        shapes.add(OmrShape.dynamicPPP);
//        shapes.add(OmrShape.dynamicPP);
//        shapes.add(OmrShape.dynamicMP);
//        shapes.add(OmrShape.dynamicMF);
//        shapes.add(OmrShape.dynamicFF);
//        shapes.add(OmrShape.dynamicFFF);
//        shapes.add(OmrShape.dynamicFFFF);
//        shapes.add(OmrShape.dynamicFFFFF);
//        shapes.add(OmrShape.dynamicFortePiano);
//        shapes.add(OmrShape.dynamicSforzando1);
//        shapes.add(OmrShape.dynamicSforzato);
//        shapes.add(OmrShape.dynamicRinforzando2);
//        shapes.add(OmrShape.graceNoteAcciaccaturaStemUp);
//        shapes.add(OmrShape.graceNoteAppoggiaturaStemUp);
//        shapes.add(OmrShape.graceNoteAcciaccaturaStemDown);
//        shapes.add(OmrShape.graceNoteAppoggiaturaStemDown);
//        shapes.add(OmrShape.ornamentTrill);
//        shapes.add(OmrShape.ornamentTurn);
//        shapes.add(OmrShape.ornamentTurnInverted);
//        shapes.add(OmrShape.ornamentMordent);
//        shapes.add(OmrShape.stringsDownBow);
//        shapes.add(OmrShape.stringsUpBow);
//        shapes.add(OmrShape.arpeggiato);
//        shapes.add(OmrShape.keyboardPedalPed);
//        shapes.add(OmrShape.keyboardPedalUp);
//        shapes.add(OmrShape.tuplet3);
//        shapes.add(OmrShape.tuplet6);
//        shapes.add(OmrShape.fingering0);
//        shapes.add(OmrShape.fingering1);
//        shapes.add(OmrShape.fingering2);
//        shapes.add(OmrShape.fingering3);
//        shapes.add(OmrShape.fingering4);
//        shapes.add(OmrShape.fingering5);
//        return shapes;
//    }
//
    //-------------//
    // storePixels //
    //-------------//

    private void storePixels (Point location,
                              double[] pixels)
    {
        Path path = WellKnowns.TEMP_FOLDER.resolve("p_" + location.x + "_" + location.y + ".png");

        try {
            ImageIO.write(imageOf(pixels), "png", path.toFile());
            logger.info("Patch saved as {}", path);
        } catch (Exception ex) {
            logger.warn("Could not store image {}", path, ex);
        }
    }
}
