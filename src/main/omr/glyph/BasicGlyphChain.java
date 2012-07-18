//----------------------------------------------------------------------------//
//                                                                            //
//                       B a s i c G l y p h C h a i n                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2012. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.Glyph;

import omr.lag.Section;

import omr.log.Logger;

import omr.sheet.SystemInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code BasicGlyphChain} implements GlyphChain, a sorted chain
 * of glyphs, called items.
 *
 * @author Hervé Bitteur
 */
public class BasicGlyphChain
        implements GlyphChain
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
            BasicGlyphChain.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** Abscissa-ordered collection of items */
    private final SortedSet<Glyph> items = Glyphs.sortedSet();

    /** Resulting compound */
    private Glyph compound;

    /** VIP flag */
    private boolean vip;

    //~ Constructors -----------------------------------------------------------
    //-----------------//
    // BasicGlyphChain //
    //-----------------//
    /**
     * Creates a new BasicGlyphChain object, with a sequence of items.
     *
     * @param items a sequence of items
     */
    public BasicGlyphChain (List<Glyph> items)
    {
        setItems(items);

        logger.fine("Multi-glyph {0}", this);
    }

    //~ Methods ----------------------------------------------------------------
    //-------------//
    // addAllItems //
    //-------------//
    @Override
    public void addAllItems (Collection<? extends Glyph> glyphs)
    {
        if (glyphs == null) {
            throw new IllegalArgumentException(
                    "Cannot add a null collection of items");
        }

        items.addAll(glyphs);

        // VIP extension from glyph to containingsentence
        for (Glyph item : glyphs) {
            if (item.isVip()) {
                setVip();
                break;
            }
        }


        invalidateCache();
    }

    //---------//
    // addItem //
    //---------//
    @Override
    public final boolean addItem (Glyph glyph)
    {
        if (glyph == null) {
            throw new IllegalArgumentException("Cannot add a null item");
        }

        if (glyph == this) {
            throw new IllegalArgumentException("Cannot resursively add itself");
        }

        boolean bool = items.add(glyph);
        invalidateCache();

        if (items.size() > 1) {
            logger.fine("Extended {0}", this);
        }

        return bool;
    }

    //------//
    // dump //
    //------//
    public void dump ()
    {
        System.out.println("   compound=" + compound);
        System.out.println(Glyphs.toString("   items=", getItems()));
    }

    //-------------//
    // getCompound //
    //-------------//
    @Override
    public Glyph getCompound ()
    {
        // Lazy allocation
        if (compound == null) {
            SystemInfo system = getFirstItem().getSystem();
            Glyph comp = null;

            if (getItemCount() == 1) {
                comp = getFirstItem();
            } else {
                comp = system.buildTransientGlyph(getMembers());
            }

            compound = system.registerGlyph(comp);
        }

        return compound;
    }

    //--------------//
    // getFirstItem //
    //--------------//
    @Override
    public Glyph getFirstItem ()
    {
        return items.first();
    }

    //--------------//
    // getItemAfter //
    //--------------//
    @Override
    public Glyph getItemAfter (Glyph start)
    {
        boolean started = start == null;

        for (Glyph glyph : items) {
            if (started) {
                return glyph;
            }

            if (glyph == start) {
                started = true;
            }
        }

        return null;
    }

    //---------------//
    // getItemBefore //
    //---------------//
    @Override
    public Glyph getItemBefore (Glyph stop)
    {
        Glyph prev = null;

        for (Glyph glyph : items) {
            if (glyph == stop) {
                return prev;
            }

            prev = glyph;
        }

        return prev;
    }

    //--------------//
    // getItemCount //
    //--------------//
    @Override
    public int getItemCount ()
    {
        return items.size();
    }

    //----------//
    // getItems //
    //----------//
    @Override
    public SortedSet<Glyph> getItems ()
    {
        return Collections.unmodifiableSortedSet(items);
    }

    //-------------//
    // getLastItem //
    //-------------//
    @Override
    public Glyph getLastItem ()
    {
        return items.last();
    }

    //------------//
    // getMembers //
    //------------//
    /**
     * Report all sections from all items.
     *
     * @return the cumulated set of items member sections
     */
    public SortedSet<Section> getMembers ()
    {
        SortedSet<Section> allMembers = new TreeSet<>();

        for (Glyph item : items) {
            allMembers.addAll(item.getMembers());
        }

        return allMembers;
    }

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip ()
    {
        return vip;
    }

    //--------//
    // setVip //
    //--------//
    @Override
    public final void setVip ()
    {
        vip = true;
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    public void invalidateCache ()
    {
        compound = null;
    }

    //------------//
    // removeItem //
    //------------//
    @Override
    public boolean removeItem (Glyph item)
    {
        if (item == null) {
            throw new IllegalArgumentException("Cannot remove a null item");
        }

        boolean bool = items.remove(item);

        if (bool) {
            invalidateCache();
        }

        return bool;
    }

    //----------//
    // setItems //
    //----------//
    @Override
    public final void setItems (Collection<? extends Glyph> items)
    {
        if (items == null) {
            throw new IllegalArgumentException(
                    "Cannot set a null items collection");
        }

        if (this.items == items) {
            return;
        }

        this.items.clear();

        // Add the new links
        addAllItems(items);        
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());
        sb.append(internalsString());
        sb.append("}");

        return sb.toString();
    }

    //-----------------//
    // internalsString //
    //-----------------//
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();

        try {
            sb.append(" compound#");
            sb.append(getCompound().getId());
        } catch (Exception ex) {
            sb.append("INVALID");
        }

        sb.append(Glyphs.toString(" items", items));

        return sb.toString();
    }
}
