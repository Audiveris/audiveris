//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       T e x t F a m i l y                                      //
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class <code>TextFamily</code> handles the collection of supported text font families.
 *
 * @author Hervé Bitteur
 */
public enum TextFamily
{
    /** Standard text family, without serif. */
    SansSerif("Sans Serif", null),

    /** Standard text family, with serif. */
    Serif("Serif", null),

    /** Jazz text family. */
    FinaleJazzText("Finale Jazz Text", "FinaleJazzText.otf");

    private static final Logger logger = LoggerFactory.getLogger(TextFamily.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** A descriptive name for the font. */
    final String fontName;

    /** Precise name of font file, if any. */
    final String fileName;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a <code>TextFamily</code> instance.
     *
     * @param fontName (mandatory) Name for the font
     * @param fileName (optional) Related font file name in 'res' folder.
     *                 If null, a platform font will be searched using fontName.
     */
    TextFamily (String fontName,
                String fileName)
    {
        this.fontName = fontName;
        this.fileName = fileName;
    }

    //~ Methods ------------------------------------------------------------------------------------

    public String getFileName ()
    {
        return fileName;
    }

    public String getFontName ()
    {
        return fontName;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    public static TextFamily valueOfName (String value)
    {
        for (TextFamily family : TextFamily.values()) {
            if (family.name().equalsIgnoreCase(value) || family.fontName.equalsIgnoreCase(value)) {
                return family;
            }
        }

        logger.warn("No text family for value: \"{}\"", value);
        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //---------//
    // MyParam //
    //---------//
    /**
     * Class <code>MyParam</code> is a param on TextFamily.
     */
    public static class MyParam
            extends Param<TextFamily>
    {
        public MyParam (Object scope)
        {
            super(scope);
        }

        public static class JaxbAdapter
                extends XmlAdapter<TextFamily, MyParam>
        {
            @Override
            public TextFamily marshal (MyParam fp)
                throws Exception
            {
                if (fp == null) {
                    return null;
                }

                return fp.getSpecific();
            }

            @Override
            public MyParam unmarshal (TextFamily value)
                throws Exception
            {
                if (value == null) {
                    return null;
                }

                final MyParam fp = new MyParam(null);
                fp.setSpecific(value);

                return fp;
            }
        }
    }
}
