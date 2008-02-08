//----------------------------------------------------------------------------//
//                                                                            //
//                            S y s t e m P a r t                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.entity;

import omr.score.common.PagePoint;
import omr.score.common.SystemPoint;
import omr.score.visitor.ScoreVisitor;

import omr.util.Logger;
import omr.util.TreeNode;

import java.awt.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * Class <code>SystemPart</code> handles the various parts found in one system,
 * since the layout of parts may vary from system to system
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SystemPart
    extends PartNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SystemPart.class);

    //~ Instance fields --------------------------------------------------------

    /** Id of this part within the system, starting at 1 */
    private final int id;

    /** The corresponding ScorePart */
    @Child
    private ScorePart scorePart;

    /** Specific child : sequence of staves that belong to this system */
    private final StaffList staves;

    /** Specific child : sequence of measures that compose this system part */
    private final MeasureList measures;

    /** Specific child : list of slurs */
    private final SlurList slurs;

    /** Specific child : list of lyric lines */
    private final LyricList lyrics;

    /** Lonesome child : Starting barline (the others are linked to measures) */
    private Barline startingBarline;

    /** Text entities within this system part */
    private Set<Text> texts;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // SystemPart //
    //------------//
    /**
     * Create a new instance of SystemPart
     *
     * @param system the containing system
     * @param id the part id within the system
     */
    public SystemPart (System system,
                       int    id)
    {
        super(system);
        this.id = id;

        // Allocate specific children
        staves = new StaffList(this);
        measures = new MeasureList(this);
        slurs = new SlurList(this);
        lyrics = new LyricList(this);
        texts = new HashSet<Text>();
    }

    //~ Methods ----------------------------------------------------------------

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
        return (Measure) getMeasures()
                             .get(getMeasures().size() - 1);
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
    public Measure getMeasureAt (SystemPoint systemPoint)
    {
        Measure measure = null;

        for (TreeNode node : getMeasures()) {
            measure = (Measure) node;

            if (systemPoint.x <= measure.getBarline()
                                        .getRightX()) {
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
    public Staff getStaffAt (PagePoint point)
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

    //------------//
    // getStaffAt //
    //------------//
    /**
     * Report the staff nearest (in ordinate) to a provided system point
     *
     * @param point the provided system point
     *
     * @return the nearest staff
     */
    public Staff getStaffAt (SystemPoint point)
    {
        return getStaffAt(getSystem().toPagePoint(point));
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
    public System getSystem ()
    {
        return (System) getParent();
    }

    //----------//
    // getTexts //
    //----------//
    public Set<Text> getTexts ()
    {
        return texts;
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
        } else if (node instanceof Measure) {
            measures.addChild(node);
        } else if (node instanceof Slur) {
            slurs.addChild(node);
        } else if (node instanceof LyricLine) {
            lyrics.addChild(node);
        } else {
            super.addChild(node);
        }
    }

    //---------//
    // addText //
    //---------//
    public void addText (Text text)
    {
        texts.add(text);
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
        texts.clear();
    }

    //--------------------//
    // populateLyricLines //
    //--------------------//
    /**
     * Organize the various lyric items in aligned lyric lines
     */
    public void populateLyricLines ()
    {
        // Create the lyric lines as needed
        for (Text text : texts) {
            if (text instanceof LyricItem) {
                LyricItem item = (LyricItem) text;
                LyricLine.populate(item, this);
            }
        }

        // Assign the lines id & related staff
        Collections.sort(
            getLyrics(),
            new Comparator<TreeNode>() {
                    public int compare (TreeNode tn1,
                                        TreeNode tn2)
                    {
                        LyricLine l1 = (LyricLine) tn1;
                        LyricLine l2 = (LyricLine) tn2;

                        return Integer.signum(l1.getY() - l2.getY());
                    }
                });

        for (TreeNode node : getLyrics()) {
            LyricLine line = (LyricLine) node;
            line.setId(lyrics.getChildren().indexOf(line) + 1);
            line.setStaff(
                getSystem().getStaffAbove(new SystemPoint(0, line.getY())));
        }
    }

    //----------------------//
    // refineLyricSyllables //
    //----------------------//
    /**
     * Determine for each lyric item of syllable kind, its precise syllabic type
     * (single, of part of a longer word)
     */
    public void refineLyricSyllables ()
    {
        for (TreeNode node : getLyrics()) {
            LyricLine line = (LyricLine) node;
            line.refineLyricSyllables();
        }
    }
    
    //-------------------------//
    // connectSyllablesToNotes //
    //-------------------------//
    /**
     * Assign each syllable to its related node
     */
    public void connectSyllablesToNotes()
    {
        for (TreeNode node : getLyrics()) {
            LyricLine line = (LyricLine) node;
            line.connectSyllablesToNotes();
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
          .append(getId())
          .append(" [");

        if (getStaves() != null) {
            for (TreeNode node : getStaves()) {
                if (node != null) {
                    Staff staff = (Staff) node;
                    sb.append(staff.getId() + " ");
                }
            }
        }

        sb.append("]}");

        return sb.toString();
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // LyricList //
    //-----------//
    private static class LyricList
        extends ScoreNode
    {
        //~ Constructors -------------------------------------------------------

        LyricList (SystemPart container)
        {
            super(container);
        }
    }

    //-------------//
    // MeasureList //
    //-------------//
    private static class MeasureList
        extends ScoreNode
    {
        //~ Constructors -------------------------------------------------------

        MeasureList (SystemPart container)
        {
            super(container);
        }
    }

    //----------//
    // SlurList //
    //----------//
    private static class SlurList
        extends ScoreNode
    {
        //~ Constructors -------------------------------------------------------

        SlurList (SystemPart container)
        {
            super(container);
        }
    }

    //-----------//
    // StaffList //
    //-----------//
    private static class StaffList
        extends ScoreNode
    {
        //~ Constructors -------------------------------------------------------

        StaffList (SystemPart container)
        {
            super(container);
        }
    }
}
