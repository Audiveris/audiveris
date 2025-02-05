//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               G l y p h s A d d i t i o n T a s k                              //
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Class <code>GlyphsAdditionTask</code> adds a collection of glyphs.
 *
 * @author Hervé Bitteur
 */
public class GlyphsAdditionTask
        extends UITask
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GlyphsAdditionTask.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The glyphs to add. */
    protected final Set<Glyph> glyphs = new LinkedHashSet<>();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>GlyphsAdditionTask</code> object, with the glyphs to add.
     *
     * @param system the referent system
     * @param glyphs the glyphs to add
     */
    public GlyphsAdditionTask (SystemInfo system,
                               Collection<Glyph> glyphs)
    {
        super(system.getSig(), "addGlyphs");
        this.glyphs.addAll(glyphs);
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public void performDo ()
    {
        final SystemInfo system = sig.getSystem();
        final GlyphIndex glyphIndex = sheet.getGlyphIndex();

        glyphs.forEach(glyph -> {
            if (glyph.getId() == 0) {
                system.addFreeGlyph(glyphIndex.registerOriginal(glyph));
            } else {
                glyphIndex.setEntities(Arrays.asList(glyph));
            }
        });
    }

    @Override
    public void performUndo ()
    {
        final GlyphIndex glyphIndex = sheet.getGlyphIndex();
        glyphs.forEach(glyph -> glyphIndex.remove(glyph));
    }
}
