package org.brewchain.mcore.actuators.tokens.action721;

import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.Action721CMD;
import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.Action721Module;
import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.ReqQueryRC721TokenWho;
import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.RespQueryRC721TokenWho;
import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.TokenRC721Info;
import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.TokenRC721Ownership;
import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.TokenRC721Value;
import org.brewchain.mcore.api.ICryptoHandler;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.handler.AccountHandler;
import org.brewchain.mcore.handler.ChainHandler;
import org.brewchain.mcore.handler.MCoreServices;
import org.brewchain.mcore.handler.TransactionHandler;
import org.brewchain.mcore.model.Account.AccountInfo;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;

@NActorProvider
@Slf4j
@Data
public class QueryRC721TokenOwner extends SessionModules<ReqQueryRC721TokenWho> {
	@ActorRequire(name = "bc_chain", scope = "global")
	ChainHandler blockChainHelper;
	@ActorRequire(name = "bc_transaction", scope = "global")
	TransactionHandler transactionHandler;
	@ActorRequire(name = "bc_crypto", scope = "global")
	ICryptoHandler crypto;
	@ActorRequire(name = "bc_account", scope = "global")
	AccountHandler accountHelper;
	
	@ActorRequire(name = "MCoreServices", scope = "global")
	MCoreServices mcore;


	@Override
	public String[] getCmds() {
		return new String[] { Action721CMD.WHOS.name() };
	}

	@Override
	public String getModule() {
		return Action721Module.C21.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqQueryRC721TokenWho pb, final CompleteHandler handler) {
		RespQueryRC721TokenWho.Builder oret = RespQueryRC721TokenWho.newBuilder();
		try {
			oret.setTokenAddress(pb.getTokenAddress());
			for(ByteString tokenid:pb.getTokenIdsList()) {
				ByteString tokenidaddr = 
						ByteString.copyFrom(mcore.getCrypto().sha3(pb.getTokenAddress().concat(tokenid).toByteArray()), 0, 20);
				
				AccountInfo.Builder acct = accountHelper.getAccount(tokenidaddr);
				if (acct != null) {
					TokenRC721Value tokeninfo = TokenRC721Value.parseFrom(acct.getExtData());
					oret.addValues(tokeninfo);
				}
			}
			AccountInfo.Builder acct = accountHelper.getAccount(pb.getTokenAddress());
			if (acct != null) {
				TokenRC721Info tokeninfo = TokenRC721Info.parseFrom(acct.getExtData());
				oret.setName(tokeninfo.getName()).setSymbol(tokeninfo.getSymbol())
				.setTotalSupply(tokeninfo.getTotalSupply());
			}
			
			oret.setRetCode(1);
			handler.onFinished(PacketHelper.toPBReturn(pack, oret.build()));

		} catch (Exception e) {
			log.debug("error in create tx:", e);
			oret.clear();
			oret.setRetCode(-1);
			oret.setRetMessage(e.getMessage());
			handler.onFinished(PacketHelper.toPBReturn(pack, oret.build()));
			return;
		}
	}
}