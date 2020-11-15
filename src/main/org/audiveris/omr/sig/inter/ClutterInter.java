//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C l u t t e r I n t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code ClutterInter} is an inter meant to ease the creation of CLUTTER samples.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "clutter")
public class ClutterInter
        extends AbstractInter
{
    //~ Constructors -------------------------------------------------------------------------------

    public ClutterInter (Glyph glyph,
                         double grade)
    {
        super(glyph, (glyph != null) ? glyph.getBounds() : null, Shape.CLUTTER, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private ClutterInter ()
    {
    }
}
