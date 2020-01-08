//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                S y m b o l G l y p h B o a r d                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.glyph.ui;

import com.jgoodies.forms.layout.CellConstraints;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.ui.field.LDoubleField;
import org.audiveris.omr.ui.selection.EntityListEvent;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code SymbolGlyphBoard} defines an extended glyph board, with normalized
 * values for weight, width and height.
 * <p>
 * <img alt="Image of SymbolGlyphBoard" src="doc-files/SymbolGlyphBoard.png">
 *
 * @author Hervé Bitteur
 */
public class SymbolGlyphBoard
        extends GlyphBoard
{

    private static final Logger logger = LoggerFactory.getLogger(SymbolGlyphBoard.class);

    private static final ResourceMap resources = Application.getInstance().getContext()
            .getResourceMap(SymbolGlyphBoard.class);

    /** Glyph characteristics : normalized weight. */
    private final LDoubleField weight = new LDoubleField(
            false,
            resources.getString("weight.text"),
            resources.getString("weight.toolTipText"),
            "%.3f");

    /** Glyph characteristics : normalized width. */
    private final LDoubleField width = new LDoubleField(
            false,
            resources.getString("width.text"),
            resources.getString("width.toolTipText"),
            "%.3f");

    /** Glyph characteristics : normalized height. */
    private final LDoubleField height = new LDoubleField(
            false,
            resources.getString("height.text"),
            resources.getString("height.toolTipText"),
            "%.3f");

    /**
     * Create the symbol glyph board.
     *
     * @param glyphsController the companion which handles glyph (de)assignments
     * @param selected         true to pre-select this board
     */
    public SymbolGlyphBoard (GlyphsController glyphsController,
                             boolean selected)
    {
        // For all glyphs
        super(glyphsController, selected);

        width.getField().setBorder(null);
        height.getField().setBorder(null);
        weight.getField().setBorder(null);

        width.setEnabled(false);
        height.setEnabled(false);
        weight.setEnabled(false);

        defineLayout();
    }

    //-----------------------//
    // handleEntityListEvent //
    //-----------------------//
    /**
     * Interest in EntityList for Weight, Width, Height fields
     *
     * @param listEvent EntityListEvent
     */
    @Override
    protected void handleEntityListEvent (EntityListEvent<Glyph> listEvent)
    {
        super.handleEntityListEvent(listEvent);

        final Glyph glyph = listEvent.getEntity();

        // Fill symbol characteristics
        if (glyph != null) {
            int interline = controller.getModel().getSheet().getScale().getInterline();
            weight.setValue(glyph.getNormalizedWeight(interline));
            width.setValue((double) glyph.getWidth() / interline);
            height.setValue((double) glyph.getHeight() / interline);
        } else {
            weight.setText("");
            width.setText("");
            height.setText("");
        }

        width.setEnabled(glyph != null);
        height.setEnabled(glyph != null);
        weight.setEnabled(glyph != null);
    }

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define a specific layout for this Symbol GlyphBoard.
     */
    private void defineLayout ()
    {
        final CellConstraints cst = new CellConstraints();

        int r = 1; // --------------------------------
        // id + width

        builder.add(width.getLabel(), cst.xy(9, r));
        builder.add(width.getField(), cst.xy(11, r));

        r += 2; // --------------------------------
        // weight + height

        builder.add(weight.getLabel(), cst.xy(5, r));
        builder.add(weight.getField(), cst.xy(7, r));

        builder.add(height.getLabel(), cst.xy(9, r));
        builder.add(height.getField(), cst.xy(11, r));
    }
}
