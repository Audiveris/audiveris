//----------------------------------------------------------------------------//
//                                                                            //
//                           S c o r e F o r m a t                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.constant.Constant;
import omr.constant.ConstantSet;

/**
 * Class <code>ScoreFormat</code> defines an enumeration of all possible formats
 * for score files.
 */
public enum ScoreFormat
{
    /**
     * Binary format, used with plain Java (de)serialization
     */
    BINARY("Binary", ".score"),

    /**
     * XML ASCII format, used with an XML mapper
     */
    XML("Xml", ".xml");

    /**
     * Readable name of the format
     */
    public final String name;

    /**
     * File extension used for file name
     */
    public final String extension;

    /**
     * The name of the default folder, where scores should be stored.
     */
    public final Constant.String folder;

    //-------------//
    // ScoreFormat //
    //-------------//
    /**
     * Definition of a score format
     *
     * @param name user name for the format
     * @param extension related file extension
     */
    ScoreFormat (String name,
                 String extension)
    {
        this.name = name;
        this.extension = extension;

        folder = new Constant.String(
            getClass().getName(),
            "score" + name + "Folder",
            "c:/",
            "Default directory for " + name + " score files");
    }
}
