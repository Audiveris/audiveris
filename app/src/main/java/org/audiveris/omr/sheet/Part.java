//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            P a r t                                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.score.LogicalPart;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.score.PageRef;
import org.audiveris.omr.score.PartRef;
import org.audiveris.omr.score.Score;
import org.audiveris.omr.score.StaffConfig;
import org.audiveris.omr.score.StaffPosition;
import org.audiveris.omr.score.SystemRef;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.KeyInter;
import org.audiveris.omr.sig.inter.LyricLineInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.text.FontInfo;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.VerticalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>Part</code> is the <b>physical</b> gathering of {@link Staff} instances
 * within an instance of {@link SystemInfo}.
 * <p>
 * It loosely corresponds to a single instrument, typical examples are a singer (1 staff), a piano
 * (2 staves) and an organ (3 staves).
 * <p>
 * Among these staves, we can find (standard) 5-line staves, one-line (percussion) staves or
 * 4-line/6-line tablatures.
 * <p>
 * Since the instrument usually persists from one system to the next, we can define the notion of
 * {@link LogicalPart}.
 * <p>
 * Generally, such LogicalPart corresponds to a separate (physical) Part instance in each
 * system but not always. For example, a singer part may appear only after one or several systems
 * played by the piano part.
 * <p>
 * We assume that the configuration of staves within the physical Part instances of the same logical
 * LogicalPart do not vary (in number of staves, in number of lines in staves or in relative
 * positions of staves within the part).
 * <p>
 * The part, as a whole, may appear (or disappear) from one system to the next.
 * <p>
 * During export to MusicXML, dummy parts (and their contained dummy staves and measures) can be
 * virtually inserted <i>on-the-fly</i> into the structure of page/system/part/measure/staff
 * to ease the handling of logical parts along the pages and score.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class Part
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Part.class);

    /** For comparing Part instances according to their id. */
    public static final Comparator<Part> byId = (p1,
                                                 p2) -> Integer.compare(
                                                         Math.abs(p1.getId()),
                                                         Math.abs(p2.getId()));

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /**
     * Id of this part, initially set as the 1-based rank within the system,
     * but later set as the ID value of its related logical part.
     * <p>
     * <b>BEWARE</b>, this ID is not to be used as an index!
     *
     * @see #getIndex()
     */
    @XmlAttribute(name = "id")
    private int id;

    /** This is the text item, if any, that faces this system part in the left margin. */
    @XmlIDREF
    @XmlAttribute(name = "name")
    private SentenceInter name;

    /**
     * This boolean indicates a <i>merged grand staff</i> part, made of 2 staves
     * not separated by a standard gutter.
     * <p>
     * See example:
     * <br>
     * <img alt="merged-grand-staff" src="doc-files/merged_grand_staff.png">
     */
    @XmlAttribute(name = "merged-grand-staff")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean merged;

    /** This is the vertical sequence of staves in this part. */
    @XmlElementRefs(
    {
            @XmlElementRef(type = Staff.class),
            @XmlElementRef(type = OneLineStaff.class),
            @XmlElementRef(type = Tablature.class) })
    private final List<Staff> staves = new ArrayList<>();

    /**
     * This is the part starting barline, if any.
     * <p>
     * (The following barlines are linked to measures)
     */
    @XmlElement(name = "left-barline")
    private PartBarline leftBarline;

    /** This is the horizontal sequence of measures in this part. */
    @XmlElement(name = "measure")
    private final List<Measure> measures = new ArrayList<>();

    /** This is the vertical sequence of lyric lines in this part. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "lyric-lines")
    private List<LyricLineInter> lyrics;

    /** This is the collection of slurs in this part. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "slurs")
    private List<SlurInter> slurs;

    // Transient data
    //---------------

    /** The containing system. */
    @Navigable(false)
    private SystemInfo system;

    /** Indicates a dummy physical part. Used only during MusicXML export. */
    private boolean dummy;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor needed for JAXB.
     */
    private Part ()
    {
    }

    /**
     * Creates a new instance of <code>Part</code>.
     *
     * @param system the containing system
     */
    public Part (SystemInfo system)
    {
        this.system = system;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // addLyric //
    //----------//
    /**
     * Add a lyric line.
     *
     * @param lyric the lyric line to add
     */
    public void addLyric (LyricLineInter lyric)
    {
        if (lyrics == null) {
            lyrics = new ArrayList<>();
        }

        if (!lyrics.contains(lyric)) {
            lyrics.add(lyric);
        }
    }

    //------------//
    // addmeasure //
    //------------//
    /**
     * Insert a measure at specified index.
     *
     * @param index   the specified index
     * @param measure the measure to insert
     */
    public void addMeasure (int index,
                            Measure measure)
    {
        measures.add(index, measure);
    }

    //------------//
    // addmeasure //
    //------------//
    /**
     * Append a measure.
     *
     * @param measure the measure to append
     */
    public void addMeasure (Measure measure)
    {
        measures.add(measure);
    }

    //---------//
    // addSlur //
    //---------//
    /**
     * Add a slur.
     *
     * @param slur the slur to add to this part
     */
    public void addSlur (SlurInter slur)
    {
        if (slurs == null) {
            slurs = new ArrayList<>();
        }

        slurs.add(slur);
    }

    //----------//
    // addStaff //
    //----------//
    /**
     * Append a staff.
     *
     * @param staff the staff to append to this part
     */
    public void addStaff (Staff staff)
    {
        staves.add(staff);
        staff.setPart(this);
    }

    //-------------//
    // afterReload //
    //-------------//
    /**
     * To be called right after unmarshalling.
     */
    public void afterReload ()
    {
        try {
            for (Measure measure : measures) {
                measure.afterReload();
            }

            if (slurs != null) {
                for (SlurInter slur : slurs) {
                    slur.setPart(this);
                }
            }
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
        }
    }

    //-----------------//
    // createDummyPart //
    //-----------------//
    /**
     * Create a dummy system part, parallel to this part in the same system, just to
     * fill needed measures for another logical part.
     * <ul>
     * <li>Clef is taken from first measure of part to be extended (a part, called the refPart,
     * which has the provided id and is found in a following system, or in a preceding system)</li>
     * <li>Key sig is taken from this part</li>
     * <li>Time sig is taken from this part</li>
     * <li>Measures are defined as parallel to this part, and filled with just one whole measure
     * rest</li>
     * </ul>
     * NOTA: This structure is transient, only meant to ease the MusicXML export of the page:
     * <ul>
     * <li>The created inters (clef, key, time, rests) are <b>not</b> inserted in sig, only in the
     * (dummy) measures of the (dummy) part.</li>
     * <li>The created dummy part is <b>not</b> inserted in system list of parts.</li>
     * <li>Similarly, the (dummy) measures of this dummy part are <b>not</b> inserted in
     * MeasureStack instances that play only with real measures.</li>
     * </ul>
     * <p>
     * <img alt="Dummy part creation" src="doc-files/DummyPart.png">
     *
     * @param id the id for the desired dummy part
     * @return the created dummy part, ready to be exported
     */
    public Part createDummyPart (int id)
    {
        final int sn = system.getSheet().getStub().getNumber();
        logger.info("Sheet#{} System#{} {} dummyPart id: {}", sn, system.getId(), this, id);

        // Find some concrete system part for the provided id
        Part refPart = findRefPart(id);
        Part dummyPart = new Part(system);
        dummyPart.setDummy();
        dummyPart.setId(id);

        Measure refMeasure = refPart.getFirstMeasure();

        // Loop on measures
        boolean isFirstMeasure = true;

        for (Measure measure : measures) {
            Measure dummyMeasure = measure.replicate(dummyPart);
            dummyMeasure.setDummy();
            dummyPart.addMeasure(dummyMeasure);
            dummyMeasure.setStack(measure.getStack());

            // Loop on staves found in reference part
            for (int staffIndex = 0; staffIndex < refPart.getStaves().size(); staffIndex++) {
                final Staff dummyStaff;

                if (isFirstMeasure) {
                    // Create dummy Staff
                    dummyStaff = getFirstStaff().replicate();
                    dummyStaff.setDummy();
                    dummyPart.addStaff(dummyStaff);
                    dummyStaff.setSystem(system);

                    // Replicate Clef (from refPart first measure)
                    ClefInter nextClef = refMeasure.getFirstMeasureClef(staffIndex);

                    if (nextClef != null) {
                        ClefInter dummyClef = nextClef.replicate(dummyStaff);
                        dummyMeasure.addInter(dummyClef);
                    }
                } else {
                    dummyStaff = dummyPart.getStaves().get(staffIndex);
                }

                // Replicate Key if any (from current measure in this part)
                KeyInter firstKey = measure.getKey(staffIndex);

                if (firstKey != null) {
                    KeyInter dummyKey = firstKey.replicate(dummyStaff);
                    dummyMeasure.addInter(dummyKey);
                }

                // Replicate Time if any (from current measure in this part)
                AbstractTimeInter ts = measure.getTimeSignature();

                if (ts != null) {
                    AbstractTimeInter dummyTime = ts.replicate(dummyStaff);
                    dummyMeasure.addInter(dummyTime);
                }

                if (!measure.getStack().isMultiRest()) {
                    // Create dummy measure rest (w/ no precise location)
                    dummyMeasure.addDummyMeasureRest(dummyStaff);
                }
            }

            isFirstMeasure = false;
        }

        return dummyPart;
    }

    //-------------//
    // findRefPart //
    //-------------//
    /**
     * Look in following systems, then in previous systems, for a real part with the
     * provided ID.
     * <p>
     * Method extended to search beyond the current page.
     *
     * @param id the desired part ID
     * @return the first real part with this ID, either in following or in preceding systems.
     */
    private Part findRefPart (int id)
    {
        // First look in the following systems in this page and the following ones
        final Score score = system.getPage().getScore();
        Page currentPage = system.getPage();
        SystemInfo otherSystem = system.getFollowingInPage();

        while (true) {
            if (otherSystem != null) {
                Part part = otherSystem.getPartById(id);

                if ((part != null) && !part.isDummy()) {
                    return part;
                }
            } else {
                // Reached end of page
                Page otherPage = score.getFollowingPage(currentPage);

                if (otherPage != null) {
                    currentPage = otherPage;
                    otherSystem = otherPage.getFirstSystem();
                    continue;
                } else {
                    break; // Reached end of score
                }
            }

            otherSystem = otherSystem.getFollowingInPage();
        }

        // Then look in the preceding systems in this page and the preceding ones
        currentPage = system.getPage();
        otherSystem = system.getPrecedingInPage();

        while (true) {
            if (otherSystem != null) {
                Part part = otherSystem.getPartById(id);

                if ((part != null) && !part.isDummy()) {
                    return part;
                }
            } else {
                // Reached start of page
                Page otherPage = score.getPrecedingPage(currentPage);

                if (otherPage != null) {
                    currentPage = otherPage;
                    otherSystem = otherPage.getLastSystem();
                    continue;
                } else {
                    break; // Reached start of score
                }
            }

            otherSystem = otherSystem.getPrecedingInPage();
        }

        logger.warn("{} Cannot find any real system part with id {}", this, id);

        return null;
    }

    //---------------//
    // getAreaBounds //
    //---------------//
    /**
     * Report the bounds of part area.
     *
     * @return the part area bounds
     */
    public Rectangle getAreaBounds ()
    {
        Rectangle bounds = null;

        for (Staff staff : staves) {
            if (bounds == null) {
                bounds = staff.getAreaBounds().getBounds();
            } else {
                bounds.add(staff.getAreaBounds().getBounds());
            }
        }

        return bounds;
    }

    //---------------//
    // getCoreMargin //
    //---------------//
    /**
     * Report the maximum distance of key classes of Part inters, into the provided
     * vertical direction, with respect to the staff border line.
     * <p>
     * Implementation notice: This method can be called at TEXTS step, when head chords are not yet
     * available, therefore we use only proper staff heads and their related stems.
     *
     * @param side desired side
     * @return maximum distance of inter bounds away from proper staff line
     */
    public int getCoreMargin (VerticalSide side)
    {
        int maxDy = 0;

        final LineInfo line = (side == VerticalSide.TOP) ? getFirstStaff().getFirstLine()
                : getLastStaff().getLastLine();
        final SIGraph sig = getSystem().getSig();
        final Set<Inter> inters = new HashSet<>();

        // Staff heads
        final Staff staff = (side == VerticalSide.TOP) ? getFirstStaff() : getLastStaff();
        List<Inter> heads = sig.inters(HeadInter.class);
        heads = Inters.inters(heads, staff);
        inters.addAll(heads);

        // Related stems
        for (Inter h : heads) {
            HeadInter head = (HeadInter) h;

            if (!h.isRemoved()) {
                inters.addAll(head.getStems());
            }
        }

        for (Inter inter : inters) {
            if (!inter.isRemoved()) {
                final Rectangle box = inter.getBounds();
                final int x = inter.getCenter().x;
                final int y = (side == VerticalSide.TOP) ? box.y : ((box.y + box.height) - 1);
                final int yStaff = line.yAt(x);
                final int dy = (side == VerticalSide.TOP) ? (yStaff - y) : (y - yStaff);
                maxDy = Math.max(maxDy, dy);
            }
        }

        return maxDy;
    }

    //-------------------//
    // getCrossSlurLinks //
    //-------------------//
    /**
     * Retrieve possible links between the orphan slurs at the beginning of this part
     * and the orphan slurs at the end of the provided preceding part.
     * <p>
     * Important: Nothing is written in slurs yet, only in links map.
     * <p>
     * This method is called in two contexts:
     * <ol>
     * <li>Within a page: it processes slur connections between systems of the page.
     * <li>Within a score: it processes slur connections between pages of a score.
     * </ol>
     *
     * @param precedingPart the part to connect to, in the preceding system,
     *                      [perhaps the last system of the preceding page]
     * @return the map (slur &rarr; prevSlur) of connections detected
     */
    public Map<SlurInter, SlurInter> getCrossSlurLinks (Part precedingPart)
    {
        Objects.requireNonNull(precedingPart, "Null part to connect Slurs with");

        // Links: Slur -> prevSlur
        Map<SlurInter, SlurInter> links = new LinkedHashMap<>();

        // Orphans slurs at the beginning of the current system part
        List<SlurInter> orphans = getSlurs(SlurInter.isBeginningOrphan);
        Collections.sort(orphans, SlurInter.verticalComparator);

        List<SlurInter> precedingOrphans = precedingPart.getSlurs(SlurInter.isEndingOrphan);
        Collections.sort(precedingOrphans, SlurInter.verticalComparator);

        // Connect the orphans as much as possible
        SlurLoop:
        for (SlurInter slur : orphans) {
            for (SlurInter prevSlur : precedingOrphans) {
                if (slur.isVip() || prevSlur.isVip()) {
                    logger.info("VIP cross test prevSlur:{} slur:{}", prevSlur, slur);
                }

                // Check left side of slur
                if ((slur.getExtension(LEFT) != null) || (links.get(slur) != null)) {
                    continue SlurLoop;
                }

                // Check right side of previous slur
                if ((prevSlur.getExtension(RIGHT) == null) && !links.containsValue(prevSlur)) {
                    // Check pitches compatibility
                    if (slur.canExtend(prevSlur)) {
                        links.put(slur, prevSlur);

                        continue SlurLoop;
                    }
                }
            }
        }

        return links;
    }

    //-----------------//
    // getFirstMeasure //
    //-----------------//
    /**
     * Report the first measure in this system part.
     *
     * @return the first measure entity
     */
    public Measure getFirstMeasure ()
    {
        if (measures.isEmpty()) {
            return null;
        }

        return measures.get(0);
    }

    //---------------//
    // getFirstStaff //
    //---------------//
    /**
     * Report the first staff in the part.
     *
     * @return the first staff
     */
    public Staff getFirstStaff ()
    {
        return staves.get(0);
    }

    //--------------------//
    // getFollowingInPage //
    //--------------------//
    /**
     * Report the corresponding part (if any) in the following system in current page.
     *
     * @return the corresponding part, or null
     */
    public Part getFollowingInPage ()
    {
        SystemInfo nextSystem = getSystem().getFollowingInPage();

        if (nextSystem == null) {
            return null;
        }

        return nextSystem.getPartById(id);
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the current id of this part.
     * <p>
     * After parts collation, this is the related logical id.
     *
     * @return the part id, not to be confused with part index.
     * @see #getIndex()
     */
    public int getId ()
    {
        return id;
    }

    //----------//
    // getIndex //
    //----------//
    /**
     * Report the index of this part within the containing system.
     *
     * @return index within system, not to be confused with id.
     * @see #getId()
     */
    public int getIndex ()
    {
        return system.getParts().indexOf(this);
    }

    //----------------//
    // getLastMeasure //
    //----------------//
    /**
     * Report the last measure in this part.
     *
     * @return the last measure entity
     */
    public Measure getLastMeasure ()
    {
        if (measures.isEmpty()) {
            return null;
        }

        return measures.get(measures.size() - 1);
    }

    //--------------//
    // getLastStaff //
    //--------------//
    /**
     * @return the lastStaff
     */
    public Staff getLastStaff ()
    {
        return staves.get(staves.size() - 1);
    }

    //--------------------//
    // getLeftPartBarline //
    //--------------------//
    /**
     * Get the PartBarline that starts the part.
     *
     * @return the starting PartBarline (which may be null)
     */
    public PartBarline getLeftPartBarline ()
    {
        return leftBarline;
    }

    //----------------//
    // getLogicalPart //
    //----------------//
    /**
     * Report the LogicalPart this (physical) part implements.
     *
     * @return the logical part.
     */
    public LogicalPart getLogicalPart ()
    {
        final Page page = system.getPage();
        if (page == null)
            return null;

        final Score score = page.getScore();
        if (score == null)
            return null;

        return score.getLogicalPartById(id);
    }

    //-----------//
    // getLyrics //
    //-----------//
    /**
     * Report the sequence of lyric lines in this part.
     *
     * @return list of lyrics, perhaps empty
     */
    public List<LyricLineInter> getLyrics ()
    {
        return (lyrics != null) ? Collections.unmodifiableList(lyrics) : Collections.emptyList();
    }

    //--------------//
    // getMeasureAt //
    //--------------//
    /**
     * Report the measure that contains a given point (assumed to be in the containing
     * part).
     *
     * @param point coordinates of the given point
     * @return the containing measure
     */
    public Measure getMeasureAt (Point2D point)
    {
        return getMeasureAt(point, getStaffJustAbove(point));
    }

    //--------------//
    // getMeasureAt //
    //--------------//
    /**
     * Report the measure that contains a given point (assumed to be in the containing
     * part).
     *
     * @param point coordinates of the given point
     * @param staff related staff
     * @return the containing measure
     */
    public Measure getMeasureAt (Point2D point,
                                 Staff staff)
    {
        if (staff == null) {
            return null;
        }

        if ((point.getX() >= staff.getAbscissa(LEFT)) && (point.getX() <= staff.getAbscissa(
                RIGHT))) {
            for (Measure measure : measures) {
                PartBarline barline = measure.getRightPartBarline();

                if ((barline == null) || (point.getX() <= barline.getRightX(this, staff))) {
                    return measure;
                }
            }
        }

        return null;
    }

    //-------------//
    // getMeasures //
    //-------------//
    /**
     * Report the collection of measures.
     *
     * @return an unmodifiable view on measures
     */
    public List<Measure> getMeasures ()
    {
        return Collections.unmodifiableList(measures);
    }

    //---------//
    // getName //
    //---------//
    /**
     * @return the name
     */
    public String getName ()
    {
        return (name != null && !name.isRemoved()) ? name.getValue() : null;
    }

    //---------------//
    // getNextInPage //
    //---------------//
    /**
     * Report the corresponding part (if any) in the next system in current page.
     *
     * @return the corresponding part, or null
     */
    public Part getNextInPage ()
    {
        SystemInfo nextSystem = getSystem().getNextInPage();

        if (nextSystem == null) {
            return null;
        }

        return nextSystem.getPartById(id);
    }

    //--------//
    // getPid //
    //--------//
    /**
     * Report a pid string, using format "Pn", where 'n' is the id
     *
     * @return the Pid
     */
    public String getPid ()
    {
        return "P" + id;
    }

    //--------------------//
    // getPrecedingInPage //
    //--------------------//
    /**
     * Report the corresponding part (if any) in the preceding system in current page.
     *
     * @return the corresponding part, or null
     */
    public Part getPrecedingInPage ()
    {
        SystemInfo prevSystem = getSystem().getPrecedingInPage();

        if (prevSystem == null) {
            return null;
        }

        return prevSystem.getPartById(id);
    }

    //--------//
    // getRef //
    //--------//
    /**
     * Report the corresponding PartRef in sheet stub.
     *
     * @return its PartRef
     */
    public PartRef getRef ()
    {
        final Page page = system.getPage();
        final SheetStub stub = page.getSheet().getStub();

        final PageRef pageRef = stub.getPageRefs().get(page.getId() - 1);
        final List<SystemRef> systemRefs = pageRef.getSystems();

        // For backward compatibility
        if (systemRefs.isEmpty()) {
            return null;
        }

        final SystemRef systemRef = system.getRef();

        return systemRef.getParts().get(getIndex());
    }

    //----------//
    // getSlurs //
    //----------//
    /**
     * Report the collection of slurs for which the provided predicate is true.
     *
     * @param predicate the check to run
     * @return the collection of selected slurs, which may be empty
     */
    public List<SlurInter> getSlurs (Predicate<SlurInter> predicate)
    {
        List<SlurInter> selectedSlurs = new ArrayList<>();

        if (slurs != null) {
            for (SlurInter slur : slurs) {
                if (predicate.test(slur)) {
                    selectedSlurs.add(slur);
                }
            }
        }

        return selectedSlurs;
    }

    //-----------------//
    // getStaffConfigs //
    //-----------------//
    /**
     * Report the staves configurations.
     *
     * @return list of staff configuration
     */
    public List<StaffConfig> getStaffConfigs ()
    {
        final List<StaffConfig> staffConfigs = new ArrayList<>(staves.size());

        for (Staff staff : staves) {
            staffConfigs.add(staff.getStaffConfig());
        }

        return staffConfigs;
    }

    //---------------//
    // getStaffCount //
    //---------------//
    public int getStaffCount ()
    {
        return staves.size();
    }

    //-------------------//
    // getStaffJustAbove //
    //-------------------//
    /**
     * Report the staff which is at or above the provided point
     *
     * @param point the provided point
     * @return the staff just above
     */
    public Staff getStaffJustAbove (Point2D point)
    {
        List<Staff> relevants = StaffManager.getStavesOf(point, staves);

        if (!relevants.isEmpty()) {
            return relevants.get(0);
        }

        return null;
    }

    //-------------------//
    // getStaffJustBelow //
    //-------------------//
    /**
     * Report the staff which is at or below the provided point
     *
     * @param point the provided point
     * @return the staff just below
     */
    public Staff getStaffJustBelow (Point point)
    {
        List<Staff> relevants = StaffManager.getStavesOf(point, staves);

        return relevants.get(relevants.size() - 1);
    }

    //------------------//
    // getStaffPosition //
    //------------------//
    /**
     * Report the vertical position of the provided point with respect to the part staves.
     *
     * @param point the point whose ordinate is to be checked
     * @return the StaffPosition value
     */
    public StaffPosition getStaffPosition (Point2D point)
    {
        if (point.getY() < getFirstStaff().getFirstLine().yAt(point.getX())) {
            return StaffPosition.ABOVE_STAVES;
        }

        if (point.getY() > getLastStaff().getLastLine().yAt(point.getX())) {
            return StaffPosition.BELOW_STAVES;
        }

        return StaffPosition.WITHIN_STAVES;
    }

    //-----------//
    // getStaves //
    //-----------//
    /**
     * Report the sequence of staves in this part.
     *
     * @return list of staves
     */
    public List<Staff> getStaves ()
    {
        return staves;
    }

    //-----------//
    // getSystem //
    //-----------//
    /**
     * Report the containing system
     *
     * @return containing system
     */
    public SystemInfo getSystem ()
    {
        return system;
    }

    //---------------//
    // getTablatures //
    //---------------//
    /**
     * Report the tablatures in this part, if any.
     *
     * @return list of tablatures, perhaps empty
     */
    public List<Staff> getTablatures ()
    {
        final List<Staff> tablatures = new ArrayList<>();

        for (Staff staff : staves) {
            if (staff.isTablature()) {
                tablatures.add(staff);
            }
        }

        return tablatures;
    }

    //------------//
    // isDrumPart //
    //------------//
    /**
     * Report whether this part is a (5-line unpitched) percussion part.
     *
     * @return true if so, false if not (or if not certain, e.g. if no first measure clef is found)
     */
    public boolean isDrumPart ()
    {
        final Measure firstMeasure = getFirstMeasure();

        if (firstMeasure != null) {
            final ClefInter staffClef = firstMeasure.getFirstMeasureClef(0);

            if (staffClef != null) {
                return staffClef.getShape() == Shape.PERCUSSION_CLEF;
            }
        }

        return false;
    }

    //---------//
    // isDummy //
    //---------//
    /**
     * Tell whether this part is a dummy (that lives only for the duration of an export)
     *
     * @return true if so
     */
    public boolean isDummy ()
    {
        return dummy;
    }

    //----------//
    // isMerged //
    //----------//
    /**
     * Report whether this part represents a pair of staves with no clear separation.
     *
     * @return the merged attribute
     */
    public boolean isMerged ()
    {
        return merged;
    }

    //----------------//
    // mergeWithBelow //
    //----------------//
    /**
     * Merge this part with the part located just below in the same system.
     * <p>
     * This extension is performed when a missing brace is manually inserted in front of these
     * 2 parts to merge them into a single part.
     * <p>
     * Not to be mistaken with the notion of "merged-grand-staff" which is a specific layout with
     * 2 staves that physically share the common C4 step.
     * <p>
     * Impact on system parts, part staves, stack measures, measures extended, pointers to part
     *
     * @param below the part just below this one
     * @see #splitBefore(Staff)
     */
    public void mergeWithBelow (Part below)
    {
        logger.info("Merging {} with below {}", this, below);

        for (int im = 0; im < measures.size(); im++) {
            final Measure measure = measures.get(im);
            final Measure measureBelow = below.getMeasures().get(im);
            measureBelow.switchItemsPart(below);
            measure.mergeWithBelow(measureBelow);
        }

        if (below.leftBarline != null) {
            if (leftBarline == null) {
                leftBarline = below.leftBarline;
            } else {
                leftBarline.mergeWithBelow(below.leftBarline);
            }
        }

        for (Staff staff : below.getStaves()) {
            addStaff(staff);
        }
        final PartRef ref = getRef();
        ref.computeStaffConfigs(staves);

        final SystemRef systemRef = system.getRef();
        final PartRef belowRef = below.getRef();
        system.removePart(below);
        systemRef.getParts().remove(belowRef);

        system.getSheet().getStub().setModified(true);
    }

    //-----------------//
    // purgeContainers //
    //-----------------//
    /**
     * Update the internal collection of lyrics, by removing the deleted ones.
     */
    public void purgeContainers ()
    {
        // Lyrics
        if (lyrics != null) {
            for (Iterator<LyricLineInter> it = lyrics.iterator(); it.hasNext();) {
                LyricLineInter lyric = it.next();

                if (lyric.isRemoved()) {
                    it.remove();
                }
            }
        }
    }

    //-------------//
    // removeLyric //
    //-------------//
    /**
     * Remove a lyric line.
     *
     * @param lyric the lyric line to remove
     */
    public void removeLyric (LyricLineInter lyric)
    {
        if (lyrics != null) {
            lyrics.remove(lyric);

            if (lyrics.isEmpty()) {
                lyrics = null;
            }
        }
    }

    //---------------//
    // removeMeasure //
    //---------------//
    /**
     * Remove a measure.
     *
     * @param measure the measure to remove from this part
     */
    public void removeMeasure (Measure measure)
    {
        measures.remove(measure);
    }

    //------------//
    // removeName //
    //------------//
    /**
     * Remove the provided PartName sentence.
     *
     * @param sentence the provided part name sentence
     * @return true if actually removed
     */
    public boolean removeName (SentenceInter sentence)
    {
        if ((name != null) && (name == sentence)) {
            setName((SentenceInter) null);
            return true;
        }

        return false;
    }

    //------------//
    // removeSLur //
    //------------//
    /**
     * Remove a slur from part collection.
     *
     * @param slur the slur to remove
     * @return true if actually removed, false if not found
     */
    public boolean removeSlur (SlurInter slur)
    {
        boolean result = false;

        if (slurs != null) {
            result = slurs.remove(slur);

            if (slurs.isEmpty()) {
                slurs = null;
            }
        }

        return result;
    }

    //----------//
    // setDummy //
    //----------//
    /**
     * Assign this part as being dummy.
     */
    public void setDummy ()
    {
        dummy = Boolean.TRUE;
    }

    //-------//
    // setId //
    //-------//
    /**
     * Set the part id.
     *
     * @param id the new id value
     */
    public void setId (int id)
    {
        if (this.id != id) {
            this.id = id;

            if (!isDummy()) {
                getSystem().getSheet().getStub().setModified(true);
            }
        }
    }

    //--------------------//
    // setLeftPartBarline //
    //--------------------//
    /**
     * Set the PartBarline that starts the part.
     *
     * @param leftBarline the starting PartBarline
     */
    public void setLeftPartBarline (PartBarline leftBarline)
    {
        this.leftBarline = leftBarline;
    }

    //----------------//
    // setLogicalPart //
    //----------------//
    /**
     * Modify the related logical part, using a specific logical id.
     *
     * @param logicalId   the new logical Id
     * @param logicalPart the provided logical part (perhaps with old Id)
     */
    public void setLogicalPart (Integer logicalId,
                                LogicalPart logicalPart)
    {
        if (logicalId != null) {
            // Update part id
            setId(logicalId);

            // Update part name accordingly
            final Boolean isFirst = system.isFirstInScore();
            if (isFirst != null && isFirst) {
                setName(logicalPart.getName());
            } else {
                final String abbrev = logicalPart.getAbbreviation();
                setName(abbrev != null ? abbrev : logicalPart.getName());
            }
        }
    }

    //----------------//
    // setLogicalPart //
    //----------------//
    /**
     * Modify the related logical part.
     *
     * @param logicalPart the provided logical part
     * @param manual      true for a manual operation
     */
    public void setLogicalPart (LogicalPart logicalPart,
                                boolean manual)
    {
        final int logicalId = logicalPart.getId();
        setLogicalPart(logicalId, logicalPart);

        // Update partRef as well
        final PartRef partRef = getRef();
        if (partRef != null) {
            partRef.setLogicalId(logicalId);
            partRef.setManual(manual);
        }
    }

    //-----------//
    // setMerged //
    //-----------//
    /**
     * Set this part a being a "merged" part like a kind of 11-line grand staff.
     *
     * @param merged the merged value to set
     */
    public void setMerged (boolean merged)
    {
        this.merged = merged;
    }

    //---------//
    // setName //
    //---------//
    /**
     * Assign a name item to this part.
     *
     * @param name the name item to set
     * @see #setName(String)
     */
    public void setName (SentenceInter name)
    {
        this.name = name;

        if (name != null && !name.isRemoved()) {
            // Normalize font info for part name
            for (Inter inter : name.getMembers()) {
                final WordInter word = (WordInter) inter;
                final FontInfo old = word.getFontInfo();
                word.setFontInfo(new FontInfo(old.pointsize, old.fontName));
            }

            final FontInfo old = name.getMeanFont();
            name.setMeanFont(new FontInfo(old.pointsize, old.fontName));

        }

        // Update PartRef as well
        final PartRef partRef = getRef();
        if (partRef != null) {
            partRef.setName((name != null && !name.isRemoved()) ? name.getValue() : null);
        }
    }

    //---------//
    // setName //
    //---------//
    /**
     * Assign a name value to this part.
     * <p>
     * This is performed by modifying current name sentence, if any, to reflect provided nameValue.
     *
     * @param nameValue the name value to set
     * @see #setName(SentenceInter)
     */
    public void setName (String nameValue)
    {
        if (name != null) {
            final List<Inter> members = name.getMembers();
            if (members.size() == 1) {
                final WordInter word = (WordInter) members.get(0);
                word.setValue(nameValue);
            } else {
                // Try to map word for word
                final String[] tokens = nameValue.split(" ");
                for (int i = 0; i < members.size(); i++) {
                    final WordInter word = (WordInter) members.get(i);
                    if (i < tokens.length) {
                        word.setValue(tokens[i]);
                    }
                }
            }

            // Update PartRef as well
            final PartRef partRef = getRef();
            if (partRef != null) {
                partRef.setName(nameValue);
            }

            getSystem().getSheet().getStub().setModified(true);
        }
    }

    //-----------//
    // setSystem //
    //-----------//
    /**
     * Assign the containing system.
     *
     * @param system the containing system
     */
    public void setSystem (SystemInfo system)
    {
        this.system = system;
    }

    //----------------//
    // sortLyricLines //
    //----------------//
    /**
     * Keep the lyrics sorted by vertical order in part, and numbered according to
     * related staff.
     */
    public void sortLyricLines ()
    {
        if (lyrics != null) {
            Collections.sort(lyrics, SentenceInter.byOrdinate);

            // Assign sequential number to lyric line above staff and below staff.
            int lyricNumber = 0;
            Staff lastStaff = null;
            boolean lastIsAbove = true;

            for (LyricLineInter line : lyrics) {
                final Staff staff = line.getStaff();
                final boolean isAbove = staff.isPointAbove(line.getCenter());

                if ((staff != lastStaff) || (isAbove != lastIsAbove)) {
                    lyricNumber = 0;
                }

                line.setNumber(++lyricNumber);

                lastStaff = staff;
                lastIsAbove = isAbove;
            }
        }
    }

    //-------------//
    // splitBefore //
    //-------------//
    /**
     * Split this part in two sub-parts, just before the provided staff.
     *
     * @param pivotStaff the first staff of second half.
     * @see #mergeWithBelow(Part)
     */
    public void splitBefore (Staff pivotStaff)
    {
        logger.info("Splitting {} before {}", this, pivotStaff);

        final SystemRef systemRef = system.getRef();
        final int staffIndex = staves.indexOf(pivotStaff);
        final Part partBelow = new Part(system);

        for (Staff staff : staves.subList(staffIndex, staves.size())) {
            partBelow.addStaff(staff);
        }

        staves.removeAll(partBelow.staves);

        final PartRef ref = getRef();
        ref.computeStaffConfigs(staves);

        for (int im = 0; im < measures.size(); im++) {
            final Measure measure = measures.get(im);
            measure.splitBefore(pivotStaff, partBelow);
        }

        final int myIndex = getIndex();
        system.addPart(myIndex + 1, partBelow);

        final PartRef belowRef = new PartRef(systemRef, partBelow.staves);
        systemRef.getParts().add(myIndex + 1, belowRef);

        // Part numbering (to be refined)
        int theId = id + 1;

        for (Part part : system.getParts().subList(myIndex + 1, system.getParts().size())) {
            part.setId(theId++);
        }

        system.getSheet().getStub().setModified(true);
    }

    //-------------//
    // swapVoiceId //
    //-------------//
    /**
     * Change the ID of voice for the specified newId
     *
     * @param oldId old voice ID
     * @param newId new ID to be used for this voice in all measures of this system part
     */
    public void swapVoiceId (int oldId,
                             int newId)
    {
        for (Measure measure : measures) {
            for (Voice voice : measure.getVoices()) {
                if (voice.getId() == oldId) {
                    measure.swapVoiceId(voice, newId);

                    break;
                }
            }
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append('#').append(id).append('{');

        if ((name != null) && !name.isRemoved()) {
            sb.append("name:").append(name.getValue());
        }

        if (isDummy()) {
            sb.append(" dummy");
        }

        sb.append(" staves[");
        sb.append(staves.stream().map(s -> "" + s.getId()).collect(Collectors.joining(",")));
        sb.append("]");

        sb.append(" configs:[").append(StaffConfig.toCsvString(getStaffConfigs())).append(']');

        sb.append("}");

        return sb.toString();
    }
}
