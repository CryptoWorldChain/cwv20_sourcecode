package org.brewchain.core.crypto.cwv.util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * 
 * @author brew
 *
 */
public class EndianHelper {

	public static byte[] revert(byte[] bb) {
		if (bb.length % 2 == 0) {
			byte[] newbb = new byte[bb.length];
			int size = bb.length;
			for (int i = 0; i < bb.length / 2; i ++) {
//				byte b1 = bb[i];
				newbb[i] = bb[size - 1 - i];
				newbb[size - 1 - i] = bb[i];
			}
			return newbb;
		} else {
			byte[] newbb = new byte[bb.length + 1];
			newbb[0] = 0;
			System.arraycopy(bb, 0, newbb, 1, bb.length);
			return revert(newbb);
		}
	}

	public static void main(String[] args) {
		try {
			String pri = "fbf721189fd7dec5e56db22d9c531ba101802e4dd477b5692434c17a0ca4de04";
			String test = "04dea40c7ac1342469b577d44d2e8001a11b539c2db26de5c5ded79f1821f7fb";
			byte[] a = Hex.decodeHex(pri.toCharArray());
			System.out.println("aa=" + Hex.encodeHexString(a));
			byte b[] = revert(a);
			String res = Hex.encodeHexString(a);
			System.out.println("rev=" + res);
			System.out.println("equal==" + test.compareTo(res));
		} catch (DecoderException e) {
			e.printStackTrace();
		}

	}
}
