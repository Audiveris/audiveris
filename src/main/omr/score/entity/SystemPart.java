//----------------------------------------------------------------------------//
//                                                                            //
//                            S y s t e m P a r t                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.visitor.ScoreVisitor;

import omr.util.Predicate;
import omr.util.TreeNode;

import java.util.*;

/**
 * Class <code>SystemPart</code> handles each of the various parts found in
 * one system, since the layout of parts may vary from system to system.
 *
 * @author Hervé Bitteur
 */
public class SystemPart
    extends PartNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SystemPart.class);

    /** For comparing (TreeNode) SystemPart instances according to their id */
    public static final Comparator<TreeNode> idComparator = new Comparator<TreeNode>() {
        public int compare (TreeNode o1,
                            TreeNode o2)
        {
            if (o1 instanceof SystemPart && o2 instanceof SystemPart) {
                SystemPart p1 = (SystemPart) o1;
                SystemPart p2 = (SystemPart) o2;

                return Integer.signum(p1.getId() - p2.getId());
            } else {
                throw new RuntimeException(
                    "Comparing illegal SystemPart instances");
            }
        }
    };


    //~ Instance fields --------------------------------------------------------

    /** Id of this part within the system, starting at 1 */
    private int id;

    /** Name, if any, that faces this system part */
    private String name;

    /** The corresponding ScorePart */
    private ScorePart scorePart;

    /** A brace attached if any */
    private Glyph brace;

    /** Specific child : sequence of staves that belong to this system */
    private final Container staves;

    /** Specific child : sequence of measures that compose this system part */
    private final Container measures;

    /** Specific child : list of slurs */
    private final Container slurs;

    /** Specific child : list of lyrics lines */
    private final Container lyrics;

    /** Specific child : list of text items */
    private final Container texts;

    /** Lonesome child : Starting barline (the others are linked to measures) */
    private Barline startingBarline;

    /** Flag to indicate this system part is just a placeholder */
    private boolean dummy;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // SystemPart //
    //------------//
    /**
     * Create a new instance of SystemPart
     *
     * @param system the containing system
     */
    public SystemPart (ScoreSystem system)
    {
        super(system);

        // Allocate specific children
        staves = new Container(this, "Staves");
        measures = new Container(this, "Measures");
        slurs = new Container(this, "Slurs");
        lyrics = new Container(this, "Lyrics");
        texts = new Container(this, "Texts");
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // setBrace //
    //----------//
    /**
     * @param brace the brace to set
     */
    public void setBrace (Glyph brace)
    {
        this.brace = brace;
    }

    //----------//
    // getBrace //
    //----------//
    /**
     * @return the brace
     */
    public Glyph getBrace ()
    {
        return brace;
    }

    //----------//
    // setDummy //
    //----------//
    public void setDummy (boolean dummy)
    {
        this.dummy = dummy;
    }

    //---------//
    // isDummy //
    //---------//
    public boolean isDummy ()
    {
        return dummy;
    }

    //-----------------//
    // getFirstMeasure //
    //-----------------//
    /**
     * Report the first measure in this system part
     *
     * @return the first measure entity
     */
    public Measure getFirstMeasure ()
    {
        return (Measure) getMeasures()
                             .get(0);
    }

    //---------------//
    // getFirstStaff //
    //---------------//
    /**
     * Report the first staff in this system aprt
     *
     * @return the first staff entity
     */
    public Staff getFirstStaff ()
    {
        return (Staff) getStaves()
                           .get(0);
    }

    //--------------//
    // getFollowing //
    //--------------//
    public SystemPart getFollowing ()
    {
        ScoreSystem nextSystem = (ScoreSystem) getSystem()
                                                   .getNextSibling();

        if (nextSystem != null) {
            return nextSystem.getPart(id);
        } else {
            return null;
        }
    }

    //-------//
    // setId //
    //-------//
    /**
     * Set the part id within the containing system, starting at 1
     *
     * @param id the id value
     */
    public void setId (int id)
    {
        this.id = id;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the part id within the containing system, starting at 1
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
     * Report the last measure in the system part
     *
     * @return the last measure entity
     */
    public Measure getLastMeasure ()
    {
        List<TreeNode> meas = getMeasures();

        if (!meas.isEmpty()) {
            return (Measure) meas.get(meas.size() - 1);
        } else {
            return null;
        }
    }

    //------------------//
    // getLastSoundTime //
    //------------------//
    /**
     * Report the time, counted from beginning of this part, when sound stops,
     * which means that ending rests are not counted.
     *
     * @param measureId potential constraint on measure id,
     * null for no constraint
     * @return the relative time of last Midi "note off" in this part
     */
    public int getLastSoundTime (Integer measureId)
    {
        // Browse measures backwards
        for (ListIterator it = getMeasures()
                                   .listIterator(getMeasures().size());
             it.hasPrevious();) {
            Measure measure = (Measure) it.previous();

            if ((measureId == null) || (measure.getId() == measureId)) {
                int time = measure.getLastSoundTime();

                if (time > 0) {
                    return measure.getStartTime() + time;
                }
            }
        }

        return 0;
    }

    //--------------//
    // getLastStaff //
    //--------------//
    /**
     * Report the last staff in this system part
     *
     * @return the last staff entity
     */
    public Staff getLastStaff ()
    {
        return (Staff) getStaves()
                           .get(getStaves().size() - 1);
    }

    //-----------//
    // getLyrics //
    //-----------//
    /**
     * Report the collection of lyrics
     *
     * @return the lyrics list, which may be empty but not null
     */
    public List<TreeNode> getLyrics ()
    {
        return lyrics.getChildren();
    }

    //--------------//
    // getMeasureAt //
    //--------------//
    /**
     * Report the measure that contains a given point (assumed to be in the
     * containing system part)
     *
     * @param systemPoint system-based coordinates of the given point
     * @return the containing measure
     */
    public Measure getMeasureAt (PixelPoint systemPoint)
    {
        Measure measure = null;

        for (TreeNode node : getMeasures()) {
            measure = (Measure) node;

            Barline barline = measure.getBarline();

            if ((barline == null) || (systemPoint.x <= barline.getRightX())) {
                return measure;
            }
        }

        return measure;
    }

    //-------------//
    // getMeasures //
    //-------------//
    /**
     * Report the collection of measures
     *
     * @return the measure list, which may be empty but not null
     */
    public List<TreeNode> getMeasures ()
    {
        return measures.getChildren();
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

    //--------------//
    // getPreceding //
    //--------------//
    /**
     * Report the corresponding part (if any) in the previous system. Even if
     * there is a previous system, there may be no part that corresponds to this
     * one.
     * @return the corresponding part, or null
     */
    public SystemPart getPreceding ()
    {
        ScoreSystem prevSystem = (ScoreSystem) getSystem()
                                                   .getPreviousSibling();

        if (prevSystem != null) {
            return prevSystem.getPart(id);
        } else {
            return null;
        }
    }

    //--------------//
    // setScorePart //
    //--------------//
    public void setScorePart (ScorePart scorePart)
    {
        this.scorePart = scorePart;
    }

    //--------------//
    // getScorePart //
    //--------------//
    public ScorePart getScorePart ()
    {
        return scorePart;
    }

    //----------//
    // getSlurs //
    //----------//
    /**
     * Report the collection of slurs
     *
     * @return the slur list, which may be empty but not null
     */
    public List<TreeNode> getSlurs ()
    {
        return slurs.getChildren();
    }

    //----------//
    // getSlurs //
    //----------//
    /**
     * Report the collection of slurs for which the provided predicate is true
     *
     * @param predicate the check to run
     * @return the collection of selected slurs, which may be empty
     */
    public List<Slur> getSlurs (Predicate<Slur> predicate)
    {
        List<Slur> selectedSlurs = new ArrayList<Slur>();

        for (TreeNode sNode : getSlurs()) {
            Slur slur = (Slur) sNode;

            if (predicate.check(slur)) {
                selectedSlurs.add(slur);
            }
        }

        return selectedSlurs;
    }

    //------------//
    // getStaffAt //
    //------------//
    /**
     * Report the staff nearest (in ordinate) to a provided page point
     *
     * @param point the provided page point
     *
     * @return the nearest staff
     */
    public Staff getStaffAt (PixelPoint point)
    {
        int   minDy = Integer.MAX_VALUE;
        Staff best = null;

        for (TreeNode node : getStaves()) {
            Staff staff = (Staff) node;
            int   midY = staff.getTopLeft().y + (staff.getHeight() / 2);
            int   dy = Math.abs(point.y - midY);

            if (dy < minDy) {
                minDy = dy;
                best = staff;
            }
        }

        return best;
    }

    //--------------------//
    // setStartingBarline //
    //--------------------//
    /**
     * Set the barline that starts the part
     *
     * @param startingBarline the starting barline
     */
    public void setStartingBarline (Barline startingBarline)
    {
        this.startingBarline = startingBarline;
    }

    //--------------------//
    // getStartingBarline //
    //--------------------//
    /**
     * Get the barline that starts the part
     *
     * @return barline the starting bar line (which may be null)
     */
    public Barline getStartingBarline ()
    {
        return startingBarline;
    }

    //-----------//
    // getStaves //
    //-----------//
    /**
     * Report the ordered list of staves that belong to this system part
     *
     * @return the list of staves
     */
    public List<TreeNode> getStaves ()
    {
        return (staves == null) ? null : staves.getChildren();
    }

    //-----------//
    // getSystem //
    //-----------//
    /**
     * Report the containing system
     *
     * @return the containing system
     */
    @Override
    public ScoreSystem getSystem ()
    {
        return (ScoreSystem) getParent();
    }

    //----------//
    // getTexts //
    //----------//
    public List<TreeNode> getTexts ()
    {
        return texts.getChildren();
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // addChild //
    //----------//
    /**
     * Overrides normal behavior, to deal with the separation of specific children
     *
     * @param node the node to insert
     */
    @Override
    public void addChild (TreeNode node)
    {
        // Specific children lists
        if (node instanceof Staff) {
            staves.addChild(node);
            reset();
        } else if (node instanceof Measure) {
            measures.addChild(node);
        } else if (node instanceof Slur) {
            slurs.addChild(node);
        } else if (node instanceof LyricsLine) {
            lyrics.addChild(node);
        } else if (node instanceof Text) {
            texts.addChild(node);
        } else {
            super.addChild(node);
        }
    }

    //---------------//
    // barlineExists //
    //---------------//
    public boolean barlineExists (int x,
                                  int maxShiftDx)
    {
        for (TreeNode node : getMeasures()) {
            Measure measure = (Measure) node;

            if (Math.abs(measure.getBarline().getCenter().x - x) <= maxShiftDx) {
                return true;
            }
        }

        return false; // Not found
    }

    //-------------//
    // cleanupNode //
    //-------------//
    public void cleanupNode ()
    {
        getSlurs()
            .clear();
        getLyrics()
            .clear();
        getTexts()
            .clear();
    }

    //-----------------//
    // createDummyPart //
    //-----------------//
    /**
     * Create an dummy system part, parallel to this part, just to fill
     * needed measures in another part. <ul>
     * <li>Clef is taken from first real measure of part to be extended</li>
     * <li>Key sig is taken from this part</li>
     * <li>Time sig is taken from this part</li>
     * <li>Measures are filled with just one whole rest</li>
     * </ul>
     * @param id the id for the desired dummy part
     * @return the created dummy part, ready to be exported
     */
    public SystemPart createDummyPart (int id)
    {
        if (logger.isFineEnabled()) {
            logger.fine(getContextString() + " createDummyPart for id=" + id);
        }

        // Find some concrete system part for the provided id
        SystemPart  nextPart;
        ScoreSystem nextSystem = getSystem();

        while (true) {
            nextSystem = (ScoreSystem) nextSystem.getNextSibling();

            if (nextSystem != null) {
                SystemPart part = nextSystem.getPart(id);

                if (part != null) {
                    nextPart = part;

                    break;
                }
            } else {
                logger.warning(
                    getContextString() +
                    " Cannot find real system part with id " + id);

                return null;
            }
        }

        SystemPart dummyPart = new SystemPart(getSystem());
        dummyPart.setId(id);
        dummyPart.setDummy(true);
        dummyPart.setScorePart(nextPart.getScorePart());

        Measure nextMeasure = nextPart.getFirstMeasure();

        // Loop on measures
        boolean isFirstMeasure = true;

        for (TreeNode mn : getMeasures()) {
            Measure measure = (Measure) mn;
            Measure dummyMeasure = new Measure(dummyPart);
            dummyMeasure.setDummy(true);
            dummyMeasure.setId(measure.getId());

            // Loop on staves
            int staffIndex = -1;

            for (TreeNode sn : nextPart.getStaves()) {
                Staff nextStaff = (Staff) sn;
                staffIndex++;

                Staff dummyStaff;

                if (isFirstMeasure) {
                    // Create dummy Staff
                    dummyStaff = new Staff(
                        null, // No staff info counterpart
                        dummyPart,
                        null,
                        getFirstStaff().getWidth(),
                        nextStaff.getHeight());
                    dummyStaff.setDummy(true);

                    // Create dummy Clef
                    Clef nextClef = nextMeasure.getFirstMeasureClef(
                        nextStaff.getId());

                    if (nextClef != null) {
                        Clef dummyClef = new Clef(
                            dummyMeasure,
                            dummyStaff,
                            nextClef);
                    }

                    isFirstMeasure = false;
                } else {
                    dummyStaff = (Staff) dummyPart.getStaves()
                                                  .get(staffIndex);
                }

                // Replicate Key if any
                if (!measure.getKeySignatures()
                            .isEmpty()) {
                    KeySignature dummyKey = new KeySignature(
                        dummyMeasure,
                        dummyStaff,
                        (KeySignature) measure.getKeySignatures().get(0));
                }

                // Replicate Time if any
                TimeSignature ts = measure.getTimeSignature();

                if (ts != null) {
                    new TimeSignature(dummyMeasure, dummyStaff, ts);
                }

                // Create dummy Whole rest (w/ no precise location)
                dummyMeasure.addWholeRest(dummyStaff, null);
                dummyMeasure.buildVoices();
            }

            dummyMeasure.setBox(
                new PixelRectangle(
                    measure.getBox().x,
                    nextPart.getBox().y,
                    measure.getBox().width,
                    nextPart.getBox().height));
        }

        if (logger.isFineEnabled()) {
            if (dummyPart.dumpNode()) {
                dummyPart.dumpChildren(1);
            }
        }

        return dummyPart;
    }

    //--------------//
    // mapSyllables //
    //--------------//
    /**
     * Assign each syllable to its related node
     */
    public void mapSyllables ()
    {
        for (TreeNode node : getLyrics()) {
            LyricsLine line = (LyricsLine) node;
            line.mapSyllables();
        }
    }

    //---------------------//
    // populateLyricsLines //
    //---------------------//
    /**
     * Organize the various lyrics items in aligned lyrics lines
     */
    public void populateLyricsLines ()
    {
        // Create the lyrics lines as needed
        for (TreeNode tn : getTexts()) {
            Text text = (Text) tn;

            if (text instanceof LyricsItem) {
                LyricsItem item = (LyricsItem) text;
                LyricsLine.populate(item, this);
            }
        }

        // Assign the lines id & related staff
        Collections.sort(getLyrics(), LyricsLine.yComparator);

        for (TreeNode node : getLyrics()) {
            LyricsLine line = (LyricsLine) node;
            line.setId(lyrics.getChildren().indexOf(line) + 1);
            line.setStaff(
                getSystem().getStaffAbove(new PixelPoint(0, line.getY())));
        }
    }

    //----------------------//
    // refineLyricSyllables //
    //----------------------//
    /**
     * Determine for each lyrics item of syllable kind, its precise syllabic type
     * (single, of part of a longer word)
     */
    public void refineLyricSyllables ()
    {
        for (TreeNode node : getLyrics()) {
            LyricsLine line = (LyricsLine) node;
            line.refineLyricSyllables();
        }
    }

    //-------------------------//
    // retrieveSlurConnections //
    //-------------------------//
    /**
     * Retrieve the connections between the (orphan) slurs at the beginning of
     * this part and the (orphan) slurs at the end of the preceding part
     */
    public void retrieveSlurConnections ()
    {
        // Orphans slurs at the beginning of the current system
        List<Slur> orphans = getSlurs(Slur.isBeginningOrphan);
        Collections.sort(orphans, Slur.verticalComparator);

        // Ending orphans in preceding system/part (if such part exists)
        SystemPart precedingPart = getPreceding();

        if (precedingPart != null) {
            List<Slur> precedingOrphans = precedingPart.getSlurs(
                Slur.isEndingOrphan);
            Collections.sort(precedingOrphans, Slur.verticalComparator);

            // Connect the orphans as much as possible
            SlurLoop: 
            for (Slur slur : orphans) {
                for (Slur prevSlur : precedingOrphans) {
                    if (slur.canExtend(prevSlur)) {
                        slur.connectTo(prevSlur);

                        continue SlurLoop;
                    }
                }

                // No connection for this orphan
                slur.addError(" Could not left-connect slur #" + slur.getId());
            }

            // Check previous orphans
            for (Slur prevSlur : precedingOrphans) {
                if (prevSlur.getRightExtension() == null) {
                    prevSlur.addError(
                        " Could not right-connect slur #" + prevSlur.getId());
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
        sb.append("{SystemPart #")
          .append(getId());

        if (dummy) {
            sb.append(" dummy");
        }

        sb.append(" [");

        if (getStaves() != null) {
            boolean first = true;

            for (TreeNode node : getStaves()) {
                if (node != null) {
                    Staff staff = (Staff) node;

                    if (!first) {
                        sb.append(",");
                    }

                    sb.append(staff.getId());
                }

                first = false;
            }
        }

        sb.append("]");

        if (name != null) {
            sb.append(" name:")
              .append(name);
        }

        sb.append("}");

        return sb.toString();
    }

    //------------//
    // computeBox //
    //------------//
    @Override
    protected void computeBox ()
    {
        // Use the union of staves boxes
        PixelRectangle newBox = null;

        for (TreeNode node : getStaves()) {
            Staff staff = (Staff) node;

            if (newBox == null) {
                newBox = staff.getBox();
            } else {
                newBox = newBox.union(staff.getBox());
            }
        }

        setBox(newBox);
    }
}
