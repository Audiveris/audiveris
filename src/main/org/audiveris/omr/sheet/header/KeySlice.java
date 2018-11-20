//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         K e y S l i c e                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sheet.header;

import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sig.inter.AlterInter;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.KeyAlterInter;
import org.audiveris.omr.sig.inter.KeyInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;

/**
 * Class {@code KeySlice} represents a rectangular slice of a key signature, likely to
 * contain an alteration item.
 *
 * @author Hervé Bitteur
 */
public class KeySlice
{

    private static final Logger logger = LoggerFactory.getLogger(KeySlice.class);

    /** Containing Roi. */
    private final KeyRoi roi;

    /** Rectangular slice definition. */
    private Rectangle rect;

    /** Best glyph, if any. */
    private Glyph glyph;

    /** Best evaluation, if any. */
    private Evaluation eval;

    /** Retrieved alter item, if any. */
    private KeyAlterInter alter;

    /** If occupied by non-valid material. */
    private boolean stuffed;

    /**
     * Creates a new {@code KeySlice} object.
     *
     * @param rect bounds of the slice
     * @param roi  the container in staff KeyBuilder
     */
    public KeySlice (Rectangle rect,
                     KeyRoi roi)
    {
        this.rect = rect;
        this.roi = roi;
    }

    /**
     * Remove the alter in this slice.
     */
    public void deleteAlter ()
    {
        if (alter != null) {
            alter.remove();
            alter = null;
        }
    }

    /**
     * Report the slice alter
     *
     * @return the alter, if any
     */
    public KeyAlterInter getAlter ()
    {
        return alter;
    }

    /**
     * Set slice alter
     *
     * @param alter the alter to set
     */
    public void setAlter (KeyAlterInter alter)
    {
        deleteAlter();
        this.alter = alter;
    }

    /**
     * Report slice evaluation
     *
     * @return the eval
     */
    public Evaluation getEval ()
    {
        return eval;
    }

    /**
     * Set slice evaluation
     *
     * @param eval the eval to set
     */
    public void setEval (Evaluation eval)
    {
        this.eval = eval;
    }

    /**
     * Report slice glyph
     *
     * @return the glyph
     */
    public Glyph getGlyph ()
    {
        return glyph;
    }

    /**
     * Set slice glyph
     *
     * @param glyph the glyph to set
     */
    public void setGlyph (Glyph glyph)
    {
        this.glyph = glyph;
    }

    /**
     * Report slice ID
     *
     * @return 1-based rank of slice within key
     */
    public int getId ()
    {
        return 1 + roi.indexOf(this);
    }

    /**
     * Report a slice string description
     *
     * @return a fixed-size description string (9 char)
     */
    public String getLabel ()
    {
        if (alter != null) {
            return String.format("%s%+1d %-5d", (alter.getShape() == Shape.FLAT) ? "b" : "#", alter
                                 .getIntegerPitch(), alter.getId());
        } else if (stuffed) {
            return "STUFFED  ";
        } else {
            return "NO_ALTER ";
        }
    }

    /**
     * Report slice bounds
     *
     * @return the rectangle definition
     */
    public final Rectangle getRect ()
    {
        return new Rectangle(rect);
    }

    /**
     * Define a new rectangle for this slice.
     *
     * @param rect the slice new rectangle
     */
    public void setRect (Rectangle rect)
    {
        this.rect = new Rectangle(rect);
    }

    /**
     * Report slice starting abscissa
     *
     * @return the abscissa value on left side
     */
    public final int getStart ()
    {
        return rect.x;
    }

    /**
     * Report slice ending abscissa
     *
     * @return the abscissa value of right side
     */
    public final int getStop ()
    {
        return (rect.x + rect.width) - 1;
    }

    /**
     * Report slice width
     *
     * @return the slice width
     */
    public final int getWidth ()
    {
        return rect.width;
    }

    /**
     * Tell whether slice is stuffed with some invalid material.
     *
     * @return true if so
     */
    public boolean isStuffed ()
    {
        return stuffed;
    }

    //--------------//
    // setPitchRect //
    //--------------//
    /**
     * Redefine slice rectangle as the pitch-based lookup area.
     *
     * @param clef          active clef
     * @param keyShape      key shape
     * @param typicalHeight typical item height
     */
    public void setPitchRect (ClefInter clef,
                              Shape keyShape,
                              int typicalHeight)
    {
        final Staff staff = roi.getStaff();
        final int[] clefPitches = KeyInter.getPitches(clef.getKind(), keyShape);
        final int pitch = clefPitches[getId() - 1];
        final double yp = staff.pitchToOrdinate(rect.x, pitch);
        final int height = typicalHeight;
        final double ratio = (keyShape == Shape.FLAT) ? AlterInter.getFlatAreaOffset() : 0.5;
        final double offset = height * ratio;
        final int y = (int) Math.rint(yp - offset);
        setRect(new Rectangle(getStart(), y, getWidth(), height));
        staff.addAttachment(roi.attachmentKey(getId()), rect);
    }

    /**
     *
     */
    public void setStuffed ()
    {
        stuffed = true;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("Slice{");
        sb.append("#").append(getId());

        if (alter != null) {
            sb.append(" ").append(alter);
        }

        if (stuffed) {
            sb.append(" STUFFED");
        }

        if (glyph != null) {
            sb.append(String.format(" glyph#%d %.3f", glyph.getId(), eval.grade));
        }

        sb.append("}");

        return sb.toString();
    }
}
