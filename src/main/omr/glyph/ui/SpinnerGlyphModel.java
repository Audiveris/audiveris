//----------------------------------------------------------------------------//
//                                                                            //
//                     S p i n n e r G l y p h M o d e l                      //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.GlyphLag;
import static omr.ui.field.SpinnerUtilities.*;

import omr.util.Implement;
import omr.util.Logger;
import omr.util.Predicate;

import javax.swing.*;

/**
 * Class <code>SpinnerGlyphModel</code> is a spinner model backed by a {@link
 * GlyphLag}. Any modification in the lag is thus transparently handled, since
 * the lag <b>is</b> the model. <p>A glyph {@link Predicate} can be assigned to
 * this SpinnerGlyphModel at construction time in order to restrict the
 * population of glyphs in the spinner.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SpinnerGlyphModel
    extends AbstractSpinnerModel
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger    logger = Logger.getLogger(
        SpinnerGlyphModel.class);

    //~ Instance fields --------------------------------------------------------

    // Underlying glyph lag
    private final GlyphLag         lag;

    // Additionnal predicate if any
    private final Predicate<Glyph> predicate;

    // Current glyph id
    private Integer currentId;

    //~ Constructors -----------------------------------------------------------

    //-------------------//
    // SpinnerGlyphModel //
    //-------------------//
    /**
     * Creates a new SpinnerGlyphModel object, on all lag glyphs
     *
     * @param lag the underlying glyph lag
     */
    public SpinnerGlyphModel (GlyphLag lag)
    {
        this(lag, null);
    }

    //-------------------//
    // SpinnerGlyphModel //
    //-------------------//
    /**
     * Creates a new SpinnerGlyphModel object, with a related glyph predicate
     *
     * @param lag the underlying glyph lag
     * @param predicate predicate of glyph
     */
    public SpinnerGlyphModel (GlyphLag         lag,
                              Predicate<Glyph> predicate)
    {
        if (lag == null) {
            throw new IllegalArgumentException(
                "SpinnerGlyphModel expects non-null glyph lag");
        }

        this.lag = lag;
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
            // Return first suitable glyph in lag
            for (Glyph glyph : lag.getGlyphs()) {
                if ((predicate == null) || predicate.check(glyph)) {
                    return glyph.getId();
                }
            }

            return null;
        } else {
            // Return first suitable glyph after current glyph
            boolean found = false;

            for (Glyph glyph : lag.getGlyphs()) {
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

        for (Glyph glyph : lag.getGlyphs()) {
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
        boolean ok;

        if (id == NO_VALUE) {
            ok = true;
        } else {
            Glyph glyph = lag.getGlyph(id);

            if (glyph != null) {
                if (predicate != null) {
                    ok = predicate.check(glyph);
                } else {
                    ok = true;
                }
            } else {
                ok = false;
            }
        }

        if (ok) {
            currentId = id;
            fireStateChanged();
        } else {
            ///logger.warning("invalid element : " + id);
            throw new IllegalArgumentException("invalid element : " + id);
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
