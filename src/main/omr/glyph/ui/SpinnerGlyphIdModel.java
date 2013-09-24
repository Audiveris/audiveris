//----------------------------------------------------------------------------//
//                                                                            //
//                    S p i n n e r G l y p h I d M o d e l                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.Nest;
import omr.glyph.facets.Glyph;
import static omr.ui.field.SpinnerUtil.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractSpinnerModel;

/**
 * Class {@literal SpinnerGlyphIdModel} is a simple spinner model backed
 * by a {@link Nest} and using natural glyph id sequence.
 *
 * @author Hervé Bitteur
 */
public class SpinnerGlyphIdModel
        extends AbstractSpinnerModel
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            SpinnerGlyphIdModel.class);

    //~ Instance fields --------------------------------------------------------
    /** Underlying glyph nest */
    private final Nest nest;

    /** Current glyph id */
    private int currentId = NO_VALUE;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new SpinnerGlyphIdModel object.
     *
     * @param nest the underlying glyph nest
     */
    public SpinnerGlyphIdModel (Nest nest)
    {
        this.nest = nest;
    }

    //~ Methods ----------------------------------------------------------------
    @Override
    public Object getNextValue ()
    {
        Glyph glyph = nest.getGlyph(currentId + 1);

        if (glyph != null) {
            return glyph.getId();
        } else {
            return null;
        }
    }

    @Override
    public Object getPreviousValue ()
    {
        if (currentId == NO_VALUE) {
            return NO_VALUE;
        } else {
            Glyph glyph = nest.getGlyph(currentId - 1);

            if (glyph != null) {
                return glyph.getId();
            } else {
                return null;
            }
        }
    }

    @Override
    public Object getValue ()
    {
        return currentId;
    }

    @Override
    public void setValue (Object value)
    {
        Integer id = (Integer) value;
        boolean ok = false;

        if (id == NO_VALUE) {
            ok = true;
        } else {
            Glyph glyph = nest.getGlyph(id);

            if (glyph != null) {
                ok = true;
            }
        }

        if (ok) {
            currentId = id;
            fireStateChanged();
        } else {
            logger.warn("Invalid glyph id: {}", id);
        }
    }
}
