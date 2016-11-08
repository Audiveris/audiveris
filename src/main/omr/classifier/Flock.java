//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            F l o c k                                           //
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
package omr.classifier;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Flock} defines a group of glyphs that compete for a symbol.
 * <p>
 * This is meant for testing classifier in its ability to separate the best candidate from other
 * sub-optimal candidates.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(value = XmlAccessType.NONE)
@XmlRootElement(name = "flock")
public class Flock
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The sample that best defines the flock underlying symbol. */
    @XmlElement
    private final Sample best;

    /** Samples that are considered as compatible with best. */
    @XmlElement(name = "good")
    private final List<Sample> goods = new ArrayList<Sample>();

    /** Samples that must be classified with lower grade than best sample. */
    @XmlElement(name = "other")
    private final List<Sample> others = new ArrayList<Sample>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code Flock} object.
     *
     * @param best the best sample for flock underlying symbol
     */
    public Flock (Sample best)
    {
        this.best = best;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private Flock ()
    {
        this.best = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    public void addGood (Sample good)
    {
        if (!goods.contains(good)) {
            goods.add(good);
        }
    }

    public void addOther (Sample other)
    {
        if (!others.contains(other)) {
            others.add(other);
        }
    }

    /**
     * @return the best
     */
    public Sample getBest ()
    {
        return best;
    }

    /**
     * @return the goods
     */
    public List<Sample> getGoods ()
    {
        return goods;
    }

    /**
     * @return the others
     */
    public List<Sample> getOthers ()
    {
        return others;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("Flock{");
        sb.append("best:").append(best);

        for (Sample good : goods) {
            sb.append(" good:").append(good);
        }

        for (Sample other : others) {
            sb.append(" other:").append(other);
        }

        sb.append("}");

        return sb.toString();
    }
}
