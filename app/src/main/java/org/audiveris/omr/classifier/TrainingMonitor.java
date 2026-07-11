//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  T r a i n i n g M o n i t o r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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

//
//import org.deeplearning4j.optimize.api.IterationListener;
//
/**
 * Monitoring interface about the training status of a classifier.
 */
public interface TrainingMonitor //        extends IterationListener

{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report the number of iterations in a period.
     *
     * @return number of iterations between reporting
     */
    public int getIterationPeriod ();

    /**
     * Call-back at end of iteration period.
     *
     * @param epochsCount          total count of epochs so far
     * @param iteration            iteration number
     * @param score                current loss value
     * @param hiddenSquaredWeights sum of squared weights for hidden layer
     * @param outputSquaredWeights sum of squared weights for output layer
     */
    public void iterationPeriodDone (int epochsCount,
                                     int iteration,
                                     double score,
                                     double hiddenSquaredWeights,
                                     double outputSquaredWeights);
}
