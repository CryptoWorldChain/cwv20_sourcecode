package org.brewchain.mcore.actuators.tokens.impl721;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.TokenRC721Value;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.model.Account.AccountInfo;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class RC721ValueWrapper extends AccountInfoWrapper {

	TokenRC721Value.Builder tokenvalue;

	public RC721ValueWrapper(AccountInfo.Builder info) {
		super(info);
		try {
			if (!info.getExtData().isEmpty()) {
				tokenvalue = TokenRC721Value.newBuilder().mergeFrom(info.getExtData());
			} else {
				tokenvalue = TokenRC721Value.newBuilder();
			}
		} catch (Exception e) {
		}

	}

	@Override
	public synchronized AccountInfo build(long blocknumber) {
		getInfo().setExtData(tokenvalue.build().toByteString());
		return super.build(blocknumber);
	}

	public synchronized boolean changeOwner(Token721SubAccount from, Token721SubAccount to, ByteString tokenid) {
		if (from != null && from.getAccount().getInfo().getAddress().equals(tokenvalue.getOwnerAddr())) {
			tokenvalue.setOwnerAddr(to.getAccount().getInfo().getAddress());
			int new_token_index = to.addAndGet(BigInteger.ONE).subtract(BigInteger.ONE).intValue();
			// 给新人增加tokenid
			{
				byte[] hashindex = ByteBuffer.allocate(8).putLong(new_token_index).array();
				ByteString tokenByIndexAddr = to.tokenAddr.concat(ByteString.copyFrom(hashindex));
				try {
					to.getAccount().putStorage(tokenByIndexAddr.toByteArray(), tokenid.toByteArray());
				} catch (Exception e) {
					log.error("error in put storage", e);
				}
			}
			tokenvalue.clearTransferExtData();
			tokenvalue.setOwnerIndex(new_token_index);
			tokenvalue.setNonce(tokenvalue.getNonce() + 1);
			return true;
		} else {
			return false;
		}
	}

	public synchronized boolean burn(Token721SubAccount from, ByteString tokenid) {
		if (from != null && from.getAccount().getInfo().getAddress().equals(tokenvalue.getOwnerAddr())) {
			tokenvalue.setOwnerAddr(ByteString.copyFrom(new byte[] { 0x00 }));
			tokenvalue.setContractIndex(-1);
			tokenvalue.setOwnerIndex(-1);
			from.addAndGet(BigInteger.ONE.negate());
			tokenvalue.setNonce(tokenvalue.getNonce() + 1);
			return true;
		} else {
			return false;
		}
	}

	public synchronized void changeOwnerIndex(int newindex) {
		tokenvalue.setOwnerIndex(newindex);
	}

}
