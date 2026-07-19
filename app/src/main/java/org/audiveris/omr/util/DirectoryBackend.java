//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   D i r e c t o r y B a c k e n d                             //
//------------------------------------------------------------------------------------------------//
package org.audiveris.omr.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plain directory storage backend.
 */
public class DirectoryBackend
        implements StorageBackend
{
    @Override
    public Path open (Path path)
        throws IOException
    {
        if (!Files.isDirectory(path)) {
            throw new IOException("Not a directory: " + path);
        }
        return path;
    }

    @Override
    public Path create (Path path)
        throws IOException
    {
        Files.createDirectories(path);
        return path;
    }

    @Override
    public void closeRoot (Path root,
                            Path bookPath)
    {
        // nothing to close
    }

    @Override
    public boolean canHandle (Path path)
    {
        if (Files.exists(path)) {
            return Files.isDirectory(path);
        }
        String name = path.getFileName().toString().toLowerCase();
        return !(name.endsWith(".omr") || name.endsWith(".mxl"));
    }
}
