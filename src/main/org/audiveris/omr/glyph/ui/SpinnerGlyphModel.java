//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               S p i n n e r G l y p h M o d e l                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphIndex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.function.Predicate;

import javax.swing.AbstractSpinnerModel;

/**
 * Class <code>SpinnerGlyphModel</code> is a spinner model backed by a {@link GlyphIndex}.
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
     * the glyph id returned by <code>getValue()</code>.
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

                if ((glyph != null) && ((predicate == null) || predicate.test(glyph))) {
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
                } else if ((predicate == null) || predicate.test(glyph)) {
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
     * glyph id returned by <code>getValue()</code>.
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
            if ((predicate == null) || predicate.test(glyph)) {
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
     * If the glyph id is illegal then an <code>IllegalArgumentException</code> is thrown.
     *
     * @param value the value to set
     * @throws IllegalArgumentException if <code>value</code> isn't allowed
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
                    ok = predicate.test(glyph);
                } else {
                    ok = true;
                }
            }
        }

        if (ok && (id != null)) {
            currentId = id;
            fireStateChanged();
        } else {
            logger.warn("Invalid glyph id: {}", id);
        }
    }
}
