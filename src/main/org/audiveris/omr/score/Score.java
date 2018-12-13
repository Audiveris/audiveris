//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S c o r e                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.score;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.param.Param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
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

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Score.class);

    /** Number of lines in a staff. */
    public static final int LINE_NB = 5;

    // Persistent data
    //----------------
    //
    /**
     * Score id, within containing book.
     * see {@link #getId()}.
     */
    /** LogicalPart list for the whole score. */
    @XmlElement(name = "logical-part")
    private List<LogicalPart> logicalParts;

    /** Pages links. */
    @XmlElement(name = "page")
    private final List<PageLink> pageLinks = new ArrayList<>();

    // Transient data
    //---------------
    //
    /** Containing book. */
    @Navigable(false)
    private Book book;

    /** Page references. */
    private ArrayList<PageRef> pageRefs = new ArrayList<>();

    /** Referenced pages. */
    private ArrayList<Page> pages;

    /** Handling of parts name and program. */
    private final Param<List<PartData>> partsParam = new PartsParam();

    /** Handling of tempo parameter. */
    private final Param<Integer> tempoParam = new Param<>();

    /** The specified sound volume, if any. */
    private Integer volume;

    /**
     * Create a Score.
     */
    public Score ()
    {
        tempoParam.setParent(Tempo.defaultTempo);
    }

    //------------//
    // addPageRef //
    //------------//
    /**
     * Add a PageRef.
     *
     * @param stubNumber id of containing sheet stub
     * @param pageRef    to add
     */
    public void addPageRef (int stubNumber,
                            PageRef pageRef)
    {
        pageRefs.add(pageRef);
        pageLinks.add(new PageLink(stubNumber, pageRef.getId()));
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

    /**
     * Report the containing book for this score
     *
     * @return the book
     */
    public Book getBook ()
    {
        return book;
    }

    //---------//
    // setBook //
    //---------//
    /**
     * Assign the containing book.
     *
     * @param book the book to set
     */
    public void setBook (Book book)
    {
        this.book = book;
    }

    //--------------//
    // getFirstPage //
    //--------------//
    /**
     * Report the first page in this score
     *
     * @return first page
     */
    public Page getFirstPage ()
    {
        if (pageRefs.isEmpty()) {
            return null;
        }

        return getPage(pageRefs.get(0));
    }

    //-----------------//
    // getFirstPageRef //
    //-----------------//
    /**
     * Return the first PageRef in this score
     *
     * @return first pageRef
     */
    public PageRef getFirstPageRef ()
    {
        if (pageRefs.isEmpty()) {
            return null;
        }

        return pageRefs.get(0);
    }

    //------------------//
    // getFollowingPage //
    //------------------//
    /**
     * Report the page, if any, that follows the provided page within containing score.
     *
     * @param page the provided page
     * @return the following page or null
     */
    public Page getFollowingPage (Page page)
    {
        int index = getPageIndex(page);

        if (index < (pageRefs.size() - 1)) {
            return getPage(pageRefs.get(index + 1));
        }

        return null;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the score ID, if any
     *
     * @return the id, or null
     */
    @XmlAttribute
    public Integer getId ()
    {
        final int index = book.getScores().indexOf(this);

        if (index != -1) {
            return 1 + index;
        }

        return null;
    }

    //-------------//
    // getLastPage //
    //-------------//
    /**
     * Report the last page in this score.
     *
     * @return last page
     */
    public Page getLastPage ()
    {
        if (pageRefs.isEmpty()) {
            return null;
        }

        return getPage(pageRefs.get(pageRefs.size() - 1));
    }

    //----------------//
    // getLastPageRef //
    //----------------//
    /**
     * Report the last PageRef in this score.
     *
     * @return last pageRef
     */
    public PageRef getLastPageRef ()
    {
        if (pageRefs.isEmpty()) {
            return null;
        }

        return pageRefs.get(pageRefs.size() - 1);
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

            offset += pageRef.getDeltaMeasureId();
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

    //------------//
    // getPageRef //
    //------------//
    /**
     * Return the score pageRef for a specified sheet stub.
     *
     * @param sheetNumber sheet stub number
     * @return the score page in this sheet, or null
     */
    public PageRef getPageRef (int sheetNumber)
    {
        for (PageRef pageRef : pageRefs) {
            if (pageRef.getSheetNumber() == sheetNumber) {
                return pageRef;
            }
        }

        return null;
    }

    //-------------//
    // getPageRefs //
    //-------------//
    /**
     * Report the sequence of PageRef instances for this score.
     *
     * @return sequence of PageRef's
     */
    public List<PageRef> getPageRefs ()
    {
        return Collections.unmodifiableList(pageRefs);
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
            pages = new ArrayList<>();

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
    /**
     * Report the sequence of parts parameters.
     *
     * @return sequence of parts parameters (name, midi program)
     */
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
            return pageRef.getId();
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
        final List<SheetStub> pageStubs = new ArrayList<>();
        final List<SheetStub> bookStubs = book.getStubs();

        for (PageRef ref : pageRefs) {
            pageStubs.add(bookStubs.get(ref.getSheetNumber() - 1));
        }

        return pageStubs;
    }

    //----------------//
    // getTempoParam //
    //---------------//
    /**
     * Report the tempo parameter.
     *
     * @return tempo information
     */
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

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{Score " + getId() + "}";
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller u,
                                 Object parent)
    {
        for (PageLink pageLink : pageLinks) {
            SheetStub stub = book.getStub(pageLink.sheetNumber);

            if (pageLink.sheetPageId > 0) {
                if (stub.getPageRefs().size() >= pageLink.sheetPageId) {
                    pageRefs.add(stub.getPageRefs().get(pageLink.sheetPageId - 1));
                } else {
                    logger.info("Missing pages in {}", stub);
                }
            } else {
                logger.info("Illegal pageLink.sheetPageId: {}", pageLink.sheetPageId);
            }
        }
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
        Sheet sheet = book.getStubs().get(ref.getSheetNumber() - 1).getSheet();

        return sheet.getPages().get(ref.getId() - 1);
    }

    //------------//
    // getPageRef //
    //------------//
    private PageRef getPageRef (Page page)
    {
        final int sheetNumber = page.getSheet().getStub().getNumber();

        for (PageRef pageRef : pageRefs) {
            if (pageRef.getSheetNumber() == sheetNumber) {
                return pageRef;
            }
        }

        logger.error("No page ref for " + page);

        return null;
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

    //------------//
    // PartsParam //
    //------------//
    private class PartsParam
            extends Param<List<PartData>>
    {

        @Override
        public List<PartData> getSpecific ()
        {
            List<LogicalPart> list = getLogicalParts();
            if (list != null) {
                List<PartData> data = new ArrayList<>();
                for (LogicalPart logicalPart : list) {
                    // Initial setting for part midi program
                    int prog = (logicalPart.getMidiProgram() != null) ? logicalPart.getMidiProgram()
                            : logicalPart.getDefaultProgram();

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

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Integer defaultTempo = new Constant.Integer(
                "QuartersPerMn",
                120,
                "Default tempo, stated in number of quarters per minute");

        private final Constant.Integer defaultVolume = new Constant.Integer(
                "Volume",
                78,
                "Default Volume in 0..127 range");
    }

    //----------//
    // PageLink //
    //----------//
    private static class PageLink
    {

        @XmlAttribute(name = "sheet-number")
        public final int sheetNumber;

        @XmlAttribute(name = "sheet-page-id")
        public final int sheetPageId;

        PageLink (int sheetNumber,
                  int sheetPageId)
        {
            this.sheetNumber = sheetNumber;
            this.sheetPageId = sheetPageId;
        }

        private PageLink ()
        {
            this.sheetNumber = 0;
            this.sheetPageId = 0;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("PageLink{");
            sb.append("sheetNumber:").append(sheetNumber);
            sb.append(" sheetPageId:").append(sheetPageId);
            sb.append('}');

            return sb.toString();
        }
    }

}
