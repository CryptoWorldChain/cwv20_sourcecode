package org.brewchain.mcore.actuators.tokens.action20;

import org.brewchain.mcore.actuators.tokencontracts20.TokensContract20.ActionCMD;
import org.brewchain.mcore.actuators.tokencontracts20.TokensContract20.ActionModule;
import org.brewchain.mcore.actuators.tokencontracts20.TokensContract20.ReqQueryRC20TokenInfo;
import org.brewchain.mcore.actuators.tokencontracts20.TokensContract20.RespQueryRC20TokenInfo;
import org.brewchain.mcore.actuators.tokencontracts20.TokensContract20.TokenRC20Info;
import org.brewchain.mcore.api.ICryptoHandler;
import org.brewchain.mcore.handler.AccountHandler;
import org.brewchain.mcore.handler.ChainHandler;
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
public class QueryRC20TokenInfo extends SessionModules<ReqQueryRC20TokenInfo> {
	@ActorRequire(name = "bc_chain", scope = "global")
	ChainHandler blockChainHelper;
	@ActorRequire(name = "bc_transaction", scope = "global")
	TransactionHandler transactionHandler;
	@ActorRequire(name = "bc_crypto", scope = "global")
	ICryptoHandler crypto;
	@ActorRequire(name = "bc_account", scope = "global")
	AccountHandler accountHelper;

	@Override
	public String[] getCmds() {
		return new String[] { ActionCMD.QINFO.name() };
	}

	@Override
	public String getModule() {
		return ActionModule.C20.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final ReqQueryRC20TokenInfo pb, final CompleteHandler handler) {
		RespQueryRC20TokenInfo.Builder oret = RespQueryRC20TokenInfo.newBuilder();
		try {
			AccountInfo.Builder acct = accountHelper.getAccount(pb.getTokenAddress());
			if (acct != null) {
				TokenRC20Info tokeninfo = TokenRC20Info.parseFrom(acct.getExtData());
				oret.setTokenAddress(pb.getTokenAddress()).setInfo(tokeninfo);
			}
			oret.setTokenNonce(acct.getNonce());
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