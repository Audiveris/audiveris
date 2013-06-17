//----------------------------------------------------------------------------//
//                                                                            //
//                     S p i n n e r G l y p h M o d e l                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.Nest;
import omr.glyph.facets.Glyph;
import static omr.ui.field.SpinnerUtil.*;

import omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractSpinnerModel;

/**
 * Class {@code SpinnerGlyphModel} is a spinner model backed by a
 * {@link Nest}.
 * Any modification in the nest is thus transparently handled, since the nest
 * <b>is</b> the model.
 * <p>A glyph {@link Predicate} can be assigned to this SpinnerGlyphModel at
 * construction time in order to restrict the population of glyphs in the
 * spinner.
 * This class is used by {@link GlyphBoard} only, but is not coupled with it.
 *
 * @author Hervé Bitteur
 */
public class SpinnerGlyphModel
        extends AbstractSpinnerModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            SpinnerGlyphModel.class);

    //~ Instance fields --------------------------------------------------------
    /** Underlying glyph nest */
    private final Nest nest;

    /** Additional predicate if any */
    private final Predicate<Glyph> predicate;

    /** Current glyph id */
    private Integer currentId;

    //~ Constructors -----------------------------------------------------------
    //-------------------//
    // SpinnerGlyphModel //
    //-------------------//
    /**
     * Creates a new SpinnerGlyphModel object, on all nest glyphs
     *
     * @param nest the underlying glyph nest
     */
    public SpinnerGlyphModel (Nest nest)
    {
        this(nest, null);
    }

    //-------------------//
    // SpinnerGlyphModel //
    //-------------------//
    /**
     * Creates a new SpinnerGlyphModel object, with a related glyph predicate
     *
     * @param nest      the underlying glyph nest
     * @param predicate predicate of glyph, or null
     */
    public SpinnerGlyphModel (Nest nest,
                              Predicate<Glyph> predicate)
    {
        if (nest == null) {
            throw new IllegalArgumentException(
                    "SpinnerGlyphModel expects non-null glyph nest");
        }

        this.nest = nest;
        this.predicate = predicate;

        currentId = NO_VALUE;
    }

    //~ Methods ----------------------------------------------------------------
    //--------------//
    // getNextValue //
    //--------------//
    /**
     * Return the next legal glyph id in the sequence that comes after the glyph
     * id returned by {@code getValue()}. If the end of the sequence has
     * been reached then return null.
     *
     * @return the next legal glyph id or null if one doesn't exist
     */
    @Override
    public Object getNextValue ()
    {
        final int cur = currentId.intValue();
        logger.debug("getNextValue cur={}", cur);

        if (cur == NO_VALUE) {
            // Return first suitable glyph in nest
            for (Glyph glyph : nest.getAllGlyphs()) {
                if ((predicate == null) || predicate.check(glyph)) {
                    return glyph.getId();
                }
            }

            return null;
        } else {
            // Return first suitable glyph after current glyph in nest
            boolean found = false;

            for (Glyph glyph : nest.getAllGlyphs()) {
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
     * returned by {@code getValue()}. If the end of the sequence has been
     * reached then return null.
     *
     * @return the previous legal value or null if one doesn't exist
     */
    @Override
    public Object getPreviousValue ()
    {
        Glyph prevGlyph = null;
        final int cur = currentId.intValue();
        logger.debug("getPreviousValue cur={}", cur);

        if (cur == NO_VALUE) {
            return NO_VALUE;
        }

        // Nest
        for (Glyph glyph : nest.getAllGlyphs()) {
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
    // getValue //
    //----------//
    /**
     * The <i>current element</i> of the sequence.
     *
     * @return the current spinner value.
     */
    @Override
    public Object getValue ()
    {
        logger.debug("getValue currentId={}", currentId);

        return currentId;
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Changes current glyph id of the model. If the glyph id is illegal then
     * an {@code IllegalArgumentException} is thrown.
     *
     * @param value the value to set
     * @exception IllegalArgumentException if {@code value} isn't allowed
     */
    @Override
    public void setValue (Object value)
    {
        logger.debug("setValue value={}", value);

        Integer id = (Integer) value;
        boolean ok = false;

        if (id == NO_VALUE) {
            ok = true;
        } else {
            // Nest
            Glyph glyph = nest.getGlyph(id);

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
            logger.warn("Invalid glyph id: {}", id);
        }
    }
}
