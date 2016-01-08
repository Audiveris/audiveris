//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            P a r t                                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.score.LogicalPart;
import omr.score.StaffPosition;

import omr.sheet.rhythm.Measure;
import omr.sheet.rhythm.Voice;

import omr.sig.inter.ClefInter;
import omr.sig.inter.KeyInter;
import omr.sig.inter.LyricLineInter;
import omr.sig.inter.SlurInter;
import omr.sig.inter.TimeInter;

import omr.step.PageStep;

import static omr.util.HorizontalSide.*;

import omr.util.Navigable;
import omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;

/**
 * Class {@code Part} is the <b>physical</b> gathering of {@link Staff} instances in an
 * instance of {@link SystemInfo}.
 * <p>
 * It loosely corresponds to a single instrument, typical examples are a singer (1 staff) or a piano
 * (2 staves).
 * <p>
 * Since the instrument usually persists from one system to the next, we can define the notion of
 * "logical" part, named {@link LogicalPart}.
 * <p>
 * Generally, such LogicalPart corresponds to a separate (physical) Part instance in each
 * system but not always. For example, a singer part may not appear at the very beginning of a
 * score, but only after one or several systems played by the piano part.
 * <p>
 * We assume that the configuration of staves within the physical Part instances of the same logical
 * LogicalPart do not vary (in number of staves or in relative positions of staves within the part).
 * However, the part as a whole may appear (or disappear?) from one system to the next.
 * <p>
 * During step {@link PageStep}, dummy parts (and dummy staves and measures) can be inserted in the
 * concrete structure of page/system/part/measure/staff to ease the handling of logical parts along
 * the pages and score.
 * <p>
 * Before {@link PageStep} is run, a part IDs is the 1-based index within the containing system.
 * After {@link PageStep} is run, part IDs, because of dummy parts, may not be exactly the position
 * number within the containing system. But all the (physical) Parts related to the same (logical)
 * LogicalPart share the same ID.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class Part
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            Part.class);

    /** For comparing Part instances according to their id. */
    public static final Comparator<Part> byId = new Comparator<Part>()
    {
        @Override
        public int compare (Part p1,
                            Part p2)
        {
            return Integer.compare(Math.abs(p1.getId()), Math.abs(p2.getId()));
        }
    };

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Staves in this part. */
    @XmlElement(name = "staff")
    private final List<Staff> staves = new ArrayList<Staff>();

    /** Id of this part within the system, starting at -1 or +1. */
    @XmlAttribute
    private int id;

    /** Name, if any, that faces this system part. */
    @XmlAttribute
    private String name;

    /** Starting barline, if any. (the others are linked to measures) */
    @XmlElement(name = "starting-barline")
    private PartBarline startingBarline;

    /** Measures in this part. */
    @XmlElement(name = "measure")
    private final List<Measure> measures = new ArrayList<Measure>();

    /** Lyric lines in this part. */
    @XmlElement(name = "lyric-line")
    private final List<LyricLineInter> lyrics = new ArrayList<LyricLineInter>();

    /** Slurs in this part. */
    @XmlElement(name = "slur")
    private final List<SlurInter> slurs = new ArrayList<SlurInter>();

    /** Voice IDs in this part. */
    @XmlList
    @XmlElement(name = "voices")
    private List<Integer> voiceIds;

    // Transient data
    //---------------
    //
    /** Flag to indicate this system part is just a placeholder. */
    private boolean dummy;

    /** The containing system. */
    @Navigable(false)
    private SystemInfo system;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new instance of {@code Part}.
     *
     * @param system the containing system
     */
    public Part (SystemInfo system)
    {
        this.system = system;
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private Part ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // addLyric //
    //----------//
    public void addLyric (LyricLineInter lyric)
    {
        lyrics.add(lyric);
    }

    //------------//
    // addmeasure //
    //------------//
    public void addMeasure (Measure measure)
    {
        measures.add(measure);
    }

    //---------//
    // addSlur //
    //---------//
    public void addSlur (SlurInter slur)
    {
        slurs.add(slur);
    }

    //----------//
    // addStaff //
    //----------//
    public void addStaff (Staff staff)
    {
        staves.add(staff);
    }

    //-------------//
    // afterReload //
    //-------------//
    public void afterReload ()
    {
        try {
            // Process staves upfront, so that their notes have their staff assigned.
            // Doing so, measure chords can determine which staves they belong to.
            for (Staff staff : staves) {
                staff.afterReload();
            }

            for (Measure measure : measures) {
                measure.afterReload();
            }
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
        }
    }

    //------------------//
    // connectSlursWith //
    //------------------//
    /**
     * Try to connect the orphan slurs at the beginning of this part
     * with the orphan slurs at the end of the provided preceding part.
     *
     * @param precedingPart the part to connect to, in the preceding system,
     *                      [TODO: or in the last system of the preceding page]
     */
    public void connectSlursWith (Part precedingPart)
    {
        if (precedingPart != null) {
            // Orphans slurs at the beginning of the current system part
            List<SlurInter> orphans = getSlurs(SlurInter.isBeginningOrphan);
            Collections.sort(orphans, SlurInter.verticalComparator);

            List<SlurInter> precedingOrphans = precedingPart.getSlurs(SlurInter.isEndingOrphan);
            Collections.sort(precedingOrphans, SlurInter.verticalComparator);

            // Connect the orphans as much as possible
            SlurLoop:
            for (SlurInter slur : orphans) {
                for (SlurInter prevSlur : precedingOrphans) {
                    if (slur.canExtend(prevSlur)) {
                        slur.connectTo(prevSlur);

                        continue SlurLoop;
                    }
                }

                // No connection for this orphan
                logger.info("Could not left-connect slur#" + slur.getId());
                slur.delete();
            }

            // Check previous orphans
            for (SlurInter prevSlur : precedingOrphans) {
                if (prevSlur.getExtension(RIGHT) == null) {
                    logger.info("Could not right-connect slur#" + prevSlur.getId());
                    prevSlur.delete();
                }
            }
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
     * <li> Similarly, the (dummy) measures of this dummy part are <b>not</b> inserted in
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
        logger.info("S#{} {} createDummyPart for id={}", system.getId(), this, id);

        // Find some concrete system part for the provided id
        Part refPart = findRefPart(id);
        Part dummyPart = new Part(system);

        dummyPart.setId(id);
        dummyPart.setDummy();
        dummyPart.setLogicalPart(refPart.getLogicalPart());

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
                TimeInter ts = measure.getTimeSignature();

                if (ts != null) {
                    TimeInter dummyTime = ts.replicate(dummyStaff);
                    dummyMeasure.addInter(dummyTime);
                }

                // Create dummy Whole rest (w/ no precise location)
                dummyMeasure.addDummyWholeRest(dummyStaff);
            }

            isFirstMeasure = false;
        }

        return dummyPart;
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
     * Report the first staff of the part.
     *
     * @return the first staff
     */
    public Staff getFirstStaff ()
    {
        return staves.get(0);
    }

    //--------------------//
    // getPrecedingInPage //
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
     *
     * @return the part id
     */
    public int getId ()
    {
        return id;
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

    //----------------//
    // getLogicalPart //
    //----------------//
    public LogicalPart getLogicalPart ()
    {
        return system.getPage().getLogicalPartById(id);
    }

    //-----------//
    // getLyrics //
    //-----------//
    public List<LyricLineInter> getLyrics ()
    {
        return lyrics;
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
    public Measure getMeasureAt (Point point)
    {
        Staff staff = getStaffJustAbove(point);

        if (staff == null) {
            return null;
        }

        if ((point.x >= staff.getAbscissa(LEFT)) && (point.x <= staff.getAbscissa(RIGHT))) {
            for (Measure measure : measures) {
                PartBarline barline = measure.getRightBarline();

                if ((barline == null) || (point.x <= barline.getRightX(this, staff))) {
                    return measure;
                }
            }

            //
            //            // Ending (curtesy?) measure with no ending barline
            //            if (!measures.isEmpty()) {
            //                return measures.get(measures.size() - 1);
            //            }
        }

        return null;
    }

    //-------------//
    // getMeasures //
    //-------------//
    /**
     * Report the collection of measures.
     *
     * @return the measure list, which may be empty but not null
     */
    public List<Measure> getMeasures ()
    {
        return measures;
    }

    //---------//
    // getName //
    //---------//
    /**
     * @return the name
     */
    public String getName ()
    {
        return name;
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
        List<SlurInter> selectedSlurs = new ArrayList<SlurInter>();

        for (SlurInter slur : slurs) {
            if (predicate.check(slur)) {
                selectedSlurs.add(slur);
            }
        }

        return selectedSlurs;
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
    public Staff getStaffJustAbove (Point point)
    {
        List<Staff> relevants = StaffManager.getStavesOf(point, staves, null);

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
        List<Staff> relevants = StaffManager.getStavesOf(point, staves, null);

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

    //--------------------//
    // getStartingBarline //
    //--------------------//
    /**
     * Get the barline that starts the part.
     *
     * @return barline the starting bar line (which may be null)
     */
    public PartBarline getStartingBarline ()
    {
        return startingBarline;
    }

    //-----------//
    // getStaves //
    //-----------//
    public List<Staff> getStaves ()
    {
        return staves;
    }

    //-----------//
    // getSystem //
    //-----------//
    public SystemInfo getSystem ()
    {
        return system;
    }

    //-------------//
    // getVoiceIds //
    //-------------//
    /**
     * @return the voice IDs
     */
    public List<Integer> getVoiceIds ()
    {
        return voiceIds;
    }

    //---------//
    // isDummy //
    //---------//
    public boolean isDummy ()
    {
        return dummy;
    }

    //-----------------//
    // purgeContainers //
    //-----------------//
    public void purgeContainers ()
    {
        // Lyrics
        for (Iterator<LyricLineInter> it = lyrics.iterator(); it.hasNext();) {
            LyricLineInter lyric = it.next();

            if (lyric.isDeleted()) {
                it.remove();
            }
        }
    }

    //----------//
    // setDummy //
    //----------//
    public void setDummy ()
    {
        dummy = true;
    }

    //-------//
    // setId //
    //-------//
    /**
     * Set the part id.
     *
     * @param id the id value
     */
    public void setId (int id)
    {
        this.id = id;
    }

    //----------------//
    // setLogicalPart //
    //----------------//
    public void setLogicalPart (LogicalPart logicalPart)
    {
        setId(logicalPart.getId());
    }

    //---------//
    // setName //
    //---------//
    /**
     * @param name the name to set
     */
    public void setName (String name)
    {
        this.name = name;
    }

    //--------------------//
    // setStartingBarline //
    //--------------------//
    /**
     * Set the barline that starts the part.
     *
     * @param startingBarline the starting barline
     */
    public void setStartingBarline (PartBarline startingBarline)
    {
        this.startingBarline = startingBarline;
    }

    //-----------//
    // setSystem //
    //-----------//
    public void setSystem (SystemInfo system)
    {
        this.system = system;
    }

    //-------------//
    // setVoiceIds //
    //-------------//
    /**
     * @param voiceIds the voice IDs to set
     */
    public void setVoiceIds (List<Integer> voiceIds)
    {
        this.voiceIds = voiceIds;
    }

    //-------------//
    // swapVoiceId //
    //-------------//
    /**
     * Change the ID of voice at provided index for the specified newId
     *
     * @param index index of voice in part voiceIds
     * @param newId new ID to be used for this voice in all measures of this system part
     */
    public void swapVoiceId (int index,
                             int newId)
    {
        for (Measure measure : measures) {
            for (Voice voice : measure.getVoices()) {
                int voiceIndex = voiceIds.indexOf(voice.getId());

                if (voiceIndex == index) {
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
        StringBuilder sb = new StringBuilder();
        sb.append("{Part#").append(id);

        if (dummy) {
            sb.append(" dummy");
        }

        sb.append(" staves[");

        boolean first = true;

        for (Staff staff : staves) {
            if (!first) {
                sb.append(",");
            }

            sb.append(staff.getId());

            first = false;
        }

        sb.append("]");

        if (name != null) {
            sb.append(" name:").append(name);
        }

        sb.append("}");

        return sb.toString();
    }

    //-------------//
    // findRefPart //
    //-------------//
    /**
     * Look in following systems, then in previous systems, for a real part with the
     * provided ID.
     *
     * @param id the desired part ID
     * @return the first real part with this ID, either in following or in preceding systems.
     */
    private Part findRefPart (int id)
    {
        // First look in the following systems in the same page
        SystemInfo nextSystem = system;

        while (true) {
            nextSystem = nextSystem.getFollowingInPage();

            if (nextSystem != null) {
                Part part = nextSystem.getPartById(id);

                if ((part != null) && !part.isDummy()) {
                    return part;
                }
            } else {
                break;
            }
        }

        // Then look in the preceding systems in the same page
        SystemInfo prevSystem = system;

        while (true) {
            prevSystem = prevSystem.getPrecedingInPage();

            if (prevSystem != null) {
                Part part = prevSystem.getPartById(id);

                if ((part != null) && !part.isDummy()) {
                    return part;
                }
            } else {
                break;
            }
        }

        logger.warn("{} Cannot find real system part with id {}", this, id);

        return null;
    }
}
