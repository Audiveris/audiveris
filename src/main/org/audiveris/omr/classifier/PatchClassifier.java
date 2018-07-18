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

import org.apache.commons.io.FileUtils;

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Picture.SourceKey;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.util.UriUtil;
import org.audiveris.omr.util.ZipFileSystem;
import org.audiveris.omrdataset.api.OmrShape;

import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.util.ModelSerializer;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
    public static final int CONTEXT_HEIGHT = 96;

    /** Width of patch, within scaled image: {@value}. */
    public static final int CONTEXT_WIDTH = 48;

    /** Value used for background pixel feature: {@value}. */
    public static final int BACKGROUND = 0;

    /** Value used for foreground pixel feature: {@value}. */
    public static final int FOREGROUND = 255;

    /**
     * Name of file containing the trained model.
     * To be searched in user location first, then in default location second.
     */
    public static final String FILE_NAME = "patch-classifier.zip";

    /** The singleton. */
    private static volatile PatchClassifier INSTANCE;

    //~ Instance fields ----------------------------------------------------------------------------
    /** The underlying network.
     * TODO: Either a simple MultiLayerNetwork or a more complex ComputationGraph
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
        int xMin = pt.x - (CONTEXT_WIDTH / 2);
        int yMin = pt.y - (CONTEXT_HEIGHT / 2);

        // Extract patch pixels from scaled image
        final double[] pixels = new double[CONTEXT_HEIGHT * CONTEXT_WIDTH];
        int i = 0;

        for (int y = yMin; y < (yMin + CONTEXT_HEIGHT); y++) {
            for (int x = xMin; x < (xMin + CONTEXT_WIDTH); x++) {
                int val = bp.get(x, y);
                //TODO: val = 255 - val; // Inversion????? CHECK THIS
                pixels[i++] = val;
            }
        }

        INDArray features = Nd4j.create(pixels);

        // Network inference
        //TODO: I have not used ComputationGraph yet, so please check this is the correct method
        INDArray output = model.outputSingle(features);

        // Extract and sort evaluations
        List<OmrEvaluation> evalList = new ArrayList<OmrEvaluation>();
        OmrShape[] values = OmrShape.values();

        for (int is = 0; is < values.length; is++) {
            OmrShape omrShape = values[is];
            double grade = output.getDouble(is);

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
        // First, try user data, if any, in local EVAL folder
        logger.debug("AbstractClassifier. Trying user data");

        {
            final Path path = WellKnowns.TRAIN_FOLDER.resolve(fileName);

            if (Files.exists(path)) {
                try {
                    Path root = ZipFileSystem.open(path);
                    logger.debug("loadModel...");

                    ComputationGraph model = loadModel(root);
                    logger.debug("loaded.");
                    root.getFileSystem().close();

                    if (!isCompatible(model)) {
                        logger.warn(
                                "Obsolete classifier user data in {}, trying default data",
                                path);
                    } else {
                        // Tell user we are not using the default
                        logger.info("Classifier data loaded from local {}", path);

                        return model; // Normal exit
                    }
                } catch (Exception ex) {
                    logger.warn("Load error {}", ex.toString(), ex);
                }
            }
        }

        // Second, use default data (in program RES folder)
        logger.debug("AbstractClassifier. Trying default data");

        final URI uri = UriUtil.toURI(WellKnowns.RES_URI, fileName);

        try {
            // Must be a path to a true zip *file*
            final Path zipPath;
            logger.debug("uri={}", uri);

            if (uri.toString().startsWith("jar:")) {
                // We have a .zip within a .jar
                // Quick fix: copy the .zip into a separate temp file
                // Investigate a better solution!
                File tmpFile = File.createTempFile("AbstractClassifier-", ".tmp");
                logger.debug("tmpFile={}", tmpFile);
                tmpFile.deleteOnExit();

                InputStream is = uri.toURL().openStream();
                FileUtils.copyInputStreamToFile(is, tmpFile);
                is.close();
                zipPath = tmpFile.toPath();
            } else {
                zipPath = Paths.get(uri);
            }

            final Path root = ZipFileSystem.open(zipPath);
            ComputationGraph model = loadModel(root);
            root.getFileSystem().close();

            if (!isCompatible(model)) {
                logger.warn(
                        "Obsolete classifier default data in {}, please retrain from scratch",
                        uri);
            } else {
                logger.info("Classifier data loaded from default uri {}", uri);

                return model; // Normal exit
            }
        } catch (Exception ex) {
            logger.warn("Load error on {} {}", uri, ex.toString(), ex);
        }

        return null; // No suitable model found
    }

    //-----------//
    // loadModel //
    //-----------//
    /**
     * Load the model from the provided root path (root of zip archive).
     *
     * @param root provided root path
     * @return the loaded trained model
     * @throws IOException if load failed
     */
    protected ComputationGraph loadModel (Path root)
            throws IOException
    {
        InputStream is = null;

        try {
            final Path path = root.resolve(FILE_NAME);
            is = Files.newInputStream(path);

            //TODO: check this method
            return ModelSerializer.restoreComputationGraph(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
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
        return true;
    }
}
