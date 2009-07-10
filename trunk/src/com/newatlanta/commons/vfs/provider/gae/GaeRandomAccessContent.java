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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.RandomAccessContent;
import org.apache.commons.vfs.util.RandomAccessMode;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Entity;

/**
 * The class mimics java.io.RandomAccessFile, which has this description:
 *  
 *    "Instances of this class support both reading and writing to a random access
 *  file. A random access file behaves like a large array of bytes stored in the
 *  file system. There is a kind of cursor, or index into the implied array, called
 *  the file pointer; input operations read bytes starting at the file pointer and
 *  advance the file pointer past the bytes read. If the random access file is
 *  created in read/write mode, then output operations are also available; output
 *  operations write bytes starting at the file pointer and advance the file pointer
 *  past the bytes written. Output operations that write past the current end of
 *  the implied array cause the array to be extended. The file pointer can be read
 *  by the getFilePointer method and set by the seek method.
 *  
 *    "It is generally true of all the reading routines in this class that if
 *  end-of-file is reached before the desired number of bytes has been read, an
 *  EOFException (which is a kind of IOException) is thrown. If any byte cannot
 *  be read for any reason other than end-of-file, an IOException other than
 *  EOFException is thrown. In particular, an IOException may be thrown if the
 *  stream has been closed." 
 *     
 * @author Vince Bonfanti <vbonfanti@gmail.com>
 */
public class GaeRandomAccessContent extends OutputStream implements RandomAccessContent {
 
    private static final String CONTENT_BLOB = "content-blob"; // property key
    
    private GaeFileObject fileObject;
    
    private Entity entity;  // the current content entity
    private int index;      // index of the current entity
    private boolean write;  // current entity needs to be written?
    
    private long filePointer; // absolute position within the file
    
    private byte[] buffer;  // current entity buffer
    private int offset;     // relative position within buffer
    
    private RandomAccessMode mode;
    
    private DataOutputStream dataOutput;
    private DataInputStream dataInput;
    
    static void copyContent( Entity oldEntity, Entity newEntity ) {
        newEntity.setProperty( CONTENT_BLOB, oldEntity.getProperty( CONTENT_BLOB ) );
    }
    
    public GaeRandomAccessContent( GaeFileObject gfo, RandomAccessMode m )
            throws FileSystemException        
    {
        this( gfo, m, 0 );
    }
    
    public GaeRandomAccessContent( GaeFileObject gfo, RandomAccessMode m, long pos )
            throws FileSystemException
    {
        fileObject = gfo;
        mode = m;
        seek( pos );
        
        dataOutput = new DataOutputStream( this );
        dataInput = new DataInputStream( new GaeInputStream( this ) );
    }
    
    public long getFilePointer() {
        return filePointer;
    }
    
    @Override
    public synchronized void close() throws IOException {
        flush();
        index = 0;
        filePointer = 0;
        offset = 0;
    }
    
    @Override
    public synchronized void flush() throws FileSystemException {
        if ( mode.requestWrite() && write ) {
            int fileLen = (int)length();
            // if this is the last entity for the file, only write out the actual
            // number of bytes, not the full buffer
            if ( calcEntityIndex( fileLen ) == index ) {
                // calculate EOF offset within current buffer
                int eofoffset = calcBufferOffset( fileLen );
                byte[] outbuf = new byte[ eofoffset ];
                System.arraycopy( buffer, 0, outbuf, 0, eofoffset );
                entity.setProperty( CONTENT_BLOB, new Blob( outbuf ) );
            }
            fileObject.writeContentEntity( entity );
        }
        entity = null;
        buffer = null;
        write = false;
    }

    public InputStream getInputStream() throws IOException {
        return dataInput;
    }

    public long length() {
        return fileObject.doGetContentSize();
    }

    /**
     *   "Sets the file-pointer offset, measured from the beginning of this
     * file, at which the next read or write occurs. The offset may be
     * set beyond the end of the file. Setting the offset beyond the end
     * of the file does not change the file length. The file length will
     * change only by writing after the offset has been set beyond the end
     * of the file."
     */
    public synchronized void seek( long pos ) throws FileSystemException {
        if ( filePointer == pos ) {
            return;
        }
        if ( pos < 0 ) {
            throw new IllegalArgumentException( "invalid offset: " + pos );
        }
        int newIndex = calcEntityIndex( pos );
        if ( newIndex != index ) {
            flush();
            index = newIndex;
        }
        filePointer = pos;
        offset = calcBufferOffset( filePointer );
    }
    
    /**
     * Given an absolute index within the file, calculate the entity index.
     */
    private int calcEntityIndex( long i ) {
        return (int)( i / fileObject.getBlockSize() );
    }
    
