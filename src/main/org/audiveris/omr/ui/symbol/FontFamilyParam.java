//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  F o n t F a m i l y P a r a m                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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
package org.audiveris.omr.ui.symbol;

import org.audiveris.omr.util.param.Param;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class <code>FontFamilyParam</code> is a param on MusicFont.Family.
 *
 * @author Hervé Bitteur
 */
public class FontFamilyParam
        extends Param<Family>
{
    //~ Constructors -------------------------------------------------------------------------------

    public FontFamilyParam (Object scope)
    {
        super(scope);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    public static class JaxbAdapter
            extends XmlAdapter<Family, FontFamilyParam>
    {

        @Override
        public Family marshal (FontFamilyParam fp)
                throws Exception
        {
            if (fp == null) {
                return null;
            }

            return fp.getSpecific();
        }

        @Override
        public FontFamilyParam unmarshal (Family value)
                throws Exception
        {
            if (value == null) {
                return null;
            }

            final FontFamilyParam fp = new FontFamilyParam(null);
            fp.setSpecific(value);

            return fp;
        }
    }
}
