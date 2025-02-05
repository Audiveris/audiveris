//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S i m i l e M a r k I n t e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.audiveris.omr.sig.SIGraph;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>SimileMarkInter</code> is kept only to convert old .omr files that used it.
 * <p>
 * It is replaced by {@link MeasureRepeatInter}.
 *
 * @author Hervé Bitteur
 */
@Deprecated
@SuppressWarnings("deprecation")
@XmlRootElement(name = "simile")
public class SimileMarkInter
        extends AbstractInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    private SimileMarkInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // replace //
    //---------//
    /**
     * Replace deprecated SimileMarkInter instance by MeasureRepeatInter instance.
     *
     * @param simile the SimileMarkInter instance to replace and remove
     * @return the replacement as MeasureRepeatInter instance
     */
    public static MeasureRepeatInter replace (SimileMarkInter simile)
    {
        final SIGraph sig = simile.getSig();
        final MeasureRepeatInter repeat = new MeasureRepeatInter(
                simile.getGlyph(),
                simile.getShape(),
                simile.getGrade());
        repeat.setStaff(simile.getStaff());
        sig.addVertex(repeat);

        simile.remove();

        return repeat;
    }
}
