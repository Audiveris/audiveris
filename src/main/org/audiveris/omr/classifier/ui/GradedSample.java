//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     G r a d e d S a m p l e                                    //
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
package org.audiveris.omr.classifier.ui;

import org.audiveris.omr.classifier.Sample;

import java.util.Comparator;

/**
 * Class <code>GradedSample</code> is a sample coupled with its recognition grade.
 *
 * @author Hervé Bitteur
 */
public class GradedSample
{
    //~ Static fields/initializers -----------------------------------------------------------------

    public static final Comparator<GradedSample> byReverseGrade
            = (o1, o2) -> Double.compare(o2.grade, o1.grade);

    //~ Instance fields ----------------------------------------------------------------------------
    final Sample sample;

    final double grade;

    //~ Constructors -------------------------------------------------------------------------------
    GradedSample (Sample sample,
                  double grade)
    {
        this.sample = sample;
        this.grade = grade;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String toString ()
    {
        return "GradedSample{" + sample + " " + grade + "}";
    }
}
