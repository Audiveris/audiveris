//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S h e e t S t u b                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.image.FilterDescriptor;

import omr.sheet.ui.SheetAssembly;

import omr.step.Step;
import omr.step.StepException;

import omr.util.LiveParam;

import java.util.concurrent.locks.Lock;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code SheetStub} represents a placeholder in a {@link Book} to decouple the
 * Book instance from the actual {@link Sheet} instances and avoid loading all of them
 * in memory.
 *
 * @author Hervé Bitteur
 */
@XmlJavaTypeAdapter(BasicStub.Adapter.class)
public interface SheetStub
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Close this stub, and remove it from the containing book.
     */
    void close ();

    /**
     * Create a sheet for this stub.
     *
     * @return the stub sheet just created
     */
    Sheet createSheet ();

    /**
     * An abnormal situation has been found, as detailed in provided message,
     * now how should we proceed, depending on batch mode or user answer.
     *
     * @param msg   the problem description
     * @param dummy true for a dummy (positive) decision
     * @throws StepException thrown when processing must stop
     */
    void decideOnRemoval (String msg,
                          boolean dummy)
            throws StepException;

    /**
     * In non batch mode, report the related SheetAssembly for GUI
     *
     * @return the stub UI assembly, or null in batch mode
     */
    SheetAssembly getAssembly ();

    /**
     * Report the containing book.
     *
     * @return containing book
     */
    Book getBook ();

    /**
     * Report the step being processed, if any.
     *
     * @return the current step or null
     */
    Step getCurrentStep ();

    /**
     * Report the binarization filter defined at sheet level.
     *
     * @return the filter parameter
     */
    LiveParam<FilterDescriptor> getFilterParam ();

    /**
     * Report the distinguished name for this sheet stub.
     *
     * @return sheet (stub) name
     */
    String getId ();

    /**
     * Report the OCR language(s) specification defined at sheet level.
     *
     * @return the OCR language(s) spec
     */
    LiveParam<String> getLanguageParam ();

    /**
     * Report the latest step done so far on this sheet.
     *
     * @return the latest step done, or null
     */
    Step getLatestStep ();

    /**
     * Report the lock that protects stub processing.
     *
     * @return stub processing lock
     */
    Lock getLock ();

    /**
     * Report the number string for this sheet in containing book
     *
     * @return "#n" for a multi-sheet book, "" otherwise
     */
    String getNum ();

    /**
     * Report the number for this sheet in containing book
     *
     * @return the sheet index number (1-based) in containing book
     */
    int getNumber ();

    /**
     * Make sure the sheet material is in memory.
     *
     * @return the sheet ready to use
     */
    Sheet getSheet ();

    /**
     * Report whether the stub has a sheet in memory
     *
     * @return true if sheet is present in memory
     */
    boolean hasSheet ();

    /**
     * Flag a stub as invalid (containing no music).
     */
    void invalidate ();

    /**
     * Report whether the specified step has been performed on this sheet
     *
     * @param step the step to check
     * @return true if already performed
     */
    boolean isDone (Step step);

    /**
     * Has the sheet been modified with respect to its book data?.
     *
     * @return true if modified
     */
    boolean isModified ();

    /**
     * Report whether this sheet is valid music.
     *
     * @return true if valid, false if invalid
     */
    boolean isValid ();

    /**
     * Make sure the provided step has been reached on this sheet stub
     *
     * @param step  the step to check
     * @param force if true and step already reached, stub is reset and processed until step
     * @return true if OK
     */
    boolean reachStep (Step step,
                       boolean force);

    /**
     * Reset this stub to its initial state (that is valid and non-processed).
     */
    void reset ();

    /**
     * Set the modified flag.
     *
     * @param val the new flag value
     */
    void setModified (boolean val);

    /**
     * Store sheet material into book.
     *
     * @throws Exception if storing fails
     */
    void storeSheet ()
            throws Exception;

    /**
     * Swap sheet material.
     * If modified and not discarded, sheet material will be stored before being disposed of.
     */
    void swapSheet ();
}
