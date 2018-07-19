//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    O m r E v a l u a t i o n                                   //
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

import org.audiveris.omrdataset.api.OmrShape;

/**
 * Class {@code OmrEvaluation} is an evaluation using OmrShape (rather than Shape).
 *
 * @author Hervé Bitteur
 */
public class OmrEvaluation
        implements Comparable<OmrEvaluation>
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The evaluated OMR shape. */
    public final OmrShape omrShape;

    /**
     * The evaluation grade (larger is better), generally provided by the classifier
     * in the range 0 - 1.
     */
    public final double grade;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code OmrEvaluation} object.
     *
     * @param omrShape the OmrShape this evaluation measures
     * @param grade    the measurement result (larger is better)
     */
    public OmrEvaluation (OmrShape omrShape,
                          double grade)
    {
        this.omrShape = omrShape;
        this.grade = grade;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // compareTo //
    //-----------//
    /**
     * To sort from best to worst.
     *
     * @param that the other evaluation instance
     * @return -1, 0 or +1
     */
    @Override
    public int compareTo (OmrEvaluation that)
    {
        return Double.compare(that.grade, this.grade); // Reverse order: highest to lowest
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(omrShape);
        sb.append("(");
        sb.append(String.format("%.5f", grade));
        sb.append(")");

        return sb.toString();
    }
}