    private int calcBufferOffset( long i ) {
        return (int)( i - ( index * fileObject.getBlockSize() ) );
    }
    
    /**
     * From the java.io.RandomAccessFile.setLength() javadocs:
     * 
     *   "If the present length of the file as returned by the length method is
     * greater than the newLength argument then the file will be truncated. In this
     * case, if the file offset as returned by the getFilePointer method is greater
     * than newLength then after this method returns the offset will be equal to
     * newLength.
     * 
     *   "If the present length of the file as returned by the length method is
     * smaller than the newLength argument then the file will be extended. In this
     * case, the contents of the extended portion of the file are not defined."
     */
    public synchronized void setLength( long newLength ) throws IOException {
        if ( length() > newLength ) { // truncate
            fileObject.deleteContentEntities( calcEntityIndex( newLength ) );
            if ( filePointer > newLength ) {
                seek( newLength );
                Arrays.fill( buffer, offset, buffer.length, (byte)0 );
            }
        }
        fileObject.updateContentSize( newLength, true );
    }

    /**
     * The following two write methods are the primary methods via which all
     * other internal and external methods should write to the buffer.
     */
    @Override
    public synchronized void write( int b ) throws IOException {
        if ( !mode.requestWrite() ) {
            throw new FileSystemException( "vfs.provider/write-read-only.error" );
        }
        initBuffer();
        if ( offset >= buffer.length ) {
            extendBuffer();
        }
        buffer[ offset ] = (byte)b;
        moveFilePointer( filePointer + 1 );
    }
    
    @Override
    public synchronized void write( byte[] b, int off, int len ) throws IOException {
        if ( !mode.requestWrite() ) {
            throw new FileSystemException( "vfs.provider/write-read-only.error" );
        }
        if ( ( off < 0 ) || ( off > b.length ) || ( len < 0 ) ||
                    ( ( off + len ) > b.length ) || ( ( off + len ) < 0 ) ) {
            throw new IndexOutOfBoundsException();
        } else if ( ( b.length == 0 ) || ( len == 0 ) ) {
            return;
        }
        internalWrite( b, off, len );
    }
        
    private synchronized void internalWrite( byte[] b, int off, int len )
            throws IOException
    {
        initBuffer();
        
        long newPos = filePointer + len;
        if ( calcEntityIndex( newPos ) == index ) { // within current entity
            writeBuffer( b, off, len );
            moveFilePointer( newPos );
        } else {
            // fill the current buffer
            int bytesAvailable = buffer.length - offset;
            writeBuffer( b, off, bytesAvailable );
            moveFilePointer( filePointer + bytesAvailable );
            
            // recursively write the rest of the output
            internalWrite( b, off + bytesAvailable, len - bytesAvailable );
        }
    }
    
    private synchronized void writeBuffer( byte[] b, int off, int len ) throws FileSystemException {
        if ( ( offset + len ) > buffer.length ) {
            extendBuffer();
        }
        System.arraycopy( b, off, buffer, offset, len );
        write = true;
    }

    private synchronized void extendBuffer() {
        byte[] tempBuf = buffer;
        buffer = new byte[ Math.min( buffer.length << 1, fileObject.getBlockSize() ) ];
        System.arraycopy( tempBuf, 0, buffer, 0, tempBuf.length );
        entity.setProperty( CONTENT_BLOB, new Blob( buffer ) );
    }

    private synchronized void moveFilePointer( long newPos ) throws FileSystemException {
        fileObject.updateContentSize( newPos );
        seek( newPos );
    }
    
    private synchronized void initBuffer() throws FileSystemException {
        if ( buffer != null ) {
            return;
        }
        if ( entity == null ) {
            entity = fileObject.getContentEntity( index );
            write = false;
        }
        Blob contentBlob = (Blob)entity.getProperty( CONTENT_BLOB );
        if ( contentBlob != null ) {
            buffer = contentBlob.getBytes();
        } else {
            int blockSize = fileObject.getBlockSize();
            if ( blockSize <= ( 1024 * 64 ) ) {
                buffer = new byte[ 1024 * 8 ]; // smallest init size 8KB
            } else if ( blockSize >= ( 1024 * 256 ) ) {
                buffer = new byte[ 1024 * 32 ]; // largest init size 32KB
            } else {
                buffer = new byte[ blockSize >> 3 ]; // 1/8th block size
            }
            entity.setProperty( CONTENT_BLOB, new Blob( buffer ) );
        }
    }
    
    public void writeBoolean( boolean v ) throws IOException {
        dataOutput.writeBoolean( v );
    }

    public void writeByte( int v ) throws IOException {
        dataOutput.writeByte( v );
    }

    public void writeBytes( String s ) throws IOException {
        dataOutput.writeBytes( s );
    }

