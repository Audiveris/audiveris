//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                G l y p h s R e m o v a l T a s k                               //
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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Class <code>GlyphsRemovalTask</code> removes a collection of glyphs.
 *
 * @author Hervé Bitteur
 */
public class GlyphsRemovalTask
        extends UITask
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GlyphsRemovalTask.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The glyphs to remove. */
    final Set<Glyph> glyphs = new LinkedHashSet<>();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>GlyphsRemovalTask</code> object, with the glyphs to remove.
     *
     * @param system the referent system
     * @param glyphs the glyphs to remove
     */
    public GlyphsRemovalTask (SystemInfo system,
                              Collection<Glyph> glyphs)
    {
        super(system.getSig(), "delGlyphs");
        this.glyphs.addAll(glyphs);
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public void performDo ()
    {
        final GlyphIndex glyphIndex = sheet.getGlyphIndex();
        glyphs.forEach(glyph -> glyphIndex.remove(glyph));
    }

    @Override
    public void performUndo ()
    {
        sheet.getGlyphIndex().setEntities(glyphs);
    }
}
