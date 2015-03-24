//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      G l y p h A c t i o n                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.ui;

import omr.glyph.GlyphNest;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.selection.GlyphEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

class GlyphAction
        extends AbstractAction
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The underlying glyph. */
    private final Glyph glyph;

    //~ Constructors -------------------------------------------------------------------------------
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

    //~ Methods ------------------------------------------------------------------------------------
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
        GlyphNest nest = glyph.getNest();
        nest.getGlyphService().publish(
                new GlyphEvent(this, SelectionHint.GLYPH_INIT, MouseMovement.PRESSING, glyph));
    }

    //-------//
    // tipOf //
    //-------//
    private String tipOf (Glyph glyph)
    {
        String tip = "layer: " + glyph.getLayer();
        Shape shape = glyph.getShape();

        if (shape != null) {
            tip += (", shape: " + shape);
        }

        return tip;
    }
}
