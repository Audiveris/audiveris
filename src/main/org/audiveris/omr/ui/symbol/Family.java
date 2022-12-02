//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           F a m i l y                                          //
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enum <code>Family</code> handles the supported music font families.
 *
 * @author Hervé Bitteur
 */
public enum Family
{
    /** Main family. */
    Bravura("Bravura", "Bravura.otf", null, new BravuraSymbols()),
    /** Alternate family, some symbols missing. */
    FinaleJazz("Finale Jazz", "FinaleJazz.otf", Bravura, new FinaleJazzSymbols()),
    /** Alternate family, for percussion symbols. */
    JazzPerc("Jazz Perc", "JazzPerc.ttf", FinaleJazz, new JazzPercSymbols()),
    /** Alternate family, with many missing symbols. */
    MusicalSymbols("MusicalSymbols", "MusicalSymbols.ttf", Bravura, new MusicalSymbols());

    private static final Logger logger = LoggerFactory.getLogger(Family.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** A descriptive name for the font. */
    final String fontName;

    /** Precise name of font file, if any. */
    final String fileName;

    /** Another family, if any, used as backup. */
    final Family backup;

    /** Specific symbols handling for this font. */
    final Symbols symbols;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a <code> Family</code> instance.
     *
     * @param fontName (mandatory) Name for the font
     * @param fileName (optional) Related font file name in 'res' folder.
     *                 If null, a platform font will be searched using fontName.
     * @param backup   (optional) Backup family if any
     * @param symbols  (mandatory) Implementation of related symbols
     */
    Family (String fontName,
            String fileName,
            Family backup,
            Symbols symbols)
    {
        this.fontName = fontName;
        this.fileName = fileName;
        this.backup = backup;
        this.symbols = symbols;
    }

    //~ Methods ------------------------------------------------------------------------------------
    public String getFontName ()
    {
        return fontName;
    }

    public String getFileName ()
    {
        return fileName;
    }

    public Family getBackup ()
    {
        return backup;
    }

    public Symbols getSymbols ()
    {
        return symbols;
    }

    public static Family valueOfName (String fontName)
    {
        for (Family family : Family.values()) {
            if (family.name().equalsIgnoreCase(fontName)
                        || family.fontName.equalsIgnoreCase(fontName)) {
                return family;
            }
        }

        logger.warn("No family for font name {}", fontName);
        return null;
    }
}
