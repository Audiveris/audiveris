//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       B a s i c N o r m s                                      //
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

import org.audiveris.omr.math.PoorManAlgebra.INDArray;

/**
 * Class <code>BasicNorms</code> encapsulates the means and standard deviations of glyph features.
 *
 * @author Hervé Bitteur
 */
public class BasicNorms
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Features means. */
    public final INDArray means;

    /** Features standard deviations. */
    public final INDArray stds;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>Norms</code> object.
     *
     * @param means
     * @param stds
     */
    public BasicNorms (INDArray means,
                       INDArray stds)
    {
        this.means = means;
        this.stds = stds;
    }
}
