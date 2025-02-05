//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      M u s i c F a m i l y                                     //
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
 * Enum <code>MusicFamily</code> handles the hierarchy of supported music font families.
 *
 * @author Hervé Bitteur
 */
public enum MusicFamily
{
    /** Main family. */
    Bravura(
            "Bravura",
            "Bravura.otf",
            null, // No backup needed for this comprehensive font
            new BravuraSymbols()),

    /** Alternate family, some symbols missing. */
    Leland("Leland", "Leland.otf", Bravura, new LelandSymbols()),

    /** Alternate family, some symbols missing. */
    FinaleJazz("Finale Jazz", "FinaleJazz.otf", Bravura, new FinaleJazzSymbols()),

    /** Alternate family, for percussion symbols. */
    JazzPerc("Jazz Perc", "JazzPerc.ttf", FinaleJazz, new JazzPercSymbols()),

    /** Alternate family, with many missing symbols. Kept for sample generation. */
    MusicalSymbols("MusicalSymbols", "MusicalSymbols.ttf", Bravura, new MusicalSymbols());

    private static final Logger logger = LoggerFactory.getLogger(MusicFamily.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** A descriptive name for the font. */
    final String fontName;

    /** Precise name of font file, if any. */
    final String fileName;

    /** Another family, if any, used as backup. */
    final MusicFamily backup;

    /** Specific symbols handling for this font. */
    final Symbols symbols;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a <code>MusicFamily</code> instance.
     *
     * @param fontName (mandatory) Name for the font
     * @param fileName (optional) Related font file name in 'res' folder.
     *                 If null, a platform font will be searched using fontName.
     * @param backup   (optional) Backup family if any
     * @param symbols  (mandatory) Implementation of related symbols
     */
    MusicFamily (String fontName,
                 String fileName,
                 MusicFamily backup,
                 Symbols symbols)
    {
        this.fontName = fontName;
        this.fileName = fileName;
        this.backup = backup;
        this.symbols = symbols;
    }

    //~ Methods ------------------------------------------------------------------------------------

    public MusicFamily getBackup ()
    {
        return backup;
    }

    public String getFileName ()
    {
        return fileName;
    }

    public String getFontName ()
    {
        return fontName;
    }

    public Symbols getSymbols ()
    {
        return symbols;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    public static MusicFamily valueOfName (String value)
    {
        for (MusicFamily family : MusicFamily.values()) {
            if (family.name().equalsIgnoreCase(value) || family.fontName.equalsIgnoreCase(value)) {
                return family;
            }
        }

        if (value.equalsIgnoreCase("generic")) {
            return MusicFamily.Bravura;
        }

        logger.warn("No music family for value: \"{}\"", value);
        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //---------//
    // MyParam //
    //---------//
    /**
     * Class <code>MyParam</code> is a param on MusicFamily.
     */
    public static class MyParam
            extends Param<MusicFamily>
    {
        public MyParam (Object scope)
        {
            super(scope);
        }

        public static class JaxbAdapter
                extends XmlAdapter<MusicFamily, MyParam>
        {
            @Override
            public MusicFamily marshal (MyParam fp)
                throws Exception
            {
                if (fp == null) {
                    return null;
                }

                return fp.getSpecific();
            }

            @Override
            public MyParam unmarshal (MusicFamily value)
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
