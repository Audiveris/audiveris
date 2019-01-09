//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            M o d e l S y s t e m S e r i a l i z e r                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.api.Updater;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;

import static org.deeplearning4j.util.ModelSerializer.OLD_UPDATER_BIN;
import static org.deeplearning4j.util.ModelSerializer.UPDATER_BIN;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.factory.Nd4j;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE;

/**
 * Class {@code ModelSystemSerializer} is a {@link ModelSerializer} operating on a
 * (zip) file system.
 *
 * @author Hervé Bitteur
 */
public abstract class ModelSystemSerializer
{

    /**
     * Load a multi layer network from a file system
     *
     * @param root        the root path of file system
     * @param loadUpdater true for loading updater
     * @return the loaded multi layer network
     * @throws IOException if something goes wrong during IO operations
     */
    public static MultiLayerNetwork restoreMultiLayerNetwork (Path root,
                                                              boolean loadUpdater)
            throws IOException
    {
        boolean gotConfig = false;
        boolean gotCoefficients = false;
        boolean gotOldUpdater = false;
        boolean gotUpdaterState = false;
        boolean gotPreProcessor = false;

        String json = "";
        INDArray params = null;
        Updater updater = null;
        INDArray updaterState = null;
        DataSetPreProcessor preProcessor = null;

        final Path config = root.resolve("configuration.json");

        if (Files.exists(config)) {
            //restoring configuration
            InputStream stream = Files.newInputStream(config);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = "";
            StringBuilder js = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                js.append(line).append("\n");
            }

            json = js.toString();

            reader.close();
            stream.close();
            gotConfig = true;
        }

        final Path coefficients = root.resolve("coefficients.bin");

        if (Files.exists(coefficients)) {
            InputStream stream = Files.newInputStream(coefficients);
            DataInputStream dis = new DataInputStream(new BufferedInputStream(stream));
            params = Nd4j.read(dis);

            dis.close();
            gotCoefficients = true;
        }

        if (loadUpdater) {
            //This can be removed a few releases after 0.4.1...
            final Path oldUpdaters = root.resolve(OLD_UPDATER_BIN);

            if (Files.exists(oldUpdaters)) {
                InputStream stream = Files.newInputStream(oldUpdaters);
                ObjectInputStream ois = new ObjectInputStream(stream);

                try {
                    updater = (Updater) ois.readObject();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

                gotOldUpdater = true;
            }

            final Path updaterStateEntry = root.resolve(UPDATER_BIN);

            if (updaterStateEntry != null) {
                InputStream stream = Files.newInputStream(updaterStateEntry);
                DataInputStream dis = new DataInputStream(stream);
                updaterState = Nd4j.read(dis);

                dis.close();
                gotUpdaterState = true;
            }
        }

        final Path prep = root.resolve("preprocessor.bin");

        if (Files.exists(prep)) {
            InputStream stream = Files.newInputStream(prep);
            ObjectInputStream ois = new ObjectInputStream(stream);

            try {
                preProcessor = (DataSetPreProcessor) ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            gotPreProcessor = true;
        }

        if (gotConfig && gotCoefficients) {
            MultiLayerConfiguration confFromJson = MultiLayerConfiguration.fromJson(json);
            MultiLayerNetwork network = new MultiLayerNetwork(confFromJson);
            network.init(params, false);

            if (gotUpdaterState && (updaterState != null)) {
                network.getUpdater().setStateViewArray(network, updaterState, false);
            } else if (gotOldUpdater && (updater != null)) {
                network.setUpdater(updater);
            }

            return network;
        } else {
            throw new IllegalStateException(
                    "Model wasn't found within file: gotConfig: [" + gotConfig
                            + "], gotCoefficients: [" + gotCoefficients + "], gotUpdater: ["
                    + gotUpdaterState
                            + "]");
        }
    }

    /**
     * Write a model to a file system
     *
     * @param model       the model to save
     * @param root        the root path of file system
     * @param saveUpdater whether to save the updater for the model or not
     * @throws IOException if something goes wrong during IO operations
     */
    public static void writeModel (Model model,
                                   Path root,
                                   boolean saveUpdater)
            throws IOException
    {
        {
            // save json first
            String json = "";

            if (model instanceof MultiLayerNetwork) {
                json = ((MultiLayerNetwork) model).getLayerWiseConfigurations().toJson();
            } else if (model instanceof ComputationGraph) {
                json = ((ComputationGraph) model).getConfiguration().toJson();
            }

            Path config = root.resolve("configuration.json");
            OutputStream bos = new BufferedOutputStream(Files.newOutputStream(config, CREATE));
            Writer writer = new OutputStreamWriter(bos, "UTF-8");
            writer.write(json);
            writer.close();
        }

        {
            Path coefficients = root.resolve("coefficients.bin");
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(
                    Files.newOutputStream(coefficients, CREATE)));
            Nd4j.write(model.params(), dos);
            dos.flush();
            dos.close();
        }

        if (saveUpdater) {
            INDArray updaterState = null;

            if (model instanceof MultiLayerNetwork) {
                updaterState = ((MultiLayerNetwork) model).getUpdater().getStateViewArray();
            } else if (model instanceof ComputationGraph) {
                updaterState = ((ComputationGraph) model).getUpdater().getStateViewArray();
            }

            if ((updaterState != null) && (updaterState.length() > 0)) {
                Path updater = root.resolve(UPDATER_BIN);
                DataOutputStream dos = new DataOutputStream(
                        new BufferedOutputStream(Files.newOutputStream(updater, CREATE)));
                Nd4j.write(updaterState, dos);
                dos.flush();
                dos.close();
            }
        }
    }
}
