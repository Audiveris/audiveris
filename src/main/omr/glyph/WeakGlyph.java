//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        W e a k G l y p h                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.util.Entity;
import omr.util.IdUtil;

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
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code WeakGlyph} object.
     *
     * @param referent the actual glyph, which may no longer exist
     */
    public WeakGlyph (Glyph referent)
    {
        super(referent);
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public int compareTo (WeakGlyph that)
    {
        final Glyph thisGlyph = this.get();
        final Glyph thatGlyph = that.get();

        if (thisGlyph == null) {
            if (thatGlyph == null) {
                return 0;
            }

            return -1;
        }

        if (thatGlyph == null) {
            return +1;
        }

        return IdUtil.compare(thisGlyph.getId(), thatGlyph.getId());
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
    public String getId ()
    {
        final Glyph glyph = get();

        if (glyph != null) {
            return glyph.getId();
        }

        return null;
    }

    @Override
    public int getIntId ()
    {
        final Glyph glyph = get();

        if (glyph != null) {
            return glyph.getIntId();
        }

        return 0;
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
    public void setId (String id)
    {
        final Glyph glyph = get();

        if (glyph != null) {
            glyph.setId(id);
        }
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
