//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S c o r e                                            //
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
package org.audiveris.omr.score;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.score.ui.LogicalPartsEditor;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>Score</code> represents a single movement, and is composed of one or
 * several instances of <code>Page</code> class.
 * <p>
 * The diagram below presents the roles of <code>Book</code> vs <code>Score</code>
 * and of <code>Sheet</code> vs <code>Page</code>:
 * <p>
 * The book at hand contains 3 sheets
 * (certainly because the input PDF or TIFF file contained 3 images):
 * <ol>
 * <li><code>Sheet</code> #1 begins with an indented system, which indicates the start of a movement
 * (named a <code>Score</code> by MusicXML).
 * There is no other indented system, so this <code>Sheet</code> contains a single
 * <code>Page</code>.
 * <li><code>Sheet</code> #2 exhibits an indentation for its system #3.
 * So, this indented system ends the previous score and starts a new one.
 * We have thus 2 <code>Page</code> instances in this <code>Sheet</code>.
 * <li><code>Sheet</code> #3 has no indented system and is thus composed of a single
 * <code>Page</code>
 * </ol>
 * <p>
 * To summarize, we have 2 scores in 3 sheets:
 * <ol>
 * <li><code>Score</code> #1, composed of:
 * <ol>
 * <li>single <code>Page</code> #1 of <code>Sheet</code> #1, followed by
 * <li><code>Page</code> #1 of <code>Sheet</code> #2
 * </ol>
 * <li><code>Score</code> #2, composed of:
 * <ol>
 * <li><code>Page</code> #2 of <code>Sheet</code> #2, followed by
 * <li>single <code>Page</code> #1 of <code>Sheet</code> #3
 * </ol>
 * </ol>
 * <img src="doc-files/Book-vs-Score.png" alt="Book-vs-Score UML">
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "score")
public class Score
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Score.class);

    /** Number of lines in a staff. */
    public static final int LINE_NB = 5;

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /**
     * This is the list of <code>LogicalPart</code>'s defined for the whole score.
     */
    @XmlElement(name = "logical-part")
    private List<LogicalPart> logicalParts;

    /**
     * This boolean signals the locking of logicalPart sequence.
     */
    @XmlAttribute(name = "logicals-locked")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean logicalsLocked;

    /**
     * This is the list of soft references to score pages, as seen from this score.
     */
    @XmlElement(name = "page")
    private final List<PageNumber> pageNumbers = new ArrayList<>();

    // Transient data
    //---------------

    /** Containing book. */
    @Navigable(false)
    private Book book;

    /** The specified sound volume, if any. */
    private Integer volume;

    /** The editor, if any, on score logical parts. */
    private LogicalPartsEditor logicalsEditor;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a Score.
     */
    public Score ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------------//
    // addPageNumber //
    //---------------//
    /**
     * Add a PageNumber.
     *
     * @param stubNumber id of containing sheet stub
     * @param pageRef    to add
     */
    public void addPageNumber (int stubNumber,
                               PageRef pageRef)
    {
        pageNumbers.add(new PageNumber(stubNumber, pageRef.getId()));
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

    //-------//
    // close //
    //-------//
    /**
     * Close this score instance, as well as its view if any.
     */
    public void close ()
    {
        logger.info("Closing {}", this);

        if (logicalsEditor != null) {
            logicalsEditor.getComponent().dispose();
        }
    }

    //----------//
    // contains //
    //----------//
    /**
     * Report whether this score contains the provided PageNumber.
     *
     * @param pageNumber provided page number
     * @return true if so
     */
    public boolean contains (PageNumber pageNumber)
    {
        return pageNumbers.indexOf(pageNumber) != -1;
    }

    //---------//
    // getBook //
    //---------//
    /**
     * Report the containing book for this score.
     *
     * @return the book
     */
    public Book getBook ()
    {
        return book;
    }

    //--------------------//
    // getFirstOccurrence //
    //--------------------//
    /**
     * Report the first part in this score which implements the provided logical part.
     *
     * @param logicalPart the desired logicalPart
     * @return first corresponding (physical) part, or null if not found
     */
    public PartRef getFirstOccurrence (LogicalPart logicalPart)
    {
        final int logId = logicalPart.getId();

        for (PageNumber pageNumber : pageNumbers) {
            final PageRef pageRef = pageNumber.getPageRef(book);

            for (SystemRef systemRef : pageRef.getSystems()) {
                for (PartRef partRef : systemRef.getParts()) {
                    if (partRef.getLogicalId() == logId) {
                        return partRef;
                    }
                }
            }
        }

        return null;
    }

    //--------------//
    // getFirstPage //
    //--------------//
    /**
     * Report the first Page in this score.
     *
     * @return first page
     */
    public Page getFirstPage ()
    {
        if (pageNumbers.isEmpty()) {
            return null;
        }

        return getPage(pageNumbers.get(0));
    }

    //-----------------//
    // getFirstPageRef //
    //-----------------//
    /**
     * Return the first PageRef in this score.
     *
     * @return first pageRef
     */
    public PageRef getFirstPageRef ()
    {
        if (pageNumbers.isEmpty()) {
            return null;
        }

        return pageNumbers.get(0).getPageRef(book);
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

        if (index < (pageNumbers.size() - 1)) {
            return getPage(pageNumbers.get(index + 1));
        }

        return null;
    }

    //-------//
    // getId //
    //-------//
    /**
     * The score ID is the rank, starting at 1, of this <code>score</code>
     * in the containing <code>book</code>.
     *
     * @return the score id
     */
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
        if (pageNumbers.isEmpty()) {
            return null;
        }

        return getPage(pageNumbers.get(pageNumbers.size() - 1));
    }

    //-------------------//
    // getLastPageNumber //
    //-------------------//
    /**
     * Report the last PageNumber in this score.
     *
     * @return last PageNumber or null
     */
    public PageNumber getLastPageNumber ()
    {
        if (pageNumbers.isEmpty()) {
            return null;
        }

        return pageNumbers.get(pageNumbers.size() - 1);
    }

    //----------------//
    // getLastPageRef //
    //----------------//
    /**
     * Report the last PageRef in this score.
     *
     * @return last PageRef or null
     */
    public PageRef getLastPageRef ()
    {
        final PageNumber lastNumber = getLastPageNumber();

        if (lastNumber == null) {
            return null;
        }

        return lastNumber.getPageRef(book);
    }

    //--------------------//
    // getLogicalPartById //
    //--------------------//
    /**
     * Report the LogicalPart that corresponds to the provided ID.
     *
     * @param id provided ID
     * @return corresponding LogicalPart or null if not found
     */
    public LogicalPart getLogicalPartById (int id)
    {
        if (logicalParts != null) {
            for (LogicalPart log : logicalParts) {
                if (log.getId() == id) {
                    return log;
                }
            }
        }

        return null;
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

    //-------------------//
    // getLogicalsEditor //
    //-------------------//
    /**
     * Report the LogicalPartsEditor, if any, active on this score.
     *
     * @return the logical parts editor, perhaps null
     */
    public LogicalPartsEditor getLogicalsEditor ()
    {
        return logicalsEditor;
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
        final PageNumber num = getPageNumber(page);
        int offset = 0;

        for (PageNumber pageNumber : pageNumbers) {
            if (pageNumber == num) {
                return offset;
            }

            // Beware of page with no deltaMeasureId (because its transcription failed)
            final PageRef pageRef = pageNumber.getPageRef(book);
            final Integer delta = pageRef.getDeltaMeasureId();

            if (delta != null) {
                offset += delta;
            } else {
                logger.info("No deltaMeasureId for {}", pageRef);
            }
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

        if ((index < 0) || (index >= pageNumbers.size())) {
            throw new IllegalArgumentException("No page with number " + number);
        }

        return getPage(pageNumbers.get(index));
    }

    //---------//
    // getPage //
    //---------//
    /**
     * Report the Page corresponding to the provided PageNumber.
     *
     * @param pageNumber provided page number
     * @return the corresponding page
     */
    private Page getPage (PageNumber pageNumber)
    {
        Sheet sheet = book.getStubs().get(pageNumber.sheetNumber - 1).getSheet();

        return sheet.getPages().get(pageNumber.sheetPageId - 1);
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
        return pageNumbers.size();
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
        return pageNumbers.indexOf(getPageNumber(page));
    }

    //--------------//
    // getPageIndex //
    //--------------//
    /**
     * Report index of the provided PageRef in the score sequence of pages.
     *
     * @param pageRef the provided PageRef
     * @return the page index in score, or -1 if not found
     */
    public int getPageIndex (PageRef pageRef)
    {
        return pageNumbers.indexOf(pageRef.getPageNumber());
    }

    //---------------//
    // getPageNumber //
    //---------------//
    /**
     * Report the PageNumber if any that corresponds to the provided Page in this score.
     *
     * @param page provided Page
     * @return the corresponding PageNumber or null if page is not in this score
     */
    public PageNumber getPageNumber (Page page)
    {
        final int sheetNumber = page.getSheet().getStub().getNumber();

        for (PageNumber pageNumber : pageNumbers) {
            if (pageNumber.sheetNumber == sheetNumber) {
                return pageNumber;
            }
        }

        return null;
    }

    //----------------//
    // getPageNumbers //
    //----------------//
    /**
     * Report score PageNumber's.
     *
     * @return a <b>view</b> on list of PageNumber's in this score
     */
    public List<PageNumber> getPageNumbers ()
    {
        return Collections.unmodifiableList(pageNumbers);
    }

    //------------//
    // getPageRef //
    //------------//
    /**
     * Return the PageRef in this score for a specified sheet stub.
     *
     * @param sheetNumber sheet stub number
     * @return the score page in this sheet, or null
     */
    public PageRef getPageRef (int sheetNumber)
    {
        for (PageNumber pageNumber : pageNumbers) {
            if (pageNumber.sheetNumber == sheetNumber) {
                return pageNumber.getPageRef(book);
            }
        }

        return null;
    }

    //-------------//
    // getPageRefs //
    //-------------//
    /**
     * Report the collection of PageRef's in that score, limited to the provided stubs.
     *
     * @param stubs valid selected stubs
     * @return the relevant PageRef's
     */
    public List<PageRef> getPageRefs (List<SheetStub> stubs)
    {
        final List<PageRef> relevantPageRefs = new ArrayList<>();

        for (PageNumber pageNumber : pageNumbers) {
            final SheetStub stub = book.getStub(pageNumber.sheetNumber);

            if (stubs.contains(stub)) {
                relevantPageRefs.add(pageNumber.getPageRef(book));
            }
        }

        return relevantPageRefs;
    }

    //----------//
    // getPages //
    //----------//
    /**
     * Report the list of all pages in that score.
     *
     * @return the pages list
     */
    public List<Page> getPages ()
    {
        final List<Page> pages = new ArrayList<>();

        for (PageNumber pageNumber : pageNumbers) {
            pages.add(getPage(pageNumber));
        }

        return pages;
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
            return getPage(pageNumbers.get(index - 1));
        }

        return null;
    }

    //---------------------//
    // getPrecedingPageRef //
    //---------------------//
    /**
     * Report the pageRef, if any, that precedes the provided pageRef within containing score.
     *
     * @param pageRef the provided PageRef
     * @return the preceding PageRef or null
     */
    public PageRef getPrecedingPageRef (PageRef pageRef)
    {
        int index = getPageIndex(pageRef);

        if (index > 0) {
            final PageNumber pageNumber = pageNumbers.get(index - 1);
            return pageNumber.getPageRef(book); // Perhaps null
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

    //---------//
    // getStub //
    //---------//
    /**
     * Report the stub that corresponds to the provided PageNumber.
     *
     * @param pageNumber the provided PageNumber
     * @return the corresponding stub
     */
    public SheetStub getStub (PageNumber pageNumber)
    {
        return book.getStubs().get(pageNumber.sheetNumber - 1);
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

        for (PageNumber pageNumber : pageNumbers) {
            pageStubs.add(bookStubs.get(pageNumber.sheetNumber - 1));
        }

        return pageStubs;
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

    //------------//
    // insertPage //
    //------------//
    /**
     * Insert the provided page at proper index.
     *
     * @param pageRef the reference to page
     */
    public void insertPage (PageRef pageRef)
    {
        final PageNumber pageNumber = pageRef.getPageNumber();

        int i;
        for (i = 0; i < pageNumbers.size(); i++) {
            final PageNumber p = pageNumbers.get(i);

            if (pageNumber.compareTo(p) <= 0) {
                pageNumbers.add(i, pageNumber);

                return;
            }
        }

        pageNumbers.add(i, pageNumber);
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
        final int sheetNumber = page.getSheet().getStub().getNumber();
        final PageNumber firstNumber = pageNumbers.get(0);

        return sheetNumber == firstNumber.sheetNumber;
    }

    //------------------//
    // isLogicalsLocked //
    //------------------//
    /**
     * Report whether the LogicalPart's are locked.
     *
     * @return the logicalsLocked flag
     */
    public boolean isLogicalsLocked ()
    {
        return logicalsLocked;
    }

    //-------------//
    // isMultiPage //
    //-------------//
    /**
     * Report whether this score contains several pages.
     *
     * @return true if so
     */
    public boolean isMultiPage ()
    {
        return pageNumbers.size() > 1;
    }

    //---------------//
    // mergeWithNext //
    //---------------//
    /**
     * Try to merge this score with the provided (following) one.
     *
     * @param nextScore the score to merge with
     */
    public void mergeWith (Score nextScore)
    {
        if (nextScore != this) {
            pageNumbers.addAll(nextScore.pageNumbers);

            if (!isLogicalsLocked() && nextScore.isLogicalsLocked()) {
                if (logicalParts != null) {
                    logicalParts.clear();
                } else {
                    logicalParts = new ArrayList<>();
                }

                logicalParts.addAll(nextScore.logicalParts);
                setLogicalsLocked(true);
            }

            book.getScores().remove(nextScore);
        }
    }

    //--------------------//
    // needsPartCollation //
    //--------------------//
    /**
     * Report whether this score needs parts collation.
     *
     * @return true if so
     */
    public boolean needsPartCollation ()
    {
        for (PageNumber pageNumber : pageNumbers) {
            final PageRef pageRef = pageNumber.getPageRef(book);

            for (SystemRef systemRef : pageRef.getSystems()) {
                for (PartRef partRef : systemRef.getParts()) {
                    if (partRef.getLogicalId() == null) {
                        logger.info("PartCollation needed in {}", this);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    //------------------------//
    // removeSheetPageNumbers //
    //------------------------//
    /**
     * Remove the PageNumber instances that refer to the provided sheet number.
     *
     * @param sheetNumber the sheet number to remove
     */
    public void removeSheetPageNumbers (int sheetNumber)
    {
        for (Iterator<PageNumber> it = pageNumbers.iterator(); it.hasNext();) {
            if (it.next().sheetNumber == sheetNumber) {
                it.remove();
            }
        }
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

    //-------------------//
    // setLogicalsEditor //
    //-------------------//
    /**
     * @param logicalsEditor the logicals editor to set
     */
    public void setLogicalsEditor (LogicalPartsEditor logicalsEditor)
    {
        this.logicalsEditor = logicalsEditor;
    }

    //-------------------//
    // setLogicalsLocked //
    //-------------------//
    /**
     * Lock or unlock the logicals.
     *
     * @param bool true to lock, false to unlock
     */
    public void setLogicalsLocked (boolean bool)
    {
        logicalsLocked = bool;
        logger.info("LogicalParts for {} are {}", this, bool ? "LOCKED" : "UNLOCKED");
        book.setModified(true);
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
        return "{Score " + getId() + "}";
    }

    //~ Static Methods -----------------------------------------------------------------------------

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

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Integer defaultVolume =
                new Constant.Integer("Volume", 78, "Default Volume in 0..127 range");
    }
}
