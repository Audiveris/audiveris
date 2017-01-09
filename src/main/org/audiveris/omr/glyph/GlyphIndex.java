//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      G l y p h I n d e x                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Symbol.Group;
import org.audiveris.omr.glyph.ui.GlyphService;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.selection.GroupEvent;
import org.audiveris.omr.ui.selection.IdEvent;
import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.util.BasicIndex;
import org.audiveris.omr.util.EntityIndex;
import org.audiveris.omr.util.IntUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code GlyphIndex} implements an index of (weak references to) Glyph instances.
 * <p>
 * TODO: investigate whether the notion of Group could be dropped.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
public class GlyphIndex
        implements EntityIndex<Glyph>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            GlyphIndex.class);

    /** Events read on location service. */
    public static final Class<?>[] locEventsRead = new Class<?>[]{LocationEvent.class};

    /** Events read on glyph service. */
    public static final Class<?>[] eventsRead = new Class<?>[]{
        IdEvent.class, EntityListEvent.class,
        GroupEvent.class
    };

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    /**
     * See annotated get/set methods:
     * {@link #getEntities()}
     */
    //
    // Transient data
    //---------------
    //
    /** Underlying index to weak glyphs. */
    private final WeakGlyphIndex weakIndex = new WeakGlyphIndex();

    /** Collection of original glyph instances, non sorted. */
    private final ConcurrentHashMap<WeakGlyph, WeakGlyph> originals = new ConcurrentHashMap<WeakGlyph, WeakGlyph>();

    /** Selection service, if any. */
    private EntityService<Glyph> glyphService;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code GlyphIndex} object.
     */
    public GlyphIndex ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // initTransients //
    //----------------//
    public final void initTransients (Sheet sheet)
    {
        // ID generator
        weakIndex.setIdGenerator(sheet.getPersistentIdGenerator());

        // Declared VIP IDs?
        List<Integer> vipIds = IntUtil.parseInts(constants.vipGlyphs.getValue());

        if (!vipIds.isEmpty()) {
            logger.info("VIP glyphs: {}", vipIds);
            weakIndex.setVipIds(vipIds);
        }

        for (Iterator<Glyph> it = iterator(); it.hasNext();) {
            Glyph glyph = it.next();

            if (glyph != null) {
                glyph.setIndex(this);

                if (isVipId(glyph.getId())) {
                    glyph.setVip(true);
                }
            }
        }

        // User glyph service?
        if (OMR.gui != null) {
            SelectionService locationService = (sheet != null) ? sheet.getLocationService() : null;
            setEntityService(new GlyphService(this, locationService));
        }
    }

    //-----------------//
    // containedGlyphs //
    //-----------------//
    /**
     * Look up for <b>all</b> glyph instances in the provided group that are contained
     * in a provided rectangle.
     *
     * @param rect  the coordinates rectangle
     * @param group the containing glyph group
     * @return the list of glyph instances found, perhaps empty but not null
     */
    public Set<Glyph> containedGlyphs (Rectangle rect,
                                       Group group)
    {
        Set<Glyph> set = new LinkedHashSet<Glyph>();

        for (Iterator<Glyph> it = iterator(); it.hasNext();) {
            Glyph glyph = it.next();

            if (glyph.hasGroup(group) && rect.contains(glyph.getBounds())) {
                set.add(glyph);
            }
        }

        return set;
    }

    //-------------//
    // getEntities //
    //-------------//
    @Override
    public ArrayList<Glyph> getEntities ()
    {
        Collection<WeakGlyph> weaks = weakIndex.getEntities();
        ArrayList<Glyph> glyphs = new ArrayList<Glyph>();

        for (WeakGlyph weak : weaks) {
            final Glyph glyph = weak.get();

            if (glyph != null) {
                glyphs.add(glyph);
            }
        }

        return glyphs;
    }

    @Override
    public Glyph getEntity (int id)
    {
        WeakGlyph weak = weakIndex.getEntity(id);

        if (weak != null) {
            return weak.get();
        }

        return null;
    }

    @Override
    public EntityService<Glyph> getEntityService ()
    {
        return glyphService;
    }

    @Override
    public int getIdAfter (int id)
    {
        return weakIndex.getIdAfter(id);
    }

    @Override
    public int getIdBefore (int id)
    {
        return weakIndex.getIdBefore(id);
    }

    @Override
    public int getLastId ()
    {
        return weakIndex.getLastId();
    }

    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return "weakGlyphs";
    }

    //------------------//
    // getSelectedGlyph //
    //------------------//
    /**
     * Report the glyph currently selected, if any
     *
     * @return the current glyph, or null
     */
    public Glyph getSelectedGlyph ()
    {
        final List<Glyph> list = getSelectedGlyphList();

        if ((list == null) || list.isEmpty()) {
            return null;
        }

        return list.get(list.size() - 1);
    }

    //----------------------//
    // getSelectedGlyphList //
    //----------------------//
    /**
     * Report the glyph list currently selected, if any
     *
     * @return the current glyph list, or null
     */
    @SuppressWarnings("unchecked")
    public List<Glyph> getSelectedGlyphList ()
    {
        return (List<Glyph>) glyphService.getSelection(EntityListEvent.class);
    }

    //------------------//
    // getSelectedGroup //
    //------------------//
    /**
     * Report the glyph group currently selected, if any.
     *
     * @return the current glyph group, or null
     */
    public Group getSelectedGroup ()
    {
        return (Group) glyphService.getSelection(GroupEvent.class);
    }

    //-------------------//
    // intersectedGlyphs //
    //-------------------//
    /**
     * Look up for <b>all</b> glyph instances from provided group and that are
     * intersected by the provided rectangle.
     *
     * @param rect  the coordinates rectangle
     * @param group the containing glyph group
     * @return the glyph instances found, which may be an empty list
     */
    public Set<Glyph> intersectedGlyphs (Rectangle rect,
                                         Group group)
    {
        Set<Glyph> set = new LinkedHashSet<Glyph>();

        for (Iterator<Glyph> it = iterator(); it.hasNext();) {
            Glyph glyph = it.next();

            if (glyph.hasGroup(group) && rect.intersects(glyph.getBounds())) {
                set.add(glyph);
            }
        }

        return set;
    }

    //---------//
    // isVipId //
    //---------//
    @Override
    public boolean isVipId (int id)
    {
        return weakIndex.isVipId(id);
    }

    //----------//
    // iterator //
    //----------//
    @Override
    public Iterator<Glyph> iterator ()
    {
        return new SkippingIterator(weakIndex.iterator());
    }

    //--------------------//
    // lookupVirtualGlyph //
    //--------------------//
    /**
     * Look for a glyph whose box contains the designated point for the drop group.
     *
     * @param point the designated point
     * @return the virtual glyph found, or null
     */
    public Glyph lookupVirtualGlyph (Point point)
    {
        for (Iterator<Glyph> it = iterator(); it.hasNext();) {
            Glyph glyph = it.next();

            if (glyph.hasGroup(Group.DROP) && glyph.getBounds().contains(point)) {
                return glyph;
            }
        }

        return null;
    }

    //---------//
    // publish //
    //---------//
    /**
     * Convenient debug UI method to publish and focus on a glyph.
     *
     * @param glyph the provided glyph
     */
    public void publish (final Glyph glyph)
    {
        if (glyphService != null) {
            SwingUtilities.invokeLater(
                    new Runnable()
            {
                @Override
                public void run ()
                {
                    glyphService.publish(
                            new EntityListEvent<Glyph>(
                                    this,
                                    SelectionHint.ENTITY_INIT,
                                    MouseMovement.PRESSING,
                                    (glyph != null) ? Arrays.asList(glyph) : null));
                }
            });
        }
    }

    //----------//
    // register //
    //----------//
    /**
     * This public method <b>must not be called</b> on GlyphIndex.
     * Use {@link #registerOriginal(omr.glyph.Glyph)} instead.
     *
     * @param glyph
     */
    @Override
    public int register (Glyph glyph)
    {
        throw new RuntimeException("register() must not be called on GlyphIndex");
    }

    //------------------//
    // registerOriginal //
    //------------------//
    /**
     * Check whether the provided glyph is really a new one and assign it an ID.
     * If so the glyph is returned, otherwise the original glyph is returned.
     *
     * @param glyph the glyph to check
     * @return the original one if any, otherwise this glyph
     */
    public synchronized Glyph registerOriginal (Glyph glyph)
    {
        WeakGlyph weak = new WeakGlyph(glyph);
        WeakGlyph orgWeak = originals.putIfAbsent(weak, weak);
        Glyph orgGlyph = (orgWeak != null) ? orgWeak.get() : null;

        if (orgGlyph == null) {
            privateRegister(glyph);

            return glyph;
        } else {
            logger.debug("Reuse original {}", orgGlyph);

            return orgGlyph;
        }
    }

    //--------//
    // remove //
    //--------//
    @Override
    public void remove (Glyph glyph)
    {
        WeakGlyph weak = new WeakGlyph(glyph);

        // Remove from global index
        weakIndex.remove(weak);
    }

    //-------//
    // reset //
    //-------//
    @Override
    public void reset ()
    {
        weakIndex.reset();
        originals.clear();
    }

    //-------------//
    // setEntities //
    //-------------//
    public void setEntities (ArrayList<Glyph> glyphs)
    {
        for (Glyph glyph : glyphs) {
            WeakGlyph weak = new WeakGlyph(glyph);
            weakIndex.insert(weak);
            originals.putIfAbsent(weak, weak);
        }
    }

    //------------------//
    // setEntityService //
    //------------------//
    @Override
    public void setEntityService (EntityService<Glyph> entityService)
    {
        this.glyphService = entityService;
        entityService.connect();
    }

    //-----------//
    // setLastId //
    //-----------//
    @Override
    public void setLastId (int lastId)
    {
        weakIndex.setLastId(lastId);
    }

    //-----------------//
    // privateRegister //
    //-----------------//
    /**
     * NOTA: This method is meant to be called <b>ONLY</b> from
     * {@link #registerOriginal(omr.glyph.Glyph)} in this class.
     *
     * @param glyph the glyph to register in glyphIndex
     * @return the glyph ID
     */
    private int privateRegister (Glyph glyph)
    {
        int id = glyph.getId();

        if (id == 0) {
            WeakGlyph weak = new WeakGlyph(glyph);

            // Register in index
            id = weakIndex.register(weak);

            glyph.setIndex(this);
        }

        return id;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.String vipGlyphs = new Constant.String(
                "",
                "(Debug) Comma-separated values of VIP glyphs IDs");
    }

    //----------------//
    // WeakGlyphIndex //
    //----------------//
    private static class WeakGlyphIndex
            extends BasicIndex<WeakGlyph>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void insert (WeakGlyph weak)
        {
            super.insert(weak);
        }

        @Override
        protected boolean isValid (WeakGlyph weak)
        {
            return (weak != null) && (weak.get() != null);
        }

        void setIdGenerator (AtomicInteger lastId)
        {
            this.lastId = lastId;
        }
    }

    //------------------//
    // SkippingIterator //
    //------------------//
    /**
     * This iterator skips the weak references that are no longer valid.
     */
    private class SkippingIterator
            implements Iterator<Glyph>
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Iterator<WeakGlyph> weakIt; // Underlying iterator on all weak references

        private Glyph nextGlyph = null; // Concrete glyph to be returned by next()

        //~ Constructors ---------------------------------------------------------------------------
        public SkippingIterator (Iterator<WeakGlyph> weakIt)
        {
            this.weakIt = weakIt;
            nextGlyph = findNextGlyph();
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public boolean hasNext ()
        {
            return nextGlyph != null;
        }

        @Override
        public Glyph next ()
        {
            if (nextGlyph == null) {
                throw new NoSuchElementException();
            }

            Glyph glyph = nextGlyph;
            nextGlyph = findNextGlyph();

            return glyph;
        }

        @Override
        public void remove ()
        {
            throw new UnsupportedOperationException();
        }

        private Glyph findNextGlyph ()
        {
            while (weakIt.hasNext()) {
                WeakGlyph weak = weakIt.next();
                Glyph glyph = weak.get();

                if (glyph != null) {
                    return glyph;
                }
            }

            return null;
        }
    }
}
