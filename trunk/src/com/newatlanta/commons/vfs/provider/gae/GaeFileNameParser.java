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
package com.newatlanta.commons.vfs.provider.gae;

import org.apache.commons.vfs.*;
import org.apache.commons.vfs.provider.*;

/**
 * Parses GAE URIs and creates GaeFileName instances.
 *  
 * @author Vince Bonfanti <vbonfanti@gmail.com>
 */
public class GaeFileNameParser extends AbstractFileNameParser {

    private static GaeFileNameParser instance = new GaeFileNameParser();

    public static GaeFileNameParser getInstance() {
        return instance;
    }

    /**
     * This method copied from LocalFileNameParser and modified for GaeVFS. The
     * main point is to make sure we always have a path beneath the webapp root.
     */
    public FileName parseUri( VfsComponentContext context, FileName ignore, String filename )
            throws FileSystemException
    {
        StringBuffer name = new StringBuffer();

        // Extract the scheme
        String scheme = UriParser.extractScheme( filename, name );
        if ( scheme == null ) { // this should never happen
            scheme = "gae";
        }

        // Remove encoding, and adjust the separators
        UriParser.canonicalizePath( name, 0, name.length(), this );
        UriParser.fixSeparators( name );

        // Normalise the path
        FileType fileType = UriParser.normalisePath( name );
        
        // all GAE files *must* be relative to the base file, which must be the
        // webapp root (though we have no way of enforcing this); if given a uri
        // that doesn't start with the base path, add the base path
        FileObject baseFile = context.getFileSystemManager().getBaseFile();
        if ( baseFile != null ) {
            String basePath = getBasePath( baseFile );
            if( name.indexOf( basePath ) != 0 ) { // if ( !name.startsWith( basePath ) )
                name.insert( 0, basePath );
            }
        }
        return new GaeFileName( scheme, name.toString(), fileType );
    }
    
    public String getBasePath( FileObject baseFile ) {
        StringBuffer basePath = new StringBuffer();
        String scheme = UriParser.extractScheme( baseFile.getName().getURI(), basePath );
        if ( scheme.equals( "file" ) ) {
            basePath.delete( 0, 2 ); // remove leading "//"
        }
        return basePath.toString().intern();
    }
}