//----------------------------------------------------------------------------//
//                                                                            //
//                     S p i n n e r G l y p h M o d e l                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.Scene;
import omr.glyph.facets.Glyph;

import omr.log.Logger;
import static omr.ui.field.SpinnerUtilities.*;

import omr.util.Implement;
import omr.util.Predicate;

import javax.swing.AbstractSpinnerModel;
import javax.swing.SpinnerModel;

/**
 * Class <code>SpinnerGlyphModel</code> is a spinner model backed by a {@link
 * Scene}. Any modification in the scene is thus transparently
 * handled, since the scene <b>is</b> the model. <p>A glyph {@link Predicate} can
 * be assigned to this SpinnerGlyphModel at construction time in order to
 * restrict the population of glyphs in the spinner. This class is used by
 * {@link GlyphBoard} only, but is not coupled with it.
 *
 * @author Hervé Bitteur
 */
public class SpinnerGlyphModel
    extends AbstractSpinnerModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        SpinnerGlyphModel.class);

    //~ Instance fields --------------------------------------------------------

    /** Underlying glyph scene */
    private final Scene scene;

    /** Additional predicate if any */
    private final Predicate<Glyph> predicate;

    /** Current glyph id */
    private Integer currentId;

    //~ Constructors -----------------------------------------------------------

    //-------------------//
    // SpinnerGlyphModel //
    //-------------------//
    /**
     * Creates a new SpinnerGlyphModel object, on all scene glyphs
     * @param scene the underlying glyph scene
     */
    public SpinnerGlyphModel (Scene scene)
    {
        this(scene, null);
    }

    //-------------------//
    // SpinnerGlyphModel //
    //-------------------//
    /**
     * Creates a new SpinnerGlyphModel object, with a related glyph predicate
     * @param scene the underlying glyph scene
     * @param predicate predicate of glyph, or null
     */
    public SpinnerGlyphModel (Scene            scene,
                              Predicate<Glyph> predicate)
    {
        if (scene == null) {
            throw new IllegalArgumentException(
                "SpinnerGlyphModel expects non-null glyph scene");
        }

        this.scene = scene;
        this.predicate = predicate;

        currentId = NO_VALUE;
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getNextValue //
    //--------------//
    /**
     * Return the next legal glyph id in the sequence that comes after the glyph
     * id returned by <code>getValue()</code>. If the end of the sequence has
     * been reached then return null.
     *
     * @return the next legal glyph id or null if one doesn't exist
     */
    @Implement(SpinnerModel.class)
    public Object getNextValue ()
    {
        final int cur = currentId.intValue();

        if (logger.isFineEnabled()) {
            logger.fine("getNextValue cur=" + cur);
        }

        if (cur == NO_VALUE) {
            // Return first suitable glyph in scene
            for (Glyph glyph : scene.getAllGlyphs()) {
                if ((predicate == null) || predicate.check(glyph)) {
                    return glyph.getId();
                }
            }

            return null;
        } else {
            // Return first suitable glyph after current glyph in scene
            boolean found = false;

            for (Glyph glyph : scene.getAllGlyphs()) {
                if (!found) {
                    if (glyph.getId() == cur) {
                        found = true;
                    }
                } else if ((predicate == null) || predicate.check(glyph)) {
                    return glyph.getId();
                }
            }

            return null;
        }
    }

    //------------------//
    // getPreviousValue //
    //------------------//
    /**
     * Return the legal glyph id in the sequence that comes before the glyph id
     * returned by <code>getValue()</code>.  If the end of the sequence has been
     * reached then return null.
     *
     * @return the previous legal value or null if one doesn't exist
     */
    @Implement(SpinnerModel.class)
    public Object getPreviousValue ()
    {
        Glyph     prevGlyph = null;
        final int cur = currentId.intValue();

        if (logger.isFineEnabled()) {
            logger.fine("getPreviousValue cur=" + cur);
        }

        if (cur == NO_VALUE) {
            return NO_VALUE;
        }

        // Scene
        for (Glyph glyph : scene.getAllGlyphs()) {
            if (glyph.getId() == cur) {
                return (prevGlyph != null) ? prevGlyph.getId() : NO_VALUE;
            }

            // Should we remember this as (suitable) previous glyph ?
            if ((predicate == null) || predicate.check(glyph)) {
                prevGlyph = glyph;
            }
        }

        return null;
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Changes current glyph id of the model.  If the glyph id is illegal then
     * an <code>IllegalArgumentException</code> is thrown.
     *
     * @param value the value to set
     * @exception IllegalArgumentException if <code>value</code> isn't allowed
     */
    @Implement(SpinnerModel.class)
    public void setValue (Object value)
    {
        if (logger.isFineEnabled()) {
            logger.fine("setValue value=" + value);
        }

        Integer id = (Integer) value;
        boolean ok = false;

        if (id == NO_VALUE) {
            ok = true;
        } else {
            // Scene
            Glyph glyph = scene.getGlyph(id);

            if (glyph != null) {
                if (predicate != null) {
                    ok = predicate.check(glyph);
                } else {
                    ok = true;
                }
            }
        }

        if (ok) {
            currentId = id;
            fireStateChanged();
        } else {
            logger.warning("Invalid glyph id: " + id);
        }
    }

    //----------//
    // getValue //
    //----------//
    /**
     * The <i>current element</i> of the sequence.
     *
     * @return the current spinner value.
     */
    @Implement(SpinnerModel.class)
    public Object getValue ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("getValue currentId=" + currentId);
        }

        return currentId;
    }
}
