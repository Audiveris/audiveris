//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  A b s t r a c t I m p a c t s                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sig;

import omr.sig.inter.Inter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class {@code AbstractImpacts} is an abstract implementation of {@link GradeImpacts}
 * interface.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractImpacts
        implements GradeImpacts
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code AbstractImpacts} object.
     */
    public AbstractImpacts ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------------//
    // getIntrinsicRatio //
    //-------------------//
    @Override
    public double getIntrinsicRatio ()
    {
        return Inter.intrinsicRatio;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < getImpactCount(); i++) {
            if (sb.length() > 0) {
                sb.append(" ");
            }

            sb.append(String.format("%s:%.2f", getName(i), getImpact(i)));
        }

        return sb.toString();
    }

    //--------------//
    // computeGrade //
    //--------------//
    protected double computeGrade ()
    {
        double global = 1d;
        double totalWeight = 0d;

        for (int i = 0; i < getImpactCount(); i++) {
            double weight = getWeight(i);
            double impact = getImpact(i);
            totalWeight += weight;

            if (impact == 0) {
                global = 0;
            } else if (weight != 0) {
                global *= Math.pow(impact, weight);
            }
        }

        return getIntrinsicRatio() * Math.pow(global, 1 / totalWeight);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    public static class Adapter
            extends XmlAdapter<AbstractImpacts, GradeImpacts>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public AbstractImpacts marshal (GradeImpacts itf)
                throws Exception
        {
            return (AbstractImpacts) itf;
        }

        @Override
        public GradeImpacts unmarshal (AbstractImpacts abs)
                throws Exception
        {
            return abs;
        }
    }
}
