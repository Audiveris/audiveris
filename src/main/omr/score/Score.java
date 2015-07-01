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

import omr.script.ParametersTask.PartData;

import omr.sheet.Book;
import omr.sheet.SystemInfo;

import omr.util.Param;

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
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Score.class);

    /** Number of lines in a staff */
    public static final int LINE_NB = 5;

    //~ Instance fields ----------------------------------------------------------------------------
    /** Score id, within containing book. */
    private int id;

    /** Contained pages. */
    private final List<Page> pages = new ArrayList<Page>();

    /** LogicalPart list for the whole score. */
    private List<LogicalPart> logicalParts;

    /** Handling of parts name and program. */
    private final Param<List<PartData>> partsParam = new PartsParam();

    /** Handling of tempo parameter. */
    private final Param<Integer> tempoParam = new Param<Integer>(Tempo.defaultTempo);

    /** The specified sound volume, if any. */
    private Integer volume;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a Score.
     */
    public Score ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
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
        source.setFile(book.getInputPath().toString());
        source.setOffset(book.getOffset());

        for (Page page : pages) {
            Source.SheetSystems sheetSystems = new Source.SheetSystems(page.getSheet().getNumber());
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

    //-------//
    // getId //
    //-------//
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

    //-----------------//
    // getLogicalParts //
    //-----------------//
    /**
     * Report the score list of logical parts.
     *
     * @return partList the list of logical parts
     */
    public List<LogicalPart> getLogicalParts ()
    {
        return logicalParts;
    }

    //--------------------//
    // getMeasureIdOffset //
    //--------------------//
    /**
     * Report the offset to add to page-based measure IDs of the provided page to get
     * absolute (score-based) IDs.
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

    //---------------//
    // getPartsParam //
    //---------------//
    public Param<List<PartData>> getPartsParam ()
    {
        return partsParam;
    }

    //----------------//
    // getTempoParam //
    //---------------//
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

    //-------//
    // setId //
    //-------//
    /**
     * @param id the id to set
     */
    public void setId (int id)
    {
        this.id = id;
    }

    //-----------------//
    // setLogicalParts //
    //-----------------//
    /**
     * Assign a part list valid for the whole score.
     *
     * @param logicalParts the list of logical parts
     */
    public void setLogicalParts (List<LogicalPart> logicalParts)
    {
        this.logicalParts = logicalParts;
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

        private final Constant.Integer defaultTempo = new Constant.Integer(
                "QuartersPerMn",
                120,
                "Default tempo, stated in number of quarters per minute");

        private final Constant.Integer defaultVolume = new Constant.Integer(
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
            List<LogicalPart> list = getLogicalParts();

            if (list != null) {
                List<PartData> data = new ArrayList<PartData>();

                for (LogicalPart logicalPart : list) {
                    // Initial setting for part midi program
                    int prog = (logicalPart.getMidiProgram() != null)
                            ? logicalPart.getMidiProgram() : logicalPart.getDefaultProgram();

                    data.add(new PartData(logicalPart.getName(), prog));
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
                    LogicalPart logicalPart = getLogicalParts().get(i);

                    // Part name
                    logicalPart.setName(data.name);

                    // Part midi program
                    logicalPart.setMidiProgram(data.program);
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