    public void writeChar( int v ) throws IOException {
        dataOutput.writeChar( v );
    }

    public void writeChars( String s ) throws IOException {
        dataOutput.writeChars( s );
    }

    public void writeDouble( double v ) throws IOException {
        dataOutput.writeDouble( v );
    }

    public void writeFloat( float v ) throws IOException {
        dataOutput.writeFloat( v );
    }

    public void writeInt( int v ) throws IOException {
        dataOutput.writeInt( v );
    }

    public void writeLong( long v ) throws IOException {
        dataOutput.writeLong( v );
    }

    public void writeShort( int v ) throws IOException {
        dataOutput.writeShort( v );
    }

    public void writeUTF( String str ) throws IOException {
        dataOutput.writeUTF( str );
    }
    
    public synchronized int read() throws IOException {
        if ( filePointer >= length() ) {
            return -1;
        }
        int c = buffer[ offset ] & 0xff;
        seek( filePointer + 1 );
        return c;
    }
    
    public synchronized int read( byte[] b, int off, int len ) throws IOException {
        if ( ( off < 0 ) || ( off > b.length ) || ( len < 0 ) ||
                ( ( off + len ) > b.length ) || ( ( off + len ) < 0 ) ) {
            throw new IndexOutOfBoundsException();
        } else if ( ( b.length == 0 ) || ( len == 0 ) ) {
            return 0;
        }
        
        long fileLen = length();
        if ( filePointer >= fileLen ) {
            return -1;
        }
        if ( filePointer + len > fileLen ) {
            len = (int)( fileLen - filePointer );
        }
        if ( len <= 0 ) {
            return 0;
        }
        return internalRead( b, off, len );
    }
    
    private synchronized int internalRead( byte[] b, int off, int len )
            throws IOException
    {
        // len is always less than or equal to the file length, so we're
        // always going to read exactly len number of bytes
        initBuffer();
        
        long newPos = filePointer + len;
        if ( calcEntityIndex( newPos ) == index ) { // within current entity
            System.arraycopy( buffer, offset, b, off, len );
            seek( newPos );
            return len; // recursive reads always end here
        } else {
            // read to the end of the current buffer
            int bytesAvailable = buffer.length - offset;
            System.arraycopy( buffer, offset, b, off, bytesAvailable );
            
            // move file pointer to beginning of next buffer
            seek( filePointer + bytesAvailable );
            
            // recursively read the rest of the input
            internalRead( b, off + bytesAvailable, len - bytesAvailable );
            return len;
        }
    }
    
    public void readFully( byte[] b ) throws IOException {
        dataInput.readFully( b );
    }
    
    public void readFully( byte[] b, int off, int len ) throws IOException {
        dataInput.readFully( b, off, len );
    }
    
    public boolean readBoolean() throws IOException {
        return dataInput.readBoolean();
    }

    public byte readByte() throws IOException {
        return dataInput.readByte();
    }

    public char readChar() throws IOException {
        return dataInput.readChar();
    }

    public double readDouble() throws IOException {
        return dataInput.readDouble();
    }

    public float readFloat() throws IOException {
        return dataInput.readFloat();
    }

    @Deprecated
    public String readLine() throws IOException {
        return dataInput.readLine();
    }
    
    public int readInt() throws IOException {
        return dataInput.readInt();
    }

    public long readLong() throws IOException {
        return dataInput.readLong();
    }

    public short readShort() throws IOException {
        return dataInput.readShort();
    }

    public String readUTF() throws IOException {
        return dataInput.readUTF();
    }

    public int readUnsignedByte() throws IOException {
        return dataInput.readUnsignedByte();
    }

    public int readUnsignedShort() throws IOException {
        return dataInput.readUnsignedShort();
    }

    public synchronized int skipBytes( int n ) throws IOException {
        long fileLen = length();
        if ( filePointer >= fileLen ) {
            return 0;
        }
        if ( filePointer + n > fileLen) {
            n = (int)( fileLen - filePointer );
        }
        if ( n <= 0 ) {
            return 0;
        }
        seek( filePointer + n );
        return n;
    }
    
    /**
     * This inner class exists because the outer class can't extend both OutputStream
     * and InputStream. This class just makes callbacks to the outer class.
     */
    private class GaeInputStream extends InputStream {

        private GaeRandomAccessContent outer;
        
        private GaeInputStream( GaeRandomAccessContent content ) {
            outer = content;
        }
        
        @Override
        public int read() throws IOException {
            return outer.read();
        }
        
        @Override
        public int read( byte[] b, int off, int len ) throws IOException {
            return outer.read( b, off, len );
        }
        
        @Override
        public long skip( long n ) throws IOException {
            return outer.skipBytes( (int)n );
        }
        
        @Override
        public void close() throws IOException {
        }
    }
}
