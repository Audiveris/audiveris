//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      Z i p B a c k e n d                                      //
//------------------------------------------------------------------------------------------------//
package org.audiveris.omr.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * ZIP-file storage backend.
 */
public class ZipBackend
        implements StorageBackend
{
    @Override
    public Path open (Path path)
        throws IOException
    {
        URI uri = URI.create("jar:" + path.toUri());
        Map<String, String> env = new HashMap<>();
        FileSystem fs = FileSystems.newFileSystem(uri, env);
        return fs.getPath("/");
    }

    @Override
    public Path create (Path path)
        throws IOException
    {
        Files.deleteIfExists(path);
        Files.createDirectories(path.getParent());
        URI uri = URI.create("jar:" + path.toUri());
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        FileSystem fs = FileSystems.newFileSystem(uri, env);
        return fs.getPath("/");
    }

    @Override
    public void closeRoot (Path root,
                            Path bookPath)
        throws IOException
    {
        if (root != null) {
            FileSystem fs = root.getFileSystem();
            if (fs.isOpen()) {
                fs.close();
            }
        }
    }

    @Override
    public boolean canHandle (Path path)
    {
        if (Files.exists(path)) {
            return !Files.isDirectory(path);
        }
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".omr") || name.endsWith(".mxl");
    }
}
