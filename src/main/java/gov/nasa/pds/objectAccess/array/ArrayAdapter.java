// Copyright 2019, California Institute of Technology ("Caltech").
// U.S. Government sponsorship acknowledged.
//
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// * Redistributions of source code must retain the above copyright notice,
// this list of conditions and the following disclaimer.
// * Redistributions must reproduce the above copyright notice, this list of
// conditions and the following disclaimer in the documentation and/or other
// materials provided with the distribution.
// * Neither the name of Caltech nor its operating division, the Jet Propulsion
// Laboratory, nor the names of its contributors may be used to endorse or
// promote products derived from this software without specific prior written
// permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package gov.nasa.pds.objectAccess.array;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

/**
 * Implements a class that gives access to the elements of an array.
 */
public class ArrayAdapter {

	private int[] dimensions;
	private ElementType elementType;
	private MappedBuffer buf;
	
	/**
	 * Creates a new array adapter with given dimensions, a channel
	 * with the array data, and element data type name.
	 * 
	 * @param dimensions the array dimensions
	 * @param channel the channel object containing the array data
	 * @param elementType the elmeent type
	 */
	public ArrayAdapter(int[] dimensions, SeekableByteChannel channel, ElementType elementType) {
		this.dimensions = dimensions;
		this.elementType = elementType;
		this.buf = new MappedBuffer(channel, elementType.getSize());
	}
	
	/**
	 * Gets the size of each element.
	 * 
	 * @return the element size, in bytes
	 */
	public int getElementSize() {
		return elementType.getSize();
	}
	
	/**
	 * Gets an element of a 2-D array, as an int.
	 * 
	 * @param row the row
	 * @param column the column
	 * @return the element value, as an int
	 * @throws IOException an exception
	 */
	public int getInt(int row, int column) throws IOException {
		return getInt(new int[] {row, column});
	}
	
	/**
	 * Gets an element of a 2-D array, as a long.
	 * 
	 * @param row the row
	 * @param column the column
	 * @return the element value, as a long
	 * @throws IOException an exception
	 */
	public long getLong(int row, int column) throws IOException {
		return getLong(new int[] {row, column});
	}
	
	/**
	 * Gets an element of a 2-D array, as a double.
	 * 
	 * @param row the row
	 * @param column the column
	 * @return the element value, as a double
	 * @throws IOException an exception
	 */
	public double getDouble(int row, int column) throws IOException {
		return getDouble(new int[] {row, column});
	}
	
	/**
	 * Gets an element of a 3-D array, as an int.
	 * 
	 * @param i1 the first index
	 * @param i2 the second index
	 * @param i3 the third index
	 * @return the element value, as an int
	 * @throws IOException an exception
	 */
	public int getInt(int i1, int i2, int i3) throws IOException {
		return getInt(new int[] {i1, i2, i3});
	}
	
	/**
	 * Gets an element of a 3-D array, as a long.
	 * 
	 * @param i1 the first index
	 * @param i2 the second index
	 * @param i3 the third index
	 * @return the element value, as a long
	 * @throws IOException an exception
	 */
	public long getLong(int i1, int i2, int i3) throws IOException {
		return getLong(new int[] {i1, i2, i3});
	}
	
	/**
	 * Gets an element of a 3-D array, as a double.
	 * 
	 * @param i1 the first index
	 * @param i2 the second index
	 * @param i3 the third index
	 * @return the element value, as a double
	 * @throws IOException an exception
	 */
	public double getDouble(int i1, int i2, int i3) throws IOException {
		return getDouble(new int[] {i1, i2, i3});
	}
	
	/**
	 * Gets an array element, as an int.
	 * 
	 * @param position the indices of the element
	 * @return the value of the element, as an int
	 * @throws IOException an exception
	 */
	public int getInt(int[] position) throws IOException {
		checkDimensions(position);
		ByteBuffer buf = moveToPosition(position);
		return elementType.getAdapter().getInt(buf);
	}
	
