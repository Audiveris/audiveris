//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   C r o s s E x c l u s i o n                                  //
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
package omr.sig.relation;

import omr.sheet.Sheet;

import omr.sig.inter.AbstractInter;
import omr.sig.inter.Inter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code CrossExclusion} represents a mutual exclusion between two inters located
 * in different systems (and thus different SIG's).
 * <p>
 * Such CrossExclusion may occur in the gutter between system above and system below.
 * <p>
 * It encapsulates its pair of Inter instances involved, because this edge cannot be handled by
 * a sig.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "cross-exclusion")
public class CrossExclusion
        extends BasicExclusion
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Source inter. */
    @XmlIDREF
    @XmlAttribute
    private final AbstractInter source;

    /** Target inter. */
    @XmlIDREF
    @XmlAttribute
    private final AbstractInter target;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code CrossExclusion} object (always with OVERLAP cause).
     *
     * @param source source inter (in a sig)
     * @param target target inter (in another sig)
     */
    public CrossExclusion (Inter source,
                           Inter target)
    {
        super(Cause.OVERLAP);
        this.source = (AbstractInter) source;
        this.target = (AbstractInter) target;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private CrossExclusion ()
    {
        super(null);
        this.source = null;
        this.target = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // insert //
    //--------//
    /**
     * Insert a cross-system exclusion between two Inter instances (assumed to belong to
     * different systems).
     *
     * @param inter1 an inter
     * @param inter2 another inter (in another system)
     */
    public static void insert (Inter inter1,
                               Inter inter2)
    {
        final Sheet sheet = inter1.getSig().getSystem().getSheet();
        final Map<Inter, List<CrossExclusion>> map = sheet.getCrossExclusions();
        final boolean direct = inter1.getId() < inter2.getId();
        final Inter source = direct ? inter1 : inter2;
        final Inter target = direct ? inter2 : inter1;
        final CrossExclusion exc = new CrossExclusion(source, target);

        for (Inter inter : new Inter[]{source, target}) {
            List<CrossExclusion> list = map.get(inter);

            if (list == null) {
                map.put(inter, list = new ArrayList<CrossExclusion>());
            }

            if (!list.contains(exc)) {
                list.add(exc);
            }
        }
    }

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof CrossExclusion)) {
            return false;
        }

        CrossExclusion that = (CrossExclusion) obj;

        return (this.source == that.source) && (this.target == that.target);
    }

    //-----------//
    // getSource //
    //-----------//
    /**
     * @return the source
     */
    public Inter getSource ()
    {
        return source;
    }

    //-----------//
    // getTarget //
    //-----------//
    /**
     * @return the target
     */
    public Inter getTarget ()
    {
        return target;
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 5;
        hash = (89 * hash) + Objects.hashCode(this.source);
        hash = (89 * hash) + Objects.hashCode(this.target);

        return hash;
    }
}
