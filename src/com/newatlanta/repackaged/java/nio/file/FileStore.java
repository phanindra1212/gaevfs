/*
 * Copyright 2007-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.newatlanta.repackaged.java.nio.file;

import java.io.IOException;

import com.newatlanta.repackaged.java.nio.file.attribute.*;

/**
 * Storage for files. A {@code FileStore} represents a storage pool, device,
 * partition, volume, concrete file system or other implementation specific means
 * of file storage. The {@code FileStore} for where a file is stored is obtained
 * by invoking the {@link Path#getFileStore getFileStore} method, or all file
 * stores can be enumerated by invoking the {@link FileSystem#getFileStores
 * getFileStores} method.
 *
 * <p> In addition to the methods defined by this class, a file store may support
 * one or more {@link FileStoreAttributeView FileStoreAttributeView} classes
 * that provide a read-only or updatable view of a set of file store attributes.
 * File stores associated with the default provider support the {@link
 * FileStoreSpaceAttributeView} to read the space related attributes of the
 * file store.
 *
 * @since 1.7
 */

public abstract class FileStore {

    /**
     * Initializes a new instance of this class.
     */
    protected FileStore() {
    }

    /**
     * Returns the name of this file store. The format of the name is highly
     * implementation specific. It will typically be the name of the storage
     * pool or volume.
     *
     * <p> The string returned by this method may differ from the string
     * returned by the {@link Object#toString() toString} method.
     *
     * @return  the name of this file store
     */
    public abstract String name();

    /**
     * Returns the <em>type</em> of this file store. The format of the string
     * returned by this method is highly implementation specific. It may
     * indicate, for example, the format used or if the file store is local
     * or remote.
     *
     * @return  a string representing the type of this file store
     */
    public abstract String type();

    /**
     * Tells whether this file store is read-only. A file store is read-only if
     * it does not support write operations or other changes to files. Any
     * attempt to create a file, open an existing file for writing etc. causes
     * an {@code IOException} to be thrown.
     *
     * @return  {@code true} if, and only if, this file store is read-only
     */
    public abstract boolean isReadOnly();

    /**
     * Tells whether or not this file store supports the file attributes
     * identified by the given file attribute view.
     *
     * <p> Invoking this method to test if the file store supports {@link
     * BasicFileAttributeView} will always return {@code true}. In the case of
     * the default provider, this method cannot guarantee to give the correct
     * result when the file store is not a local storage device. The reasons for
     * this are implementation specific and therefore unspecified.
     *
     * @param   type
     *          the file attribute view type
     *
     * @return  {@code true} if, and only if, the file attribute view is
     *          supported
     */
    public abstract boolean supportsFileAttributeView(Class<? extends FileAttributeView> type);

    /**
     * Tells whether or not this file store supports the file attributes
     * identified by the given file attribute view.
     *
     * <p> Invoking this method to test if the file store supports {@link
     * BasicFileAttributeView}, identified by the name "{@code basic}" will
     * always return {@code true}. In the case of the default provider, this
     * method cannot guarantee to give the correct result when the file store is
     * not a local storage device. The reasons for this are implementation
     * specific and therefore unspecified.
     *
     * @param   name
     *          the {@link FileAttributeView#name name} of file attribute view
     *
     * @return  {@code true} if, and only if, the file attribute view is
     *          supported
     */
    public abstract boolean supportsFileAttributeView(String name);

    /**
     * Returns a {@code FileStoreAttributeView} of the given type.
     *
     * <p> This method is intended to be used where the file store attribute
     * view defines type-safe methods to read or update the file store attributes.
     * The {@code type} parameter is the type of the attribute view required and
     * the method returns an instance of that type if supported.
     *
     * <p> For {@code FileStore} objects created by the default provider, then
     * the file stores support the {@link FileStoreSpaceAttributeView} that
     * provides access to space attributes. In that case invoking this method
     * with a parameter value of {@code FileStoreSpaceAttributeView.class} will
     * always return an instance of that class.
     *
     * @param   type
     *          the {@code Class} object corresponding to the attribute view
     *
     * @return  a file store attribute view of the specified type or
     *          {@code null} if the attribute view is not available
     */
    public abstract <V extends FileStoreAttributeView> V
        getFileStoreAttributeView(Class<V> type);

    /**
     * Reads the value of a file store attribute.
     *
     * <p> The {@code attribute} parameter identifies the attribute to be read
     * and takes the form:
     * <blockquote>
     * <i>view-name</i><b>:</b><i>attribute-name</i>
     * </blockquote>
     * where the character {@code ':'} stands for itself.
     *
     * <p> <i>view-name</i> is the {@link FileStoreAttributeView#name name} of
     * a {@link FileStore AttributeView} that identifies a set of file attributes.
     * <i>attribute-name</i> is the name of the attribute.
     *
     * <p> For {@code FileStore} objects created by the default provider, then
     * the file stores support the {@link FileStoreSpaceAttributeView} that
     * provides access to space attributes.
     *
     * <p> <b>Usage Example:</b>
     * Suppose we want to know if ZFS compression is enabled (assuming the "zfs"
     * view is supported):
     * <pre>
     *    boolean compression = (Boolean)fs.getAttribute("zfs:compression");
     * </pre>
     *
     * @param   attribute
     *          the attribute to read

     * @return  the attribute value; {@code null} may be a valid valid for some
     *          attributes
     *
     * @throws  UnsupportedOperationException
     *          if the attribute view is not available or it does not support
     *          reading the attribute
     * @throws  IOException
     *          if an I/O error occurs
     */
    public abstract Object getAttribute(String attribute) throws IOException;
}
