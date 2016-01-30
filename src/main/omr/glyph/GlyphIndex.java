//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      G l y p h I n d e x                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.OMR;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Symbol.Group;
import omr.glyph.ui.GlyphService;

import omr.sheet.Sheet;

import omr.ui.selection.EntityListEvent;
import omr.ui.selection.EntityService;
import omr.ui.selection.GroupEvent;
import omr.ui.selection.IdEvent;
import omr.ui.selection.LocationEvent;
import omr.ui.selection.MouseMovement;
import omr.ui.selection.SelectionHint;
import omr.ui.selection.SelectionService;

import omr.util.BasicIndex;
import omr.util.EntityIndex;
import omr.util.IdUtil;
import omr.util.IntUtil;

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

import javax.swing.SwingUtilities;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code GlyphIndex} implements an index of (weak references to) Glyph instances.
 * <p>
 * TODO: investigate whether the notion of Group could be dropped.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
@XmlType(propOrder = {
    "prefix", "lastIdValue", "entities"}
)
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
     * {@link #getPrefix()}
     * {@link #getLastIdValue()}
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
        if (OMR.getGui() != null) {
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
    @XmlElement(name = "entities")
    @XmlJavaTypeAdapter(Adapter.class)
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
    public Glyph getEntity (String id)
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
    public String getIdAfter (String id)
    {
        return weakIndex.getIdAfter(id);
    }

    @Override
    public String getIdBefore (String id)
    {
        return weakIndex.getIdBefore(id);
    }

    @Override
    public Integer getIdValueAfter (Integer idValue)
    {
        return weakIndex.getIdValueAfter(idValue);
    }

    @Override
    public Integer getIdValueBefore (Integer idValue)
    {
        return weakIndex.getIdValueBefore(idValue);
    }

    @Override
    public String getLastId ()
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

    //-----------//
    // getPrefix //
    //-----------//
    /** Prefix for IDs. */
    @Override
    @XmlAttribute(name = "prefix")
    public String getPrefix ()
    {
        return weakIndex.getPrefix();
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
    public boolean isVipId (String id)
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

    //
    //    //----------//
    //    // iterator //
    //    //----------//
    //    public Iterator<Glyph> iterator (Group group)
    //    {
    //        return new SkippingIterator(byGroup.get(group).iterator());
    //    }
    //
    //    //----------//
    //    // iterator //
    //    //----------//
    //    /**
    //     * Iterator on existing glyphs which exhibit any of the required groups.
    //     * @param groups the required groups
    //     * @return the iterator
    //     */
    //    public Iterator<Glyph> iteratorAny (EnumSet<Group> groups)
    //    {
    //        return new SkippingIterator(byGroup.get(group).iterator());
    //    }
    //
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
                            new EntityListEvent(
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
    @Override
    public String register (Glyph glyph)
    {
        String id = glyph.getId();

        if (id == null) {
            WeakGlyph weak = new WeakGlyph(glyph);

            // Register in index
            id = weakIndex.register(weak);

            glyph.setIndex(this);
        }

        return id;
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
            register(glyph);

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

    //------------------//
    // setEntityService //
    //------------------//
    @Override
    public void setEntityService (EntityService<Glyph> entityService)
    {
        this.glyphService = entityService;
        entityService.connect();
    }

    @Override
    public void setLastId (String lastId)
    {
        weakIndex.setLastId(lastId);
    }

    //----------------//
    // getLastIdValue //
    //----------------//
    @SuppressWarnings("unchecked")
    @XmlAttribute(name = "last-id-value")
    private int getLastIdValue ()
    {
        return IdUtil.getIntValue(weakIndex.getLastId());
    }

    //-------------//
    // setEntities //
    //-------------//
    @SuppressWarnings("unchecked")
    private void setEntities (ArrayList<Glyph> glyphs)
    {
        for (Glyph glyph : glyphs) {
            WeakGlyph weak = new WeakGlyph(glyph);
            weakIndex.insert(weak);
            originals.putIfAbsent(weak, weak);
        }
    }

    //----------------//
    // setLastIdValue //
    //----------------//
    @SuppressWarnings("unchecked")
    private void setLastIdValue (int value)
    {
        weakIndex.setLastId(getPrefix() + value);
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

    //---------//
    // Adapter //
    //---------//
    private static class Adapter
            extends XmlAdapter<GlyphList, ArrayList<Glyph>>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public GlyphList marshal (ArrayList<Glyph> glyphs)
                throws Exception
        {
            return new GlyphList(glyphs);
        }

        @Override
        public ArrayList<Glyph> unmarshal (GlyphList list)
                throws Exception
        {
            return list.glyphs;
        }
    }

    //-----------//
    // GlyphList //
    //-----------//
    private static class GlyphList
    {
        //~ Instance fields ------------------------------------------------------------------------

        @XmlElement(name = "glyph")
        public ArrayList<Glyph> glyphs;

        //~ Constructors ---------------------------------------------------------------------------
        public GlyphList ()
        {
        }

        public GlyphList (ArrayList<Glyph> glyphs)
        {
            this.glyphs = glyphs;
        }
    }

    //----------------//
    // WeakGlyphIndex //
    //----------------//
    private static class WeakGlyphIndex
            extends BasicIndex<WeakGlyph>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public WeakGlyphIndex ()
        {
            super("G");
        }

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
        public void remove()
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
