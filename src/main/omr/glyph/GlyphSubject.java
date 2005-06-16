//-----------------------------------------------------------------------//
//                                                                       //
//                        G l y p h S u b j e c t                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph;

import omr.util.Subject;

/**
 * Interface <code>GlyphSubject</code> is a specific {@link Subject} meant
 * for {@link GlyphObserver} observers
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public interface GlyphSubject
    extends Subject<GlyphSubject, GlyphObserver, Glyph>
{
}
