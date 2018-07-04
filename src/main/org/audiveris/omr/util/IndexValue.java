//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       I n d e x V a l u e                                      //
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
package org.audiveris.omr.util;

import org.audiveris.omr.classifier.Annotation;
import org.audiveris.omr.glyph.BasicGlyph;
import org.audiveris.omr.ui.symbol.BasicSymbol;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.ArrayList;

/**
 * Class {@code IndexValue}
 *
 * @author Hervé Bitteur
 *
 * @param <E> specific type
 */
@XmlRootElement(name = "index")
public class IndexValue<E extends Entity>
{
    //~ Instance fields ----------------------------------------------------------------------------

    @XmlElementRefs({
        @XmlElementRef(type = BasicGlyph.class)
        , @XmlElementRef(type = BasicSymbol.class)
        , @XmlElementRef(type = Annotation.class)
    })
    public final ArrayList<E> entities = new ArrayList<E>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code IndexValue} object.
     */
    public IndexValue ()
    {
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    /**
     * Meant for JAXB handling of SIG.
     */
    public static class Adapter
            extends XmlAdapter<IndexValue, BasicIndex>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public IndexValue marshal (BasicIndex index)
                throws Exception
        {
            IndexValue value = new IndexValue();

            value.entities.addAll(index.getEntities());

            return value;
        }

        @Override
        public BasicIndex unmarshal (IndexValue indexValue)
                throws Exception
        {
            return null; ///new BasicIndex(indexValue);
        }
    }
}
