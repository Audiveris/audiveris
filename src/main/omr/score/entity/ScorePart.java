//----------------------------------------------------------------------------//
//                                                                            //
//                             S c o r e P a r t                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.entity;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.math.GCD;

import omr.score.Score;
import omr.score.visitor.ScoreReductor;

import omr.util.Logger;
import omr.util.TreeNode;

import java.util.*;

/**
 * Class <code>ScorePart</code> defines a part at score level. It is instantiated in
 * each System by a SystemPart.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScorePart
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScorePart.class);

    //~ Instance fields --------------------------------------------------------

    /** The related score */
    private final Score score;

    /**
     * Distinguished id for this part (the same id is used by the corresponding
     * SystemPart in each System)
     */
    private Integer id;

    /** Name for this part */
    private String name;

    /** Instrument MIDI program, if any */
    private Integer midiProgram; // = 71;

    /** Tempo information, if any */
    private Integer tempo; // = 60;

    /** List of staff ids */
    private List<Integer> ids = new ArrayList<Integer>();

    /** Set of all different duration values in this part */
    private final SortedSet<Integer> durations = new TreeSet<Integer>();

    /** Greatest duration divisor */
    private Integer durationDivisor;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // ScorePart //
    //-----------//
    /**
     * Creates a new instance of ScorePart, built from a SystemPart
     *
     * @param systemPart the concrete SystemPart
     * @param score the related score entity
     */
    public ScorePart (SystemPart systemPart,
                      Score      score)
    {
        this.score = score;

        for (TreeNode node : systemPart.getStaves()) {
            Staff staff = (Staff) node;
            ids.add(staff.getId());
        }
    }

    /** Meant for XML binder only */
    private ScorePart ()
    {
        score = null;
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // getDefaultName //
    //----------------//
    public String getDefaultName ()
    {
        switch (getStaffIds()
                    .size()) {
        case 1 :
            return constants.defaultSingleStaffPartName.getValue();

        case 2 :
            return constants.defaultDoubleStaffPartName.getValue();

        default :
            return constants.defaultPartName.getValue();
        }
    }

    //-------------------//
    // getDefaultProgram //
    //-------------------//
    public Integer getDefaultProgram ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("Part #" + getId() + " size=" + getStaffIds().size());
        }

        switch (getStaffIds()
                    .size()) {
        case 1 :
            return constants.defaultSingleStaffPartProgram.getValue();

        case 2 :
            return constants.defaultDoubleStaffPartProgram.getValue();

        default :
            return constants.defaultPartProgram.getValue();
        }
    }

    //--------------------//
    // getDurationDivisor //
    //--------------------//
    /**
     * Report the duration divisor for this part
     * @return the divisor for this part, or null if not computable
     */
    public Integer getDurationDivisor ()
    {
        if (durationDivisor == null) {
            score.accept(new ScoreReductor());
        }

        return durationDivisor;
    }

    //-------//
    // setId //
    //-------//
    /**
     * Assign an id to this part
     *
     * @param id the assigned id
     */
    public void setId (int id)
    {
        this.id = id;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the id of this part
     *
     * @return the part id
     */
    public int getId ()
    {
        return id;
    }

    //----------------//
    // setMidiProgram //
    //----------------//
    public void setMidiProgram (Integer midiProgram)
    {
        this.midiProgram = midiProgram;
    }

    //----------------//
    // getMidiProgram //
    //----------------//
    public Integer getMidiProgram ()
    {
        return midiProgram;
    }

    //--------------//
    // isMultiStaff //
    //--------------//
    /**
     * Report whether there are more than a single staff in this part
     *
     * @return true if this part is multi-staff
     */
    public boolean isMultiStaff ()
    {
        return ids.size() > 1;
    }

    //---------//
    // setName //
    //---------//
    /**
     * Assign a name to this part
     *
     * @param name the new part name
     */
    public void setName (String name)
    {
        this.name = name;
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report the assigned name
     *
     * @return the part name
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

    //-------------//
    // getStaffIds //
    //-------------//
    /**
     * Report the staff ids for this part
     *
     * @return the list of staff ids
     */
    public List<Integer> getStaffIds ()
    {
        return ids;
    }

    //----------//
    // setTempo //
    //----------//
    public void setTempo (Integer tempo)
    {
        this.tempo = tempo;
    }

    //----------//
    // getTempo //
    //----------//
    public Integer getTempo ()
    {
        return tempo;
    }

    //-------------//
    // addDuration //
    //-------------//
    public void addDuration (int duration)
    {
        durations.add(duration);
    }

    //------------------------//
    // computeDurationDivisor //
    //------------------------//
    public void computeDurationDivisor ()
    {
        Integer[] durationArray = durations.toArray(
            new Integer[durations.size()]);
        durationDivisor = GCD.gcd(durationArray);

        if (logger.isFineEnabled()) {
            logger.fine(
                this + " durations=" + Arrays.deepToString(durationArray) +
                " divisor=" + durationDivisor);
        }
    }

    //--------//
    // equals //
    //--------//
    /**
     * Check whether the list of ids are identical
     *
     * @param obj the object to compare to
     * @return true if equal
     */
    @Override
    public boolean equals (Object obj)
    {
        if (obj instanceof ScorePart) {
            ScorePart sp = (ScorePart) obj;

            if (sp.ids.size() != ids.size()) {
                return false;
            }

            for (int i = 0; i < ids.size(); i++) {
                if (!(sp.ids.get(i).equals(ids.get(i)))) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    //----------------//
    // resetDurations //
    //----------------//
    public void resetDurations ()
    {
        durations.clear();
        durationDivisor = null;
    }

    //------------------//
    // simpleDurationOf //
    //------------------//
    /**
     * Export a duration to its simplest form, based on the greatest duration
     * divisor of the part
     *
     * @param value the raw duration
     * @return the simple duration expression, in the context of proper
     * divisions
     */
    public int simpleDurationOf (int value)
    {
        if (durationDivisor == null) {
            score.accept(new ScoreReductor());
        }

        return value / durationDivisor;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Part");

        if (id != null) {
            sb.append(" id=")
              .append(id);
        }

        if (name != null) {
            sb.append(" name=")
              .append(name);
        }

        sb.append(" [");

        for (Integer i : ids) {
            sb.append(i + " ");
        }

        sb.append("]}");

        return sb.toString();
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        // Default Part names
        Constant.String  defaultSingleStaffPartName = new Constant.String(
            "Voice",
            "Default name for a part with one staff");
        Constant.String  defaultDoubleStaffPartName = new Constant.String(
            "Piano",
            "Default name for a part with two staves");
        Constant.String  defaultPartName = new Constant.String(
            "NoName",
            "Default name for a part with more than two staves");

        // Default Midi program numbers
        Constant.Integer defaultSingleStaffPartProgram = new Constant.Integer(
            "MidiProgram",
            54,
            "Default program number for a part with one staff");
        Constant.Integer defaultDoubleStaffPartProgram = new Constant.Integer(
            "MidiProgram",
            1,
            "Default program number for a part with two staves");
        Constant.Integer defaultPartProgram = new Constant.Integer(
            "MidiProgram",
            1,
            "Default program number for a part with more than two staves");
    }
}
