//-----------------------------------------------------------------------//
//                                                                       //
//                 D e f a u l t G l y p h S u b j e c t                 //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph;

import omr.util.DefaultSubject;
import omr.util.Subject;

/**
 * Class <code>DefaultGlyphSubject</code> is an implementation of the
 * specific {@link Subject} meant for {@link GlyphObserver} observers
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class DefaultGlyphSubject
    extends DefaultSubject<GlyphSubject, GlyphObserver, Glyph>
{
}
