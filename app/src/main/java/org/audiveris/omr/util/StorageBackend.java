//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S t o r a g e B a c k e n d                                 //
//------------------------------------------------------------------------------------------------//
package org.audiveris.omr.util;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Abstraction for opening/creating book storage.
 * Implementations handle ZIP archives or plain directories.
 */
public interface StorageBackend
{
    Path open (Path path) throws IOException;

    Path create (Path path) throws IOException;

    void closeRoot (Path root,
                    Path bookPath)
        throws IOException;

    boolean canHandle (Path path);
}
