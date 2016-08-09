//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S c o r e                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.score;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.script.ParametersTask.PartData;

import omr.sheet.Book;
import omr.sheet.Sheet;
import omr.sheet.SheetStub;

import omr.util.Navigable;
import omr.util.Param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Score} represents a single movement, and is composed of one or several
 * pages.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "score")
public class Score
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            Score.class);

    /** Number of lines in a staff */
    public static final int LINE_NB = 5;

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Score id, within containing book. */
    @XmlAttribute
    private int id;

    /** LogicalPart list for the whole score. */
    @XmlElement(name = "logical-part")
    private List<LogicalPart> logicalParts;

    /** Pages references. */
    @XmlElement(name = "page")
    private final List<PageRef> pageRefs = new ArrayList<PageRef>();

    // Transient data
    //---------------
    //
    /** Containing book. */
    @Navigable(false)
    private Book book;

    /** Referenced pages. */
    private ArrayList<Page> pages;

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

    //---------//
    // addPage //
    //---------//
    public void addPage (Page page)
    {
        getPages().add(page);
        pageRefs.add(
                new PageRef(
                        page.getSheet().getStub().getNumber(),
                        page.getId(),
                        page.getMeasureDeltaId()));
        page.setScore(this);
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

    //-------------//
    // disposePage //
    //-------------//
    public void disposePage (Page page)
    {
        throw new RuntimeException("disposePage. Not implemented yet.");
    }

    /**
     * @return the book
     */
    public Book getBook ()
    {
        return book;
    }

    //--------------//
    // getFirstPage //
    //--------------//
    public Page getFirstPage ()
    {
        if (pageRefs.isEmpty()) {
            return null;
        }

        return getPage(pageRefs.get(0));
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
        if (pageRefs.isEmpty()) {
            return null;
        }

        return getPage(pageRefs.get(pageRefs.size() - 1));
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
        final PageRef ref = getPageRef(page);
        int offset = 0;

        for (PageRef pageRef : pageRefs) {
            if (pageRef == ref) {
                return offset;
            }

            offset += pageRef.deltaMeasureId;
        }

        return offset;
    }

    //---------//
    // getPage //
    //---------//
    /**
     * Report the page at provided 1-based number
     *
     * @param number 1-based number in score
     * @return the corresponding page
     */
    public Page getPage (int number)
    {
        final int index = number - 1;

        if ((index < 0) || (index >= pageRefs.size())) {
            throw new IllegalArgumentException("No page with number " + number);
        }

        return getPage(pageRefs.get(index));
    }

    //--------------//
    // getPageCount //
    //--------------//
    /**
     * Report the number of pages in score.
     *
     * @return number of pages
     */
    public int getPageCount ()
    {
        return pageRefs.size();
    }

    //--------------//
    // getPageIndex //
    //--------------//
    /**
     * Report index of the provided page in the score sequence of pages.
     *
     * @param page the provided page
     * @return the page index in score, or -1 if not found
     */
    public int getPageIndex (Page page)
    {
        final PageRef ref = getPageRef(page);

        return pageRefs.indexOf(ref);
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
        if (pages == null) {
            pages = new ArrayList<Page>();

            // De-reference pageRefs
            for (PageRef ref : pageRefs) {
                pages.add(getPage(ref));
            }
        }

        return pages;
    }

    //---------------//
    // getPartsParam //
    //---------------//
    public Param<List<PartData>> getPartsParam ()
    {
        return partsParam;
    }

    //------------------//
    // getPrecedingPage //
    //------------------//
    /**
     * Report the page, if any, that precedes the provided page within containing score.
     *
     * @param page the provided page
     * @return the preceding page or null
     */
    public Page getPrecedingPage (Page page)
    {
        int index = getPageIndex(page);

        if (index > 0) {
            return getPage(pageRefs.get(index - 1));
        }

        return null;
    }

    //----------------//
    // getSheetPageId //
    //----------------//
    /**
     * Report the local page ID for this score in provided sheet
     *
     * @param sheetNumber provided sheet number
     * @return ID of score page in provided sheet
     */
    public Integer getSheetPageId (int sheetNumber)
    {
        final PageRef pageRef = getPageRef(sheetNumber);

        if (pageRef != null) {
            return pageRef.localPageId;
        }

        return null;
    }

    //----------//
    // getStubs //
    //----------//
    /**
     * Report the sequence of stubs this score is made from.
     *
     * @return the list of relevant stubs
     */
    public List<SheetStub> getStubs ()
    {
        final List<SheetStub> pageStubs = new ArrayList<SheetStub>();
        final List<SheetStub> bookStubs = book.getStubs();

        for (PageRef ref : pageRefs) {
            pageStubs.add(bookStubs.get(ref.sheetNumber - 1));
        }

        return pageStubs;
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

    //---------//
    // isFirst //
    //---------//
    /**
     * Report whether the provided page is the first page in score.
     *
     * @param page the provided page
     * @return true if first
     */
    public boolean isFirst (Page page)
    {
        PageRef pageRef = getPageRef(page);

        if (pageRef != null) {
            return pageRefs.get(0) == pageRef;
        }

        return false;
    }

    //-------------//
    // isMultiPage //
    //-------------//
    /**
     * @return the multiPage
     */
    public boolean isMultiPage ()
    {
        return pageRefs.size() > 1;
    }

    //---------//
    // setBook //
    //---------//
    /**
     * @param book the book to set
     */
    public void setBook (Book book)
    {
        this.book = book;
    }

    //-------------------//
    // setDeltaMeasureId //
    //-------------------//
    public void setDeltaMeasureId (Page page,
                                   Integer deltaMeasureId)
    {
        PageRef pageRef = getPageRef(page);

        if (pageRef != null) {
            pageRef.deltaMeasureId = deltaMeasureId;
        }
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

    //-----------------//
    // beforeUnmarshal //
    //-----------------//
    @SuppressWarnings("unused")
    private void beforeUnmarshal (Unmarshaller u,
                                  Object parent)
    {
        book = (Book) parent;
    }

    //---------//
    // getPage //
    //---------//
    private Page getPage (PageRef ref)
    {
        Sheet sheet = book.getStubs().get(ref.sheetNumber - 1).getSheet();

        return sheet.getPages().get(ref.localPageId - 1);
    }

    //------------//
    // getPageRef //
    //------------//
    private PageRef getPageRef (int sheetNumber)
    {
        for (PageRef pageRef : pageRefs) {
            if (pageRef.sheetNumber == sheetNumber) {
                return pageRef;
            }
        }

        logger.error("No page ref for sheet number " + sheetNumber);

        return null;
    }

    //------------//
    // getPageRef //
    //------------//
    private PageRef getPageRef (Page page)
    {
        final int sheetNumber = page.getSheet().getStub().getNumber();

        for (PageRef pageRef : pageRefs) {
            if (pageRef.sheetNumber == sheetNumber) {
                return pageRef;
            }
        }

        logger.error("No page ref for " + page);

        return null;
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

    //---------//
    // PageRef //
    //---------//
    private static class PageRef
    {
        //~ Instance fields ------------------------------------------------------------------------

        @XmlAttribute(name = "sheet-number")
        public int sheetNumber;

        @XmlAttribute(name = "local-page-id")
        public int localPageId;

        @XmlAttribute(name = "delta-measure-id")
        public int deltaMeasureId;

        //~ Constructors ---------------------------------------------------------------------------
        public PageRef (int sheetNumber,
                        int localPageId,
                        int deltaMeasureId)
        {
            this.sheetNumber = sheetNumber;
            this.localPageId = localPageId;
            this.deltaMeasureId = deltaMeasureId;
        }

        private PageRef ()
        {
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("PageRef{");
            sb.append("sheetNumber:").append(sheetNumber);
            sb.append(" localPageId:").append(localPageId);
            sb.append(" deltaMeasureId:").append(deltaMeasureId);
            sb.append('}');

            return sb.toString();
        }
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
