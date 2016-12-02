//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         K e y S l i c e                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sheet.header;

import omr.classifier.Evaluation;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.sheet.Staff;

import omr.sig.inter.AlterInter;
import omr.sig.inter.ClefInter;
import omr.sig.inter.KeyAlterInter;
import omr.sig.inter.KeyInter;

import java.awt.Rectangle;

/**
 * Class {@code KeySlice} represents a rectangular slice of a key signature, likely to
 * contain an alteration item.
 *
 * @author Hervé Bitteur
 */
public class KeySlice
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Containing Roi. */
    private final KeyRoi roi;

    /** Rectangular slice definition. */
    Rectangle rect;

    /** Best glyph, if any. */
    Glyph glyph;

    /** Best evaluation, if any. */
    Evaluation eval;

    /** Retrieved alter item, if any. */
    KeyAlterInter alter;

    /** If occupied by non-valid material. */
    private boolean stuffed;

    //~ Constructors -------------------------------------------------------------------------------
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

    //~ Methods ------------------------------------------------------------------------------------
    public void deleteAlter ()
    {
        if (alter != null) {
            alter.delete();
            alter = null;
        }
    }

    /**
     * @return the alter, if any
     */
    public KeyAlterInter getAlter ()
    {
        return alter;
    }

    public int getId ()
    {
        return 1 + roi.indexOf(this);
    }

    /**
     * @return a fixed-size description string (9 char)
     */
    public String getLabel ()
    {
        if (alter != null) {
            return String.format(
                    "%s%+1d %-5d",
                    (alter.getShape() == Shape.FLAT) ? "b" : "#",
                    alter.getIntegerPitch(),
                    alter.getId());
        } else if (stuffed) {
            return "STUFFED  ";
        } else {
            return "NO_ALTER ";
        }
    }

    /**
     * @return the rectangle definition
     */
    public final Rectangle getRect ()
    {
        return new Rectangle(rect);
    }

    public final int getStart ()
    {
        return rect.x;
    }

    public final int getStop ()
    {
        return (rect.x + rect.width) - 1;
    }

    public final int getWidth ()
    {
        return rect.width;
    }

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
                              double typicalHeight)
    {
        final Staff staff = roi.getStaff();
        final int[] clefPitches = KeyInter.getPitches(clef.getKind(), keyShape);
        final int pitch = clefPitches[getId() - 1];
        final double yp = staff.pitchToOrdinate(rect.x, pitch);
        final double height = typicalHeight;
        final double ratio = (keyShape == Shape.FLAT) ? AlterInter.getFlatAreaOffset() : 0.5;
        final double offset = height * ratio;
        final int y = (int) Math.rint(yp - offset);
        final int h = (int) Math.rint(height);
        setRect(new Rectangle(getStart(), y, getWidth(), h));
        staff.addAttachment("k" + getId(), rect);
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
