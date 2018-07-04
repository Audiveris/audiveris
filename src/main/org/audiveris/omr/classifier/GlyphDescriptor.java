//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 G l y p h D e s c r i p t o r                                  //
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

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.glyph.Glyph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Class {@code GlyphDescriptor} provides glyph features for shape classifiers.
 *
 * @author Hervé Bitteur
 */
public abstract class GlyphDescriptor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GlyphDescriptor.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Descriptor name. */
    private final String name;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code GlyphDescriptor} object.
     *
     * @param name distinctive descriptor name
     */
    public GlyphDescriptor (String name)
    {
        this.name = name;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Export the provided collection of samples to a file (using CSV format).
     *
     * @param radix     file name radix
     * @param samples   the samples to export
     * @param withNorms should samples norms be exported as well? (case of a training set)
     */
    public void export (String radix,
                        Collection<Sample> samples,
                        boolean withNorms)
    {
        try {
            final String ext = "." + name + ".csv";
            final Path path = WellKnowns.TRAIN_FOLDER.resolve(radix + ext);
            final PrintWriter out = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    new FileOutputStream(path.toFile()),
                                    WellKnowns.FILE_ENCODING)));

            for (Sample sample : samples) {
                for (double in : getFeatures(sample, sample.getInterline())) {
                    out.print(in);
                    out.print(",");

                    //
                    //                if (pop != null) {
                    //                    pop.includeValue(in);
                    //                }
                }

                ///out.println(sample.getShape().getPhysicalShape()); // Shape name
                out.println(sample.getShape().getPhysicalShape().ordinal()); // Shape ordinal
            }

            //
            //        if (withNorms) {
            //            logger.info("Img {}", pop);
            //        }
            //
            out.flush();
            out.close();
            logger.info("{} {} samples saved in {}", samples.size(), radix, path.toAbsolutePath());
        } catch (Exception ex) {
            logger.warn("Could not save {} samples " + ex, radix, ex);
        }
    }

    /**
     * Report the features labels.
     *
     * @return the array of feature labels, or null if none
     */
    public abstract String[] getFeatureLabels ();

    /**
     * Gather the various features meant to describe a glyph or a shape sample.
     *
     * @param glyph     the glyph (or sample) to describe
     * @param interline the related staff interline
     * @return the glyph features, an array of size length()
     */
    public abstract double[] getFeatures (Glyph glyph,
                                          int interline);

    /**
     * Report a name for this descriptor
     *
     * @return a typical name
     */
    public String getName ()
    {
        return name;
    }

    /**
     * Report the number of features provided.
     *
     * @return the number of features
     */
    public abstract int length ();
}
