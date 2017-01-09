//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      R e s t o r e T e s t                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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

import org.audiveris.omr.classifier.ShapeDescription;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Class {@code RestoreTest}
 *
 * @author Hervé Bitteur
 */
public class RestoreTest
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(RestoreTest.class);

    //~ Methods ------------------------------------------------------------------------------------
    public static void main (String[] args)
            throws Exception
    {
        // 1/ Restore MLP
        //---------------
        File mlpFile = Paths.get("data/eval/model.zip").toFile();
        MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(mlpFile);

        String json = model.conf().toJson();
        System.out.println(json);

        // 2/ Retrieve known means and stds
        //---------------------------------
        INDArray columnMeans = null;
        INDArray columnStds = null;
        ZipFile zipFile = new ZipFile(new File("data/eval/norms.zip"));

        ZipEntry means = zipFile.getEntry("means.bin");

        if (means != null) {
            InputStream stream = zipFile.getInputStream(means);
            DataInputStream dis = new DataInputStream(stream);
            columnMeans = Nd4j.read(dis);
            dis.close();
        }

        ZipEntry stds = zipFile.getEntry("stds.bin");

        if (stds != null) {
            InputStream stream = zipFile.getInputStream(stds);
            DataInputStream dis = new DataInputStream(stream);
            columnStds = Nd4j.read(dis);
            columnStds.addi(Nd4j.scalar(Nd4j.EPS_THRESHOLD)); // Safer to avoid division by 0
            dis.close();
        }

        zipFile.close();

        final String[] vars = ShapeDescription.getParameterLabels();

        for (int i = 0; i < ShapeDescription.FEATURE_COUNT; i++) {
            System.out.println(
                    String.format(
                            "i:%2d %6s mean:%.5f std:%.5f",
                            i,
                            vars[i],
                            columnMeans.getDouble(i),
                            columnStds.getDouble(i)));
        }

        // 3/ Get a test sample
        //---------------------
        //        //First: get the dataset using the record reader. CSVRecordReader handles loading/parsing
        //        int numLinesToSkip = 0;
        //        String delimiter = ",";
        //        RecordReader recordReader = new CSVRecordReader(numLinesToSkip, delimiter);
        //        recordReader.initialize(new FileSplit(new File("data/eval/samples-MIX-1line.csv")));
        //
        //        //Second: the RecordReaderDataSetIterator handles conversion to DataSet objects, ready for use in neural network
        //        int labelIndex = 46; //5 values in each row of the iris.txt CSV: 4 input features followed by an integer label (class) index. Labels are the 5th value (index 4) in each row
        //        int numClasses = 116; //3 classes (types of iris flowers) in the iris data set. Classes have integer values 0, 1 or 2
        //        int batchSize = 1002; //Iris data set: 150 examples total. We are loading all of them into one DataSet (not recommended for large data sets)
        //        DataSetIterator iterator = new RecordReaderDataSetIterator(
        //                recordReader,
        //                batchSize,
        //                labelIndex,
        //                numClasses);
        //
        //        DataSet next = iterator.next();
        //
        //        ////next.normalizeZeroMeanZeroUnitVariance(); // needed
        //final DataSet testData = next;
        //
        final double[] doubleFeatures = new double[]{
            //            0.19254085, 0.13150686, 0.001763817, 0.001763817,
            //            0.001763817, 0.001763817, 0.001763817, 0.001763817,
            //            0.001763817, 0.001763817, 0.001763817, 0.10312766,
            //            0.01834676, 0.07047285, 0.001763817, 0.001763817,
            //            0.001763817, 0.001763817, 0.001763817, 0.001763817,
            //            0.001763817, 0.001763817, 0.001763817, 0.02896094,
            //            0.059324477, 0.059324477, 0.001763817, 0.001763817,
            //            0.001763817, 0.001763817, 0.001763817, 0.001763817,
            //            0.001763817, 0.001763817, 0.001763817, 0.13, 0.4, 0.4,
            //            0.07877219, 0.0, 0.07877219, 0.0, 0.0, 0.0, 0.0, 1.0 //, 0 = DOT_set
            0.050043695, 0.08443476, 0.04209365, 0.03514014, 0.059324477, 0.19254085, 0.19254085,
            0.08443476, 0.13150686, 0.059324477, 0.050043695,
            0.19254085, 0.19254085, 0.03514014, 0.13150686,
            0.059324477, 0.059324477, 0.19254085, 0.03514014,
            0.013714449, 0.19254085, 0.10312766, 0.013714449,
            0.08443476, 0.02896094, 0.07047285, 0.19254085,
            0.13150686, 0.04209365, 0.13150686, 0.08443476,
            0.059324477, 0.19254085, 0.19254085, 0.08443476,
            2.0181406, 1.3809524, 3.9047618, 0.05851518, 0.008297016,
            0.6159325, -0.008588942, -0.0072715157, 0.014872165,
            0.035781763, 2.8275862 //, 41 = TIME_TWO_FOUR
        };
        final INDArray features = Nd4j.create(doubleFeatures);
        logger.info("features columns:{} rows:{}", features.columns(), features.rows());

        // 4/ Normalize our test data, using known means/stds
        //---------------------------------------------------
        features.subiRowVector(columnMeans);
        features.diviRowVector(columnStds);

        System.out.println("isVector = " + features.isVector()); // true
        System.out.println("isRowVector = " + features.isRowVector()); // true

        for (int i = 0; i < ShapeDescription.FEATURE_COUNT; i++) {
            System.out.println(String.format("i:%2d %6s %.5f", i, vars[i], features.getDouble(i)));
        }

        // 5/ evaluate the model on the test set
        //--------------------------------------
        INDArray output = model.output(features);

        //        org.deeplearning4j.eval.Evaluation eval = new org.deeplearning4j.eval.Evaluation(
        //                numClasses);
        //        eval.eval(testData.getLabels(), output);
        //        log.info("eval: {}", eval);
        //        log.info(eval.stats());
        //
        logger.info("output columns:{} rows:{}", output.columns(), output.rows());

        for (Shape shape : ShapeSet.allPhysicalShapes) {
            final int i = shape.ordinal();
            final double score = output.getDouble(i);

            if (score >= 1e-6) {
                System.out.println(String.format("i:%3d %-16s %f", i, shape.toString(), score));
            }
        }
    }
}
