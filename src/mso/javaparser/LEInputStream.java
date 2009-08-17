package mso.javaparser;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * byte = int8 short = int16 char = uint16 int = int32 long = int64
 */
public class LEInputStream {

	class RewindableInputStream extends InputStream {

		int[] buffer = new int[64];

		int pos;

		int bytesInBuffer;

		final LinkedList<Integer> marks = new LinkedList<Integer>();

		final InputStream input;

		RewindableInputStream(InputStream in) {
			input = in;
			pos = -1;
		}

		@Override
		public int read() throws IOException {
			if (pos < 0)
				// no mark is set and no data in the buffer
				return input.read();

			if (pos == bytesInBuffer) {
				if (marks.size() == 0) {
					// buffer is depleted, going back to passing data directly
					pos = -1;
					return input.read();
				}
				if (pos == buffer.length) {
					buffer = Arrays.copyOf(buffer, 2 * buffer.length);
				}
				buffer[pos] = input.read();
				bytesInBuffer++;
			}

			return buffer[pos++];
		}

		public Integer setMark() {
			Integer mark;
			if (marks.size() == 0) {
				mark = pos = bytesInBuffer = 0;
			} else {
				mark = pos;
			}
			marks.add(mark);
			return mark;
		}

		public void releaseMark(Object mark) {
			Integer m = marks.removeLast();
			if (m != mark) {
				throw new Error("Logic error: mark was not set.");
			}
		}

		public void rewind(Object mark) throws IOException {
			Integer m = marks.getLast();
			if (m != mark) {
				System.out.println("marks size:" + marks.size());
				throw new Error("Logic error: mark was not set. " + m + " "
						+ mark);
			}
			pos = m;
		}

	}

	final RewindableInputStream rewindableInputStream;
	final DataInputStream input;

	private class Mark {
		final public Integer pos;

		Mark(Integer p) {
			pos = p;
		}
	}

	int bitfield;
	int bitfieldpos;

	public LEInputStream(InputStream i) {
		rewindableInputStream = new RewindableInputStream(i);
		input = new DataInputStream(rewindableInputStream);
		bitfieldpos = -1;
	}

	public Mark setMark() {
		return new Mark(rewindableInputStream.setMark());
	}

	public void releaseMark(Object mark) {
		Mark m = (Mark) mark;
		rewindableInputStream.releaseMark(m.pos);
	}

	public int distanceFromMark(Object mark) {
		Mark m = (Mark) mark;
		return rewindableInputStream.pos - m.pos;
	}

	public void rewind(Object mark) throws IOException {
		rewindableInputStream.rewind(((Mark) mark).pos);
	}

	public boolean atEnd() throws IOException {
		return input.available() == 0;
	}

	private int getBits(int n) throws IOException {
		if (bitfieldpos < 0) {
			bitfield = Short.reverseBytes(input.readShort());
			bitfieldpos = 0;
		}
		int v = bitfield >> bitfieldpos;
		bitfieldpos += n;
		if (bitfieldpos == 16) {
			bitfieldpos = -1;
		} else if (bitfieldpos > 16) {
			throw new IOException("bifield does not have enough bits left");
		}
		return v;
	}

	private void checkForLeftOverBits() throws IOException {
		if (bitfieldpos >= 0)
			throw new IOException(
					"Cannot read this type halfway through a bit operation.");
	}

	public boolean readbit() throws IOException {
		int v = getBits(1) & 1;
		return v == 1;
	}

	public byte readuint2() throws IOException {
		int v = getBits(2) & 3;
		return (byte) v;
	}

	public byte readuint3() throws IOException {
		int v = getBits(3) & 0x7;
		return (byte) v;
	}

	public byte readuint4() throws IOException {
		int v = getBits(4) & 0xF;
		return (byte) v;
	}

	public byte readuint5() throws IOException {
		int v = getBits(5) & 0x1F;
		return (byte) v;
	}

	public byte readuint6() throws IOException {
		int v = getBits(6) & 0x3F;
		return (byte) v;
	}

	public short readuint12() throws IOException {
		int v = getBits(12) & 0xFFF;
		return (short) v;
	}

	public short readuint14() throws IOException {
		int v = getBits(14) & 0x3FFF;
		return (short) v;
	}

	public byte readuint8() throws IOException {
		checkForLeftOverBits();
		return input.readByte();
	}

	public short readint16() throws IOException {
		checkForLeftOverBits();
		return Short.reverseBytes(input.readShort());
	}

	public int readuint16() throws IOException {
		checkForLeftOverBits();
		int s = Short.reverseBytes(input.readShort());
		if (s < 0)
			s = 0x10000 + s;
		return s;
	}

	public int readuint32() throws IOException {
		checkForLeftOverBits();
		return Integer.reverseBytes(input.readInt());
	}

	public int readint32() throws IOException {
		checkForLeftOverBits();
		return Integer.reverseBytes(input.readInt());
	}
}
