//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      G l y p h A c t i o n                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * Action related to UI glyph selection.
 *
 * @author Hervé Bitteur
 */
public class GlyphAction
        extends AbstractAction
{

    private static final Logger logger = LoggerFactory.getLogger(GlyphAction.class);

    /** The underlying glyph. */
    private final Glyph glyph;

    /**
     * Creates a new GlyphAction object.
     *
     * @param glyph the underlying glyph
     */
    public GlyphAction (Glyph glyph)
    {
        this(glyph, null);
    }

    /**
     * Creates a new GlyphAction object.
     *
     * @param glyph the underlying glyph
     * @param text  specific item text, if any
     */
    public GlyphAction (Glyph glyph,
                        String text)
    {
        this.glyph = glyph;
        putValue(NAME, (text != null) ? text : ("" + glyph.getId()));
        putValue(SHORT_DESCRIPTION, tipOf(glyph));
    }

    //-----------------//
    // actionPerformed //
    //-----------------//
    @Override
    public void actionPerformed (ActionEvent e)
    {
        publish();
    }

    //---------//
    // publish //
    //---------//
    public void publish ()
    {
        GlyphIndex glyphIndex = glyph.getIndex();

        if (glyphIndex == null) {
            logger.warn("No index for {}", glyph);
        } else {
            glyphIndex.getEntityService().publish(
                    new EntityListEvent<>(
                            this,
                            SelectionHint.ENTITY_INIT,
                            MouseMovement.PRESSING,
                            glyph));
        }
    }

    //-------//
    // tipOf //
    //-------//
    private String tipOf (Glyph glyph)
    {
        String tip = "groups: " + glyph.getGroups();

        return tip;
    }
}
