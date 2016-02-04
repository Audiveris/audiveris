//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               S p i n n e r G l y p h M o d e l                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.GlyphIndex;

import omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

import javax.swing.AbstractSpinnerModel;

/**
 * Class {@code SpinnerGlyphModel} is a spinner model backed by a {@link GlyphIndex}.
 * Any modification in the nest is thus transparently handled, since the nest <b>is</b> the model.
 * <p>
 * A glyph {@link Predicate} can be assigned to this SpinnerGlyphModel at construction time in order
 * to restrict the population of glyphs in the spinner.
 * This class is used by {@link GlyphBoard} only, but is not coupled with it.
 *
 * @author Hervé Bitteur
 */
public class SpinnerGlyphModel
        extends AbstractSpinnerModel
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SpinnerGlyphModel.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Underlying glyph nest */
    private final GlyphIndex nest;

    /** Additional predicate if any */
    private final Predicate<Glyph> predicate;

    /** Current glyph id */
    private int currentId;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SpinnerGlyphModel object, on all nest glyph instances.
     *
     * @param nest the underlying glyph nest
     */
    public SpinnerGlyphModel (GlyphIndex nest)
    {
        this(nest, null);
    }

    /**
     * Creates a new SpinnerGlyphModel object, with a related glyph predicate.
     *
     * @param nest      the underlying glyph nest
     * @param predicate predicate of glyph, or null
     */
    public SpinnerGlyphModel (GlyphIndex nest,
                              Predicate<Glyph> predicate)
    {
        if (nest == null) {
            throw new IllegalArgumentException("SpinnerGlyphModel expects non-null glyph nest");
        }

        this.nest = nest;
        this.predicate = predicate;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // getNextValue //
    //--------------//
    /**
     * Return the next legal glyph id in the sequence that comes after
     * the glyph id returned by {@code getValue()}.
     * If the end of the sequence has been reached then return null.
     *
     * @return the next legal glyph id or null if one doesn't exist
     */
    @Override
    public Object getNextValue ()
    {
        logger.debug("getNextValue cur={}", currentId);

        if (currentId == 0) {
            // Return first suitable glyph in nest
            for (Iterator<Glyph> it = nest.iterator(); it.hasNext();) {
                Glyph glyph = it.next();

                if ((glyph != null) && ((predicate == null) || predicate.check(glyph))) {
                    return glyph.getId();
                }
            }

            return null;
        } else {
            // Return first suitable glyph after current glyph in nest
            boolean found = false;

            for (Iterator<Glyph> it = nest.iterator(); it.hasNext();) {
                final Glyph glyph = it.next();

                if (!found) {
                    if (glyph.getId() == currentId) {
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
     * Return the legal glyph id in the sequence that comes before the
     * glyph id returned by {@code getValue()}.
     * If the end of the sequence has been reached then return null.
     *
     * @return the previous legal value or null if one doesn't exist
     */
    @Override
    public Object getPreviousValue ()
    {
        Glyph prevGlyph = null;
        logger.debug("getPreviousValue cur={}", currentId);

        if (currentId == 0) {
            return null;
        }

        // GlyphIndex
        for (Iterator<Glyph> it = nest.iterator(); it.hasNext();) {
            final Glyph glyph = it.next();

            if (glyph.getId() == currentId) {
                return (prevGlyph != null) ? prevGlyph.getId() : null;
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
     * Changes current glyph id of the model.
     * If the glyph id is illegal then an {@code IllegalArgumentException} is thrown.
     *
     * @param value the value to set
     * @throws IllegalArgumentException if {@code value} isn't allowed
     */
    @Override
    public void setValue (Object value)
    {
        logger.debug("setValue value={}", value);

        Integer id = (Integer) value;
        boolean ok = false;

        if (id == null) {
            ok = true;
        } else {
            // GlyphIndex
            final Glyph glyph = nest.getEntity(id);

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
