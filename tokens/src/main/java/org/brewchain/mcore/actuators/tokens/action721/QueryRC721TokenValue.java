package org.brewchain.mcore.actuators.tokens.action721;

import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.Action721CMD;
import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.Action721Module;
import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.ReqQueryRC721TokenValue;
import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.RespQueryRC721TokenValue;
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
public class QueryRC721TokenValue extends SessionModules<ReqQueryRC721TokenValue> {
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
		return new String[] { Action721CMD.QVALUE.name() };
	}

	@Override
	public String getModule() {
		return Action721Module.C21.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqQueryRC721TokenValue pb, final CompleteHandler handler) {
		RespQueryRC721TokenValue.Builder oret = RespQueryRC721TokenValue.newBuilder();
		try {
			AccountInfo.Builder acct = accountHelper.getAccount(pb.getOwnerAddress());
			if (acct != null) {
				AccountInfo.Builder tokenAcct = accountHelper.getAccount(pb.getTokenAddress());

				TokenRC721Info tokeninfo = TokenRC721Info.parseFrom(tokenAcct.getExtData());

				AccountInfoWrapper account = new AccountInfoWrapper(acct);
				account.loadStorageTrie(mcore.getStateTrie());
				byte tokendata[]=account.getStorage(pb.getTokenAddress().toByteArray());
				if(tokendata!=null) {
					oret.setValue(TokenRC721Ownership.parseFrom(tokendata));
				}else {
					oret.setValue(TokenRC721Ownership.newBuilder());
				}
				oret.setName(tokeninfo.getName()).setSymbol(tokeninfo.getSymbol())
				.setTotalSupply(tokeninfo.getTotalSupply());
				oret.setTokenAddress(pb.getTokenAddress());
			}
			oret.setRetCode(1);
		} catch (Exception e) {
			log.debug("error in create tx:", e);
			oret.clear();
			oret.setRetCode(-1);
			oret.setRetMessage(e.getMessage());
			handler.onFinished(PacketHelper.toPBReturn(pack, oret.build()));
			return;
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oret.build()));
	}
}