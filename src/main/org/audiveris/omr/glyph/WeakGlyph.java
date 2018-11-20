//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        W e a k G l y p h                                       //
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
package org.audiveris.omr.glyph;

import org.audiveris.omr.util.Entity;

import java.awt.Point;
import java.awt.Rectangle;
import java.lang.ref.WeakReference;

/**
 * Class {@code WeakGlyph} is a WeakReference to (and an Entity facade for) a Glyph.
 *
 * @author Hervé Bitteur
 */
public class WeakGlyph
        extends WeakReference<Glyph>
        implements Entity, Comparable<WeakGlyph>
{

    /**
     * Creates a new {@code WeakGlyph} object.
     *
     * @param referent the actual glyph, which may no longer exist
     */
    public WeakGlyph (Glyph referent)
    {
        super(referent);
    }

    @Override
    public int compareTo (WeakGlyph that)
    {
        if (this == that) {
            return 0;
        }

        final Glyph thisGlyph = this.get();
        final Glyph thatGlyph = that.get();

        if (thisGlyph == thatGlyph) {
            return 0;
        }

        if (thisGlyph == null) {
            return -1;
        }

        if (thatGlyph == null) {
            return +1;
        }

        return Integer.compare(thisGlyph.getId(), thatGlyph.getId());
    }

    @Override
    public boolean contains (Point point)
    {
        final Glyph glyph = get();

        if (glyph != null) {
            return glyph.contains(point);
        }

        return false;
    }

    @Override
    public String dumpOf ()
    {
        final Glyph glyph = get();

        if (glyph != null) {
            return glyph.dumpOf();
        }

        return "";
    }

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final Glyph glyph = get();
        final Glyph otherGlyph = ((WeakGlyph) obj).get();

        if (glyph == null) {
            return otherGlyph == null;
        }

        return glyph.equals(otherGlyph);
    }

    @Override
    public Rectangle getBounds ()
    {
        final Glyph glyph = get();

        if (glyph != null) {
            return glyph.getBounds();
        }

        return null;
    }

    @Override
    public int getId ()
    {
        final Glyph glyph = get();

        if (glyph != null) {
            return glyph.getId();
        }

        return 0;
    }

    @Override
    public void setId (int id)
    {
        final Glyph glyph = get();

        if (glyph != null) {
            glyph.setId(id);
        }
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        final Glyph glyph = get();

        if (glyph != null) {
            return glyph.hashCode();
        }

        int hash = 3;

        return hash;
    }

    @Override
    public boolean isVip ()
    {
        final Glyph glyph = get();

        if (glyph != null) {
            return glyph.isVip();
        }

        return false;
    }

    @Override
    public void setVip (boolean vip)
    {
        final Glyph glyph = get();

        if (glyph != null) {
            glyph.setVip(vip);
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        final Glyph glyph = get();

        return "wr-" + ((glyph != null) ? glyph : "null");
    }
}
