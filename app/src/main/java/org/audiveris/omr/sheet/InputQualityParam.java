//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                I n p u t Q u a l i t y P a r a m                               //
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.sheet.Profiles.InputQuality;
import org.audiveris.omr.util.param.Param;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class <code>InputQualityParam</code> is a param on Input Quality
 *
 * @author Hervé Bitteur
 */
public class InputQualityParam
        extends Param<InputQuality>
{
    //~ Constructors -------------------------------------------------------------------------------

    public InputQualityParam (Object scope)
    {
        super(scope);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    public static class JaxbAdapter
            extends XmlAdapter<InputQuality, InputQualityParam>
    {
        @Override
        public InputQuality marshal (InputQualityParam iqp)
            throws Exception
        {
            if (iqp == null) {
                return null;
            }

            return iqp.getSpecific();
        }

        @Override
        public InputQualityParam unmarshal (InputQuality value)
            throws Exception
        {
            if (value == null) {
                return null;
            }

            final InputQualityParam iqp = new InputQualityParam(null);
            iqp.setSpecific(value);

            return iqp;
        }
    }
}
