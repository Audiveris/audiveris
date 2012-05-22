//----------------------------------------------------------------------------//
//                                                                            //
//                           W o r d S c a n n e r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2012. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text;

import java.util.List;

/**
 * Class {@code Class} is an abstract scanner to retrieve words.
 *
 * @author Hervé Bitteur
 */
public abstract class WordScanner
{
    //~ Instance fields --------------------------------------------------------

    /** Precise description of each (non blank) character */
    protected List<OcrChar> chars;

    /** Position in the sequence of chars */
    protected int pos = -1;

    /** Current word and its parameters */
    private String currentWord = null;
    private int    currentWordStart = 0;
    private int    currentWordStop = 0;

    /** Next word and its parameters */
    private String nextWord = null;
    protected int  nextWordStart = -1;
    protected int  nextWordStop = -1;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // WordScanner //
    //-------------//
    /**
     * Creates a new WordScanner object.
     * @param chars DOCUMENT ME!
     */
    public WordScanner (List<OcrChar> chars)
    {
        this.chars = chars;
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getWordStart //
    //--------------//
    /** Return the start position of the current word (as returned by next) */
    public int getWordStart ()
    {
        return currentWordStart;
    }

    //-------------//
    // getWordStop //
    //-------------//
    /** Return the stop position of the current word (as returned by next) */
    public int getWordStop ()
    {
        return currentWordStop;
    }

    //---------//
    // hasNext //
    //---------//
    /** Tell whether there is a next word */
    public boolean hasNext ()
    {
        return nextWord != null;
    }

    //------//
    // next //
    //------//
    /** Make the next word current, and return it */
    public String next ()
    {
        currentWord = nextWord;
        currentWordStart = nextWordStart;
        currentWordStop = nextWordStop;
        /** Look ahead */
        nextWord = getNextWord();

        return currentWord;
    }

    //-------------//
    // getNextWord //
    //-------------//
    protected abstract String getNextWord ();

    //-----------//
    // lookAhead //
    //-----------//
    protected void lookAhead ()
    {
        nextWord = getNextWord();
    }
}
