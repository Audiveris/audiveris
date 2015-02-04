//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S c o r e                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.math.Rational;

import omr.score.entity.MeasureId.MeasureRange;
import omr.score.entity.Page;
import omr.score.entity.ScoreNode;
import omr.score.entity.ScorePart;
import omr.score.entity.Tempo;
import omr.score.visitor.ScoreVisitor;

import omr.script.ParametersTask.PartData;

import omr.sheet.Book;
import omr.sheet.SystemInfo;

import omr.util.Param;
import omr.util.TreeNode;

import com.audiveris.proxymusic.util.Source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code Score} represents a single movement, and is composed of one or several
 * pages.
 *
 * @author Hervé Bitteur
 */
public class Score
        extends ScoreNode
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Score.class);

    /** Number of lines in a staff */
    public static final int LINE_NB = 5;

    //~ Instance fields ----------------------------------------------------------------------------
    /** Score id, within containing book. */
    private int id;

    /** Greatest duration divisor. */
    private Integer durationDivisor;

    /** ScorePart list for the whole score. */
    private List<ScorePart> partList;

    /** The specified sound volume, if any. */
    private Integer volume;

    /** Potential measure range, if not all score is to be played */
    private MeasureRange measureRange;

    /** Handling of parts name and program. */
    private final Param<List<PartData>> partsParam = new PartsParam();

    /** Handling of tempo parameter. */
    private final Param<Integer> tempoParam = new Param<Integer>(Tempo.defaultTempo);

    /** Contained pages. */
    private final List<Page> pages = new ArrayList<Page>();

    //~ Constructors -------------------------------------------------------------------------------
    //-------//
    // Score //
    //-------//
    /**
     * Create a Score.
     */
    public Score ()
    {
        super(null); // No container
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //---------//
    // addPage //
    //---------//
    public void addPage (Page page)
    {
        pages.add(page);
    }

    //-------------//
    // buildSource //
    //-------------//
    /**
     * Build the Source structure that describes the source items for this score.
     *
     * @return the populated Source object
     */
    public Source buildSource ()
    {
        Source source = new Source();
        Book book = getFirstPage().getSheet().getBook();
        source.setFile(book.getImagePath().toString());
        source.setOffset(book.getOffset());

        for (Page page : pages) {
            Source.SheetSystems sheetSystems = new Source.SheetSystems(page.getSheet().getIndex());
            source.getSheets().add(sheetSystems);

            for (SystemInfo system : page.getSystems()) {
                sheetSystems.getSystems().add(system.getId());
            }
        }

        return source;
    }

    //-------//
    // close //
    //-------//
    /**
     * Close this score instance, as well as its view if any.
     */
    public void close ()
    {
        logger.info("Closing {}", this);
    }

    //------//
    // dump //
    //------//
    /**
     * Dump a whole score hierarchy.
     */
    public void dump ()
    {
        System.out.println("----------------------------------------------------------------");

        if (dumpNode()) {
            dumpChildren(1);
        }

        System.out.println("----------------------------------------------------------------");
    }

    //------------------//
    // getDefaultVolume //
    //------------------//
    /**
     * Report default value for Midi volume.
     *
     * @return the default volume value
     */
    public static int getDefaultVolume ()
    {
        return constants.defaultVolume.getValue();
    }

    //--------------------//
    // getDurationDivisor //
    //--------------------//
    /**
     * Report the common divisor used for this score when simplifying the durations.
     *
     * @return the computed divisor (GCD), or null if not computable
     */
    public Integer getDurationDivisor ()
    {
        if (durationDivisor == null) {
            accept(new ScoreReductor());
        }

        return durationDivisor;
    }

    //--------------//
    // getFirstPage //
    //--------------//
    public Page getFirstPage ()
    {
        if (pages.isEmpty()) {
            return null;
        } else {
            return pages.get(0);
        }
    }

    /**
     * @return the id
     */
    public int getId ()
    {
        return id;
    }

    //-------------//
    // getLastPage //
    //-------------//
    public Page getLastPage ()
    {
        if (pages.isEmpty()) {
            return null;
        } else {
            return pages.get(pages.size() - 1);
        }
    }

    //--------------------//
    // getMeasureIdOffset //
    //--------------------//
    /**
     * Report the offset to add to page-based measure ids of the
     * provided page to get absolute (score-based) ids.
     *
     * @param page the provided page
     * @return the measure id offset for the page
     */
    public Integer getMeasureIdOffset (Page page)
    {
        int offset = 0;

        for (Page p : pages) {
            if (p == page) {
                return offset;
            } else {
                Integer delta = p.getDeltaMeasureId();

                if (delta != null) {
                    offset += delta;
                } else {
                    // This page has no measures yet, so ...
                    return null;
                }
            }
        }

        throw new IllegalArgumentException(page + " not found in score");
    }

    //------------------//
    // getMeasureOffset //
    //------------------//
    /**
     * Report the offset to add to page-based measure index of the
     * provided page to get absolute (score-based) indices.
     *
     * @param page the provided page
     * @return the measure index offset for the page
     */
    public int getMeasureOffset (Page page)
    {
        int offset = 0;

        for (Page p : pages) {
            if (p == page) {
                return offset;
            } else {
                offset += p.getMeasureCount();
            }
        }

        throw new IllegalArgumentException(page + " not found in score");
    }

    //-----------------//
    // getMeasureRange //
    //-----------------//
    /**
     * Report the potential range of selected measures.
     *
     * @return the selected measure range, perhaps null
     */
    public MeasureRange getMeasureRange ()
    {
        return measureRange;
    }

    //----------//
    // getPages //
    //----------//
    /**
     * Report the collection of pages in that score.
     *
     * @return the pages
     */
    public List<Page> getPages ()
    {
        return pages;
    }

    //-------------//
    // getPartList //
    //-------------//
    /**
     * Report the global list of parts.
     *
     * @return partList the list of score parts
     */
    public List<ScorePart> getPartList ()
    {
        return partList;
    }

    //---------------//
    // getPartsParam //
    //---------------//
    public Param<List<PartData>> getPartsParam ()
    {
        return partsParam;
    }

    //----------//
    // getScorePages //
    //----------//
    /**
     * Report the collection of pages in that score.
     *
     * @return the pages
     */
    @Deprecated
    public List<TreeNode> getScorePages ()
    {
        return getChildren();
    }

    //----------------//
    // getTempoParam //
    //----------------//
    public Param<Integer> getTempoParam ()
    {
        return tempoParam;
    }

    //-----------//
    // getVolume //
    //-----------//
    /**
     * Report the assigned volume, if any.
     * If the value is not yet set, it is set to the default value and returned.
     *
     * @return the assigned volume, or null
     */
    public Integer getVolume ()
    {
        if (!hasVolume()) {
            volume = getDefaultVolume();
        }

        return volume;
    }

    //-----------//
    // hasVolume //
    //-----------//
    /**
     * Check whether a volume has been defined for this score.
     *
     * @return true if a volume is defined
     */
    public boolean hasVolume ()
    {
        return volume != null;
    }

    //-------------//
    // isMultiPage //
    //-------------//
    /**
     * @return the multiPage
     */
    public boolean isMultiPage ()
    {
        return pages.size() > 1;
    }

    //-----------------//
    // setDefaultTempo //
    //-----------------//
    /**
     * Assign default value for Midi tempo.
     *
     * @param tempo the default tempo value
     */
    public static void setDefaultTempo (int tempo)
    {
        constants.defaultTempo.setValue(tempo);
    }

    //------------------//
    // setDefaultVolume //
    //------------------//
    /**
     * Assign default value for Midi volume.
     *
     * @param volume the default volume value
     */
    public static void setDefaultVolume (int volume)
    {
        constants.defaultVolume.setValue(volume);
    }

    //--------------------//
    // setDurationDivisor //
    //--------------------//
    /**
     * Remember the common divisor used for this score when simplifying the durations.
     *
     * @param durationDivisor the computed divisor (GCD), or null
     */
    public void setDurationDivisor (Integer durationDivisor)
    {
        this.durationDivisor = durationDivisor;
    }

    /**
     * @param id the id to set
     */
    public void setId (int id)
    {
        this.id = id;
    }

    //-----------------//
    // setMeasureRange //
    //-----------------//
    /**
     * Remember a range of measure for this score.
     *
     * @param measureRange the range of selected measures
     */
    public void setMeasureRange (MeasureRange measureRange)
    {
        this.measureRange = measureRange;
    }

    //-------------//
    // setPartList //
    //-------------//
    /**
     * Assign a part list valid for the whole score.
     *
     * @param partList the list of score parts
     */
    public void setPartList (List<ScorePart> partList)
    {
        this.partList = partList;
    }

    //-----------//
    // setVolume //
    //-----------//
    /**
     * Assign a volume value.
     *
     * @param volume the volume value to be assigned
     */
    public void setVolume (Integer volume)
    {
        this.volume = volume;
    }

    //------------------//
    // simpleDurationOf //
    //------------------//
    /**
     * Export a duration to its simplest form, based on the greatest duration divisor of
     * the score.
     *
     * @param value the raw duration
     * @return the simple duration expression, in the param of proper divisions
     */
    public int simpleDurationOf (Rational value)
    {
        return value.num * (getDurationDivisor() / value.den);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{Score " + id + "}";
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        Constant.Integer defaultTempo = new Constant.Integer(
                "QuartersPerMn",
                120,
                "Default tempo, stated in number of quarters per minute");

        Constant.Integer defaultVolume = new Constant.Integer(
                "Volume",
                78,
                "Default Volume in 0..127 range");
    }

    //------------//
    // PartsParam //
    //------------//
    private class PartsParam
            extends Param<List<PartData>>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public List<PartData> getSpecific ()
        {
            List<ScorePart> list = getPartList();

            if (list != null) {
                List<PartData> data = new ArrayList<PartData>();

                for (ScorePart scorePart : list) {
                    // Initial setting for part midi program
                    int prog = (scorePart.getMidiProgram() != null) ? scorePart.getMidiProgram()
                            : scorePart.getDefaultProgram();

                    data.add(new PartData(scorePart.getName(), prog));
                }

                return data;
            } else {
                return null;
            }
        }

        @Override
        public boolean setSpecific (List<PartData> specific)
        {
            try {
                for (int i = 0; i < specific.size(); i++) {
                    PartData data = specific.get(i);
                    ScorePart scorePart = getPartList().get(i);

                    // Part name
                    scorePart.setName(data.name);

                    // Part midi program
                    scorePart.setMidiProgram(data.program);
                }

                logger.info("Score parts have been updated");

                return true;
            } catch (Exception ex) {
                logger.warn("Error updating score parts", ex);
            }

            return false;
        }
    }
}