	/**
	 * Gets an array element, as a long.
	 * 
	 * @param position the indices of the element
	 * @return the value of the element, as a long
	 * @throws IOException an exception
	 */
	public long getLong(int[] position) throws IOException {
		checkDimensions(position);
		ByteBuffer buf = moveToPosition(position);
		return elementType.getAdapter().getLong(buf);
	}
	
	/**
	 * Gets an array element, as a double.
	 * 
	 * @param position the indices of the element
	 * @return the value of the element, as a double
	 * @throws IOException an exception
	 */
	public double getDouble(int[] position) throws IOException {
		checkDimensions(position);
		ByteBuffer buf = moveToPosition(position);
		return elementType.getAdapter().getDouble(buf);
	}
	
	private ByteBuffer moveToPosition(int[] position) throws IOException {
		long index = position[0];
		
		for (int i=1; i < position.length; ++i) {
			index = index*dimensions[i] + position[i]; 
		}
		index = index * elementType.getSize();
		return buf.getBuffer(index);
	}
	
	private void checkDimensions(int[] position) {
		if (position.length != dimensions.length) {
			throw new IllegalArgumentException(
					"Array position as wrong number of dimensions: "
					+ position.length
					+ "!="
					+ dimensions.length
			);
		}
	}
	
	/**
	 * Class that provides a mechanism for buffering the given data for 
	 * optimal I/O especially for greater than 2GB sized data.
	 * 
	 * @author mcayanan
	 *
	 */
	private class MappedBuffer {
	  /** The position within the data of where the buffer is. */
	  private long startPosition;
	  /** A buffer to cache a portion of the data. */
	  private ByteBuffer cachedBuffer;
	  /** Indicates how large the buffer is. */
	  private final int BUFFER_SIZE = Integer.MAX_VALUE / 43;
	  /** The data type size. */
	  private int dataTypeSize;
	  /** The channel containing the data. */
	  private SeekableByteChannel channel;
	  
	  /**
	   * Constructor.
	   * 
	   * @param channel The channel containing the data.
	   * @param dataTypeSize The data type size. 
	   */
	  public MappedBuffer(SeekableByteChannel channel, int dataTypeSize) {
	    this.channel = channel;
	    this.dataTypeSize = dataTypeSize;
	  }
	  
	  /**
	   * Get the buffer.
	   * 
	   * @param index The position of where to get the data.
	   * @return The ByteBuffer.
	   * @throws IOException an exception
	   */
	  public ByteBuffer getBuffer(long index) throws IOException {
	    ByteBuffer buf = null;
	    if (cachedBuffer == null) {
	      buf = createNewBuffer(index);
	      cachedBuffer = buf;
	    } else {
	      buf = cachedBuffer;
	    }
	    /** 
	     * Conditions where we need a new buffer:
	     * - index less than startPosition
	     * - index greater than (startPosition + BUFFER_SIZE)
	     * 
	     */
	     if ( index < startPosition ||
	          (index + dataTypeSize) > (startPosition + BUFFER_SIZE)) {
	       buf = createNewBuffer(index);
	       cachedBuffer = buf;
	     } else {
	       int relativePosition = (int) (index - startPosition);
	       ((Buffer) buf).position(relativePosition);
	     }
	     return buf;
	  }
	  
	  /**
	   * Creates a new buffer starting from the given index.
	   * @param index The position of where to start creating the buffer.
	   * @return A ByteBuffer that starts from the given index.
	   * @throws IOException an exception
	   */
	  private ByteBuffer createNewBuffer(long index) throws IOException {
	    channel.position(index);
	    ByteBuffer buf = null;
	    if ( (channel.size() - channel.position()) < (BUFFER_SIZE) ) {
	      buf = ByteBuffer.allocate((int) (channel.size() - channel.position()));
	    } else {
	      buf = ByteBuffer.allocate(BUFFER_SIZE);
	    }
	    channel.read(buf);
	    ((Buffer) buf).flip();
	    startPosition = index;
	    return buf;
	  }
	}
}
