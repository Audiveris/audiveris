//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  P a t c h C l a s s i f i e r                                 //
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
package org.audiveris.omr.classifier;

import ij.process.ByteProcessor;
import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Picture.SourceKey;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.util.UriUtil;
import org.audiveris.omrdataset.api.OmrShape;
import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code PatchClassifier} is the first shot of a patch classifier.
 * <p>
 * As opposed to a glyph classifier which expects a glyph as input, a patch classifier expects
 * a sub-image (a "patch" of fixed size) centered on a given image location.
 *
 * @author Hervé Bitteur
 */
public class PatchClassifier
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PatchClassifier.class);

    /** Predefined interline value: {@value}. */
    public static final int INTERLINE = 10;

    /** Height of patch, within scaled image: {@value}. */
    public static final int CONTEXT_HEIGHT = 160;

    /** Width of patch, within scaled image: {@value}. */
    public static final int CONTEXT_WIDTH = 40;

    /** Value used for background pixel feature: {@value}. */
    public static final int BACKGROUND = 0;

    /** Value used for foreground pixel feature: {@value}. */
    public static final int FOREGROUND = 255;

    /**
     * Name of file containing the trained model.
     * To be searched in user location first, then in default location second.
     */
    public static final String FILE_NAME = "patch-classifier.h5";

    /** The singleton. */
    private static volatile PatchClassifier INSTANCE;

    //~ Instance fields ----------------------------------------------------------------------------
    /** The underlying network.
     * To my knowledge, the ComputationGraph would better fit our needs.
     */
    private final ComputationGraph model;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Private constructor to create a classifier from a existing model.
     */
    private PatchClassifier ()
    {
        // Unmarshal from user or default data, if compatible
        model = load(FILE_NAME);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of PatchClassifier in the application.
     *
     * @return the instance
     */
    public static PatchClassifier getInstance ()
    {
        if (INSTANCE == null) {
            synchronized (PatchClassifier.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PatchClassifier();
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

    //-------------------//
    // getOmrEvaluations //
    //-------------------//
    /**
     * Report, by decreasing quality, the shapes found at provided location,
     * using the provided interline scaling value.
     *
     * @param sheet     the sheet at hand
     * @param location  provided location in sheet
     * @param interline relevant interline (staff or system)
     * @return the top shapes found
     */
    public OmrEvaluation[] getOmrEvaluations (Sheet sheet,
                                              Point location,
                                              int interline)
    {
        logger.info("Patching sheet at ({}, {}).", location.x, location.y);
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

        // From provided location in original image, derive pt in scaled image
        final double ratio = (double) INTERLINE / interline;
        final Point pt = PointUtil.rounded(PointUtil.times(location, ratio));
        int xMin = pt.x - (int) ((CONTEXT_WIDTH / 2) * ratio);
        int yMin = pt.y - (int) ((CONTEXT_HEIGHT / 2) * ratio);

        // Extract patch pixels from scaled image
        final double[][] pixels = new double[CONTEXT_HEIGHT][CONTEXT_WIDTH];
        for (int w = 0; w < CONTEXT_WIDTH; w++) {
            for (int h = 0; h < CONTEXT_HEIGHT; h++) {
                int x = xMin + w;
                int y = yMin + h;
                if (x < 0 || x >= bp.getWidth()) {
                    pixels[h][w] = 0.0;
                } else if (y < 0 || y >= bp.getHeight()) {
                    pixels[h][w] = 0.0;
                } else {
                    pixels[h][w] = bp.get(x, y);
                }
            }
        }

        INDArray features = Nd4j.create(pixels).reshape(1, 1, CONTEXT_HEIGHT, CONTEXT_WIDTH);

        // Network inference
        INDArray output = model.outputSingle(features);

        // Extract and sort evaluations
        List<OmrEvaluation> evalList = new ArrayList<>();
        OmrShape[] values = OmrShape.values();

        for (int i = 0; i < output.length(); i++) {
            OmrShape omrShape = values[i];
            double grade = output.getDouble(i);

            evalList.add(new OmrEvaluation(omrShape, grade));
        }

        Collections.sort(evalList);

        return evalList.toArray(new OmrEvaluation[evalList.size()]);
    }

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
    protected ComputationGraph load (String fileName)
    {
        ComputationGraph model = null;
        Path path = WellKnowns.TRAIN_FOLDER.resolve(fileName);
        logger.debug("Searching for patch classifier model...");
        if (!Files.exists(path)) {
            logger.info("No model found at {}. Searching in resource folder...", path);
            path = Paths.get(UriUtil.toURI(WellKnowns.RES_URI, fileName));
        }
        if (Files.exists(path)) {
            logger.debug("Found model at {}", path);
            logger.debug("Loading Model...");
            try {
                model = KerasModelImport.importKerasModelAndWeights(path.toString());
            } catch (Exception ex) {
                logger.warn("Load error {}", ex.toString(), ex);
            }
            logger.debug("Model loaded.");

            if (!isCompatible(model)) {
                logger.error("Classifier is not compatible!");
            } else {
                logger.debug("Classifier data successfully loaded.");
            }
        } else {
            logger.warn("Couldn't find patch classifier model. Expected: {}", path);
        }
        return model;
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
        compatability &= inputs.size() == 1;
        compatability &= inputs.get(0).equals("input_1");
        compatability &= outputs.size() == 1;
        compatability &= outputs.get(0).equals("dense_1_loss");
        return compatability;
    }
}
