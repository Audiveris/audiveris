//----------------------------------------------------------------------------//
//                                                                            //
//                          K e y S i g n a t u r e                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;

import omr.score.visitor.Visitor;

import omr.sheet.Scale;

import omr.util.Logger;

import java.util.*;

/**
 * Class <code>KeySignature</code> encapsulates a key signature, which may be
 * composed of one or several sticks.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class KeySignature
    extends StaffNode
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger    logger = Logger.getLogger(
        KeySignature.class);

    //~ Instance fields --------------------------------------------------------

    /**
     * Precise key signature. 0 for none, +n for n sharps, -n for n flats
     */
    private Integer key;

    /**
     * The glyph(s) that compose the key signature, a collection which is kept
     * sorted on glyph abscissa.
     */
    private SortedSet<Glyph> glyphs = new TreeSet<Glyph>();

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // KeySignature //
    //--------------//
    /**
     * Create a key signature, with related sheet scale and containing staff
     *
     * @param measure the containing measure
     * @param scale the sheet global scale
     */
    public KeySignature (Measure measure,
                         Scale   scale)
    {
        super(measure, measure.getStaff());
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // getKey //
    //--------//
    /**
     * Report the key signature
     *
     * @return the (lazily determined) key
     */
    public Integer getKey ()
    {
        if (key == null) {
            computekey();
        }

        return key;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (Visitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // addGlyph //
    //----------//
    /**
     * Add a new glyph as part of this key signature
     *
     * @param glyph the new component glyph
     */
    public void addGlyph (Glyph glyph)
    {
        glyphs.add(glyph);
        reset();
    }

    //-------//
    // reset //
    //-------//
    /**
     * Invalidate cached data, so that it gets lazily recomputed when needed
     */
    public void reset ()
    {
        center = null;
        key = null;
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description
     *
     * @return description
     */
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{KeySignature");

        sb.append(" key=")
          .append(key);

        sb.append(" center=")
          .append(getCenter());

        sb.append(" glyphs[");

        for (Glyph glyph : glyphs) {
            sb.append("#")
              .append(glyph.getId());
        }

        sb.append("]");
        sb.append("}");

        return sb.toString();
    }

    //---------------//
    // computeCenter //
    //---------------//
    @Override
    protected void computeCenter ()
    {
        center = computeGlyphsCenter(glyphs);
    }

    //----------//
    // populate //
    //----------//
    /**
     * Populate the score with a key signature built from the provided glyph
     *
     * @param glyph the source glyph
     * @param measure containing measure
     * @param scale sheet scale
     *
     * @return true if population is successful, false otherwise
     */
    static boolean populate (Glyph   glyph,
                             Measure measure,
                             Scale   scale)
    {
        // Make sure we have no note-head nearby (test already done ?)

        // Do we have a (beginning of) key signature nearby ?
        // If so, just try to extend it, else create a brand new one
        StaffPoint center = measure.computeGlyphCenter(glyph);
        int        unitDx = center.x - measure.getLeftX();

        // Then, processing
        return true;
    }

    //------------//
    // computekey //
    //------------//
    private void computekey ()
    {
        if (glyphs.size() > 0) {
            // Check we have only sharps or only flats
            Shape shape = null;

            for (Glyph glyph : glyphs) {
                if ((shape != null) && (glyph.getShape() != shape)) {
                    logger.warning("Inconsistent key signature " + this);

                    return;
                } else {
                    shape = glyph.getShape();
                }
            }

            // Number and shape determine key signature
            if (shape == SHARP) {
                key = glyphs.size();
            } else if (shape == FLAT) {
                key = -glyphs.size();
            } else {
                logger.warning("Weird key signature " + this);
            }
        } else {
            logger.warning("Empty key signature " + this);
        }
    }
}
