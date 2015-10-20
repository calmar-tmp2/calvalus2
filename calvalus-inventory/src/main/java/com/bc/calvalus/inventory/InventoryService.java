package com.bc.calvalus.inventory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * The interface to the Calvalus inventory.
 *
 * @author MarcoZ
 * @author Norman
 */
public interface InventoryService {

    /**
     * Gets pre-defined product sets.
     *
     * @param filter A filter expression (unused)
     *
     * @return The array product sets, which may be empty.
     *
     * @throws java.io.IOException If an I/O error occurs
     */
    ProductSet[] getProductSets(String username, String filter) throws Exception;

    /**
     * Globs the given path pattern list against the inventory service's file system.
     * <p/>
     * <i>TODO: Use Unix file system wildcards instead (nf, 2011-09-09). See {@link AbstractInventoryService#getRegexpForPathGlob(String)}. </i>
     *
     * @param pathPatterns A list of relative or absolute data paths which may contain regular expressions.
     *
     * @return An array of fully qualified URIs comprising the filesystem and absolute data input path.
     *
     * @throws java.io.IOException If an I/O error occurs
     */
    String[] globPaths(String username, List<String> pathPatterns) throws IOException;

    /**
     * @param path A relative or absolute path.
     *
     * @return A fully qualified URI comprising the filesystem and absolute data output path.
     */
    String getQualifiedPath(String username, String path) throws IOException;

    /**
     * Adds a file to the inventory (creates necessary directories) and returns an {@link OutputStream output stream} for writing data into it.
     *
     * @param userPath the path to the file to be created.
     *
     * @return the {@link OutputStream output stream} for writing.
     *
     * @throws IOException If an I/O error occurs
     */
    OutputStream addFile(String username, String userPath) throws IOException;

    /**
     * Deletes the file specified by the given path.
     *
     * @param userPath the path to the file to be deleted.
     *
     * @return {@code true}, if the deletion was successful, otherwise {@code false}.
     *
     * @throws IOException If an I/O error occurs
     */
    boolean removeFile(String username, String userPath) throws IOException;

    /**
     * Deletes the directory specified by the given path.
     *
     * @param userPath the path to the directory to be deleted.
     *
     * @return {@code true}, if the deletion was successful, otherwise {@code false}.
     *
     * @throws IOException If an I/O error occurs
     */
    boolean removeDirectory(String username, String userPath) throws IOException;

    /**
     * Provides the information if the given path exists.
     *
     * @param path    the path to check.
     * @return {@code true}, if the path exists, otherwise {@code false}.
     * @throws IOException If an I/O error occurs
     */
    boolean pathExists(String path) throws IOException;
}
