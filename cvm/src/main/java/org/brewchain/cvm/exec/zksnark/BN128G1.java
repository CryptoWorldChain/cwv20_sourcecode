package org.brewchain.cvm.exec.zksnark;

public class BN128G1 extends BN128Fp {

	BN128G1(BN128<Fp> p) {
		super(p.x, p.y, p.z);
	}

	@Override
	public BN128G1 toAffine() {
		return new BN128G1(super.toAffine());
	}

	/**
	 * 
	 * Checks whether point is a member of subgroup, returns a point if check has
	 * been passed and null otherwise
	 */
	public static BN128G1 create(byte[] x, byte[] y) {

		BN128<Fp> p = BN128Fp.create(x, y);

		if (p == null)
			return null;

		if (!isGroupMember(p))
			return null;

		return new BN128G1(p);
	}

	/**
	 * Formally we have to do this check but in our domain it's not necessary, thus
	 * always return true
	 */
	private static boolean isGroupMember(BN128<Fp> p) {
		return true;
	}
}
