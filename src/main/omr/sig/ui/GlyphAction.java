//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      G l y p h A c t i o n                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.ui;

import omr.glyph.Glyph;
import omr.glyph.GlyphIndex;

import omr.selection.EntityListEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.util.Arrays;

import javax.swing.AbstractAction;

/**
 * Action related to UI glyph selection.
 *
 * @author Hervé Bitteur
 */
public class GlyphAction
        extends AbstractAction
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GlyphAction.class);

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
        GlyphIndex glyphIndex = glyph.getIndex();

        if (glyphIndex == null) {
            logger.warn("No index for {}", glyph);
        } else {
            glyphIndex.getEntityService().publish(
                    new EntityListEvent<Glyph>(
                            this,
                            SelectionHint.ENTITY_INIT,
                            MouseMovement.PRESSING,
                            Arrays.asList(glyph)));
        }
    }

    //-------//
    // tipOf //
    //-------//
    private String tipOf (Glyph glyph)
    {
        String tip = "groups: " + glyph.getGroups();

        //        Shape shape = glyph.getShape();
        //
        //        if (shape != null) {
        //            tip += (", shape: " + shape);
        //        }
        //
        return tip;
    }
}
