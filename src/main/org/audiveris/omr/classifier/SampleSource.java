//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S a m p l e S o u r c e                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import java.util.List;

/**
 * Interface {@code SampleSource} defines a source of samples
 *
 * @author Hervé Bitteur
 */
public interface SampleSource
{

    /**
     * Get the collection of samples to test.
     *
     * @return the collection of samples
     */
    List<Sample> getTestSamples ();

    /**
     * Get the collection of samples for training.
     *
     * @return the collection of samples
     */
    List<Sample> getTrainSamples ();

    /**
     * A basic source of samples, the same set being used for training and for test.
     */
    static class ConstantSource
            implements SampleSource
    {

        /** The underlying collection of samples. */
        private final List<Sample> samples;

        /**
         * Create a ConstantSource object.
         *
         * @param samples the underlying collection of samples.
         */
        public ConstantSource (List<Sample> samples)
        {
            this.samples = samples;
        }

        @Override
        public List<Sample> getTestSamples ()
        {
            return samples;
        }

        @Override
        public List<Sample> getTrainSamples ()
        {
            return samples;
        }
    }
}
