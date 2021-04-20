package org.brewchain.mcore.action.account;

import org.brewchain.mcore.api.ICryptoHandler;
import org.brewchain.mcore.exception.DposNodeNotReadyException;
import org.brewchain.mcore.handler.AccountHandler;
import org.brewchain.mcore.model.Account.AccountInfo;
import org.brewchain.mcore.model.Action.AccountMessage;
import org.brewchain.mcore.model.Action.ActionCommand;
import org.brewchain.mcore.model.Action.ActionModule;
import org.brewchain.mcore.model.Action.RetAccountMessage;
import org.brewchain.mcore.model.Action.RetAccountMessage.AccountType;
import org.brewchain.mcore.service.ChainConfig;
import org.brewchain.mcore.service.StateTrie;
import org.brewchain.mcore.tools.bytes.BytesHelper;
import org.brewchain.mcore.tools.chain.AddressHelper;

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
public class GetAccountImpl extends SessionModules<AccountMessage> {
	@ActorRequire(name = "bc_account", scope = "global")
	AccountHandler accountHelper;
	@ActorRequire(name = "bc_crypto", scope = "global")
	ICryptoHandler crypto;
	@ActorRequire(name = "bc_statetrie", scope = "global")
	StateTrie stateTrie;

	@ActorRequire(name = "bc_chainconfig", scope = "global")
	ChainConfig chainConfig;

	@Override
	public String[] getCmds() {
		return new String[] { ActionCommand.GAC.name() };
	}

	@Override
	public String getModule() {
		return ActionModule.ACT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final AccountMessage pb, final CompleteHandler handler) {
		RetAccountMessage.Builder oRet = RetAccountMessage.newBuilder();
		try {
			if (!chainConfig.isNodeStart()) {
				throw new DposNodeNotReadyException("dpos node not ready");
			}

			AccountInfo.Builder oAccount = accountHelper.getAccount(AddressHelper.hex2Bytes(pb.getAddress()));

			if (oAccount == null) {
				oRet.setRetCode(-1);
				oRet.setRetMsg("账户不存在");
				handler.onFinished(PacketHelper.toPBReturn(pack, oRet.build()));
				return;
			}

			oRet.setAddress(oAccount.getAddress());
			oRet.setNonce(oAccount.getNonce());
			if (!oAccount.getBalance().isEmpty()) {
				oRet.setBalance("0x" + BytesHelper.bytesToBigInteger(oAccount.getBalance().toByteArray()).toString(16));
			} else {
				oRet.setBalance("0x0");
			}
			if (!oAccount.getExtData().isEmpty()) {
				oRet.setExtData(oAccount.getExtData());
			}
			oRet.setStatus(oAccount.getStatus());
			if (!oAccount.getStorageTrieRoot().isEmpty()) {
				oRet.setStorageTrieRoot(oAccount.getStorageTrieRoot());
			}
			// if(oAccount.getType())
			oRet.setType(AccountType.ACCOUNT);

			oRet.setRetCode(1);
		} catch (Exception e) {
			log.error("", e);
			oRet.clear();
			oRet.setRetCode(-1);
			oRet.setRetMsg(e.getMessage());
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRet.build()));
	}
}
