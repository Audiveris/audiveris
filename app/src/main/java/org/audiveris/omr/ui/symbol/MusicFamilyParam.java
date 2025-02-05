//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 M u s i c F a m i l y P a r a m                                //
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
package org.audiveris.omr.ui.symbol;

import org.audiveris.omr.util.param.Param;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class <code>MusicFamilyParam</code> is a param on MusicFamily.
 *
 * @author Hervé Bitteur
 */
public class MusicFamilyParam
        extends Param<MusicFamily>
{
    //~ Constructors -------------------------------------------------------------------------------

    public MusicFamilyParam (Object scope)
    {
        super(scope);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    public static class JaxbAdapter
            extends XmlAdapter<MusicFamily, MusicFamilyParam>
    {
        @Override
        public MusicFamily marshal (MusicFamilyParam fp)
            throws Exception
        {
            if (fp == null) {
                return null;
            }

            return fp.getSpecific();
        }

        @Override
        public MusicFamilyParam unmarshal (MusicFamily value)
            throws Exception
        {
            if (value == null) {
                return null;
            }

            final MusicFamilyParam fp = new MusicFamilyParam(null);
            fp.setSpecific(value);

            return fp;
        }
    }
}
