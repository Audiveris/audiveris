//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B a s i c I m p a c t s                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.sig;

/**
 * Class {@code BasicImpacts} is an array-based implementation of {@link GradeImpacts}
 * interface.
 *
 * @author Hervé Bitteur
 */
public abstract class BasicImpacts
        extends AbstractImpacts
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final String[] names;

    private final double[] weights;

    protected final double[] impacts;

    protected double grade = -1;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BasicImpacts object.
     *
     * @param names   array of names
     * @param weights array of weights
     */
    public BasicImpacts (String[] names,
                         double[] weights)
    {
        if (names.length != weights.length) {
            throw new IllegalArgumentException("Arrays for names & weights have different lengths");
        }

        this.names = names;
        this.weights = weights;

        impacts = new double[names.length];
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String getDump ()
    {
        final StringBuilder sb = new StringBuilder();

        return sb.toString();
    }

    @Override
    public double getGrade ()
    {
        if (grade == -1) {
            grade = computeGrade();
        }

        return grade;
    }

    @Override
    public double getImpact (int index)
    {
        return impacts[index];
    }

    @Override
    public int getImpactCount ()
    {
        return impacts.length;
    }

    @Override
    public String getName (int index)
    {
        return names[index];
    }

    @Override
    public double getWeight (int index)
    {
        return weights[index];
    }

    public void setImpact (int index,
                           double impact)
    {
        impacts[index] = GradeUtil.clamp(impact);
    }
}
