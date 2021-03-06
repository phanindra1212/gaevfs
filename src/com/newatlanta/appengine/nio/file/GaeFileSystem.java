/*
 * Copyright 2009 New Atlanta Communications, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.newatlanta.appengine.nio.file;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.newatlanta.appengine.nio.file.attribute.GaeFileAttributes;
import com.newatlanta.repackaged.java.nio.file.FileStore;
import com.newatlanta.repackaged.java.nio.file.FileSystem;
import com.newatlanta.repackaged.java.nio.file.Path;
import com.newatlanta.repackaged.java.nio.file.PathMatcher;
import com.newatlanta.repackaged.java.nio.file.WatchService;
import com.newatlanta.repackaged.java.nio.file.attribute.UserPrincipalLookupService;
import com.newatlanta.repackaged.java.nio.file.spi.FileSystemProvider;

public class GaeFileSystem extends FileSystem {

    private FileSystemProvider provider;
    
    public GaeFileSystem( FileSystemProvider provider ) {
        this.provider = provider;
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        List<FileStore> fileStores = new ArrayList<FileStore>();
        fileStores.add( GaeFileStore.getInstance() );
        return fileStores;
    }

    @Override
    public Path getPath( String path ) {
        return new GaePath( this, path );
    }

    @Override
    public PathMatcher getPathMatcher( String syntaxAndPattern ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSeparator() {
        return java.io.File.separator;
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOpen() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return GaeFileAttributes.supportedFileAttributeViews();
    }
}
