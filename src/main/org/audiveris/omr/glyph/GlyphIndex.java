//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      G l y p h I n d e x                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
import org.audiveris.omr.glyph.ui.GlyphService;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.util.BasicIndex;
import org.audiveris.omr.util.ClassUtil;
import org.audiveris.omr.util.Entities;
import org.audiveris.omr.util.EntityIndex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code GlyphIndex} implements an index of (weak references to) Glyph instances.
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

    private static final Logger logger = LoggerFactory.getLogger(GlyphIndex.class);

    //~ Instance fields ----------------------------------------------------------------------------
    // Persistent data
    //----------------
    /**
     * See {@link #getEntities()} and {@link #setEntities(java.util.ArrayList)} methods
     * which are called by private methods
     * Sheet#getGlyphIndexContent() and Sheet#setGlyphIndexContent()
     * triggered by JAXB (un)marshalling.
     */
    //
    // Transient data
    //---------------
    //
    /** Underlying index to weak glyphs. */
    private final WeakGlyphIndex weakIndex = new WeakGlyphIndex();

    /** Collection of original glyph instances, non sorted. */
    private final ConcurrentHashMap<WeakGlyph, WeakGlyph> originals = new ConcurrentHashMap<>();

    /** Selection service, if any. */
    private GlyphService glyphService;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code GlyphIndex} object.
     */
    public GlyphIndex ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------------//
    // getContainedEntities //
    //----------------------//
    @Override
    public List<Glyph> getContainedEntities (Rectangle rectangle)
    {
        return Entities.containedEntities(iterator(), rectangle);
    }

    //-----------------------//
    // getContainingEntities //
    //-----------------------//
    @Override
    public List<Glyph> getContainingEntities (Point point)
    {
        return Entities.containingEntities(iterator(), point);
    }

    //-------------//
    // getEntities //
    //-------------//
    @Override
    public ArrayList<Glyph> getEntities ()
    {
        Collection<WeakGlyph> weaks = weakIndex.getEntities();
        ArrayList<Glyph> glyphs = new ArrayList<>();

        for (WeakGlyph weak : weaks) {
            final Glyph glyph = weak.get();

            if (glyph != null) {
                glyphs.add(glyph);
            }
        }

        return glyphs;
    }

    //-------------//
    // setEntities //
    //-------------//
    /**
     * Populate the weak index.
     *
     * @param glyphs populating glyphs
     */
    @Override
    public void setEntities (Collection<Glyph> glyphs)
    {
        for (Glyph glyph : glyphs) {
            WeakGlyph weak = new WeakGlyph(glyph);
            weakIndex.insert(weak);
            originals.putIfAbsent(weak, weak);
        }
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
    public GlyphService getEntityService ()
    {
        return glyphService;
    }

    //------------------//
    // setEntityService //
    //------------------//
    @Override
    public void setEntityService (EntityService<Glyph> entityService)
    {
        this.glyphService = (GlyphService) entityService;
        entityService.connect();
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

    //------------------------//
    // getIntersectedEntities //
    //------------------------//
    @Override
    public List<Glyph> getIntersectedEntities (Rectangle rectangle)
    {
        return Entities.intersectedEntities(iterator(), rectangle);
    }

    @Override
    public int getLastId ()
    {
        return weakIndex.getLastId();
    }

    //-----------//
    // setLastId //
    //-----------//
    @Override
    public void setLastId (int lastId)
    {
        weakIndex.setLastId(lastId);
    }

    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return "glyphIndex";
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

        return list.get(list.size() - 1); // Last one
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
        List<Glyph> list = (List<Glyph>) glyphService.getSelection(EntityListEvent.class);

        if (list != null) {
            return list;
        } else {
            return Collections.emptyList();
        }
    }

    //----------------//
    // initTransients //
    //----------------//
    /**
     * Initialize the transient members of the class.
     *
     * @param sheet containing sheet
     */
    public final void initTransients (Sheet sheet)
    {
        // ID generator
        weakIndex.setIdGenerator(sheet.getPersistentIdGenerator());

        // Declared VIP IDs?
        final String vipIds = constants.vipGlyphs.getValue();

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
            SelectionService locationService = sheet.getLocationService();
            setEntityService(new GlyphService(this, locationService));
        }
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

    //---------//
    // publish //
    //---------//
    /**
     * Convenient debug UI method to publish and focus on a glyph.
     *
     * @param glyph the provided glyph
     */
    public void publish (Glyph glyph)
    {
        publish(glyph, SelectionHint.ENTITY_INIT);
    }

    //---------//
    // publish //
    //---------//
    /**
     * Convenient debug UI method to publish and focus on a glyph.
     *
     * @param glyph the provided glyph
     * @param hint  selection hint (ENTITY_INIT or ENTITY_TRANSIENT)
     */
    public void publish (Glyph glyph,
                         SelectionHint hint)
    {
        if (glyphService != null) {
            final EntityListEvent event = new EntityListEvent<>(
                    this,
                    hint,
                    MouseMovement.PRESSING,
                    glyph);

            SwingUtilities.invokeLater(() -> glyphService.publish(event));
        }
    }

    //----------//
    // register //
    //----------//
    /**
     * This public method <b>must not be called</b> on GlyphIndex.
     * Use {@link #registerOriginal(org.audiveris.omr.glyph.Glyph)} instead.
     *
     * @param glyph the glyph to process
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return ClassUtil.nameOf(this);
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
    private static class Constants
            extends ConstantSet
    {

        private final Constant.String vipGlyphs = new Constant.String(
                "",
                "(Debug) Comma-separated values of VIP glyphs IDs");
    }

    //------------------//
    // SkippingIterator //
    //------------------//
    /**
     * This iterator skips the weak references that are no longer valid.
     */
    private static class SkippingIterator
            implements Iterator<Glyph>
    {

        private final Iterator<WeakGlyph> weakIt; // Underlying iterator on all weak references

        private Glyph nextGlyph = null; // Concrete glyph to be returned by next()

        SkippingIterator (Iterator<WeakGlyph> weakIt)
        {
            this.weakIt = weakIt;
            nextGlyph = findNextGlyph();
        }

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

    //----------------//
    // WeakGlyphIndex //
    //----------------//
    private static class WeakGlyphIndex
            extends BasicIndex<WeakGlyph>
    {

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
}
