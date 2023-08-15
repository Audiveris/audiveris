//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            T r i b e                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>Tribe</code> defines a group of glyphs that compete for a symbol.
 * <p>
 * This is meant for testing classifier in its ability to separate the best candidate from other
 * sub-optimal candidates.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(value = XmlAccessType.NONE)
@XmlRootElement(name = "tribe")
public class Tribe
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The sample that best defines the tribe underlying symbol. */
    @XmlElement
    private final Sample head;

    /** Samples that are considered as compatible with best. */
    @XmlElement(name = "good")
    private final List<Sample> goods = new ArrayList<>();

    /** Samples that must be classified with lower grade than best sample. */
    @XmlElement(name = "member")
    private final List<Sample> members = new ArrayList<>();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-arg constructor meant for JAXB.
     */
    private Tribe ()
    {
        this.head = null;
    }

    /**
     * Creates a new <code>Tribe</code> object.
     *
     * @param best the best sample for tribe underlying symbol
     */
    public Tribe (Sample best)
    {
        this.head = best;
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Add a good sample.
     *
     * @param good the good sample to add
     */
    public void addGood (Sample good)
    {
        if (!goods.contains(good)) {
            goods.add(good);
        }
    }

    /**
     * Add a basic sample.
     *
     * @param other a plain sample to add
     */
    public void addOther (Sample other)
    {
        if (!members.contains(other)) {
            members.add(other);
        }
    }

    /**
     * Report all the good samples.
     *
     * @return the goods
     */
    public List<Sample> getGoods ()
    {
        return goods;
    }

    /**
     * Report the best sample in tribe.
     *
     * @return the best
     */
    public Sample getHead ()
    {
        return head;
    }

    /**
     * Return the other samples in tribe (apart the best).
     *
     * @return the others
     */
    public List<Sample> getMembers ()
    {
        return members;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("Tribe{");
        sb.append("head:").append(head);

        for (Sample good : goods) {
            sb.append(" good:").append(good);
        }

        for (Sample other : members) {
            sb.append(" member:").append(other);
        }

        sb.append("}");

        return sb.toString();
    }
}
