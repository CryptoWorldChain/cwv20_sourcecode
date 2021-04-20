package org.brewchain.mcore.action.block;

import org.brewchain.mcore.model.Action.ActionCommand;
import org.brewchain.mcore.model.Action.ActionModule;
import org.brewchain.mcore.model.Action.LastBlockMessage;
import org.brewchain.mcore.model.Action.LastBlockMessage.BlockMessageType;
import org.brewchain.mcore.model.Action.RetBlockMessage;
import org.brewchain.mcore.model.Block.BlockInfo;
import org.brewchain.mcore.api.ICryptoHandler;
import org.brewchain.mcore.handler.ChainHandler;
import org.brewchain.mcore.handler.TransactionHandler;
import org.brewchain.mcore.tools.bytes.BytesHelper;

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
public class GetLastBlockImpl extends SessionModules<LastBlockMessage> {
	@ActorRequire(name = "bc_chain", scope = "global")
	ChainHandler blockChainHelper;
	@ActorRequire(name = "bc_transaction", scope = "global")
	TransactionHandler transactionHandler;
	@ActorRequire(name = "bc_crypto", scope = "global")
	ICryptoHandler crypto;

	@Override
	public String[] getCmds() {
		return new String[] { ActionCommand.GLB.name() };
	}

	@Override
	public String getModule() {
		return ActionModule.BCT.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final LastBlockMessage pb, final CompleteHandler handler) {
		RetBlockMessage.Builder oRet = RetBlockMessage.newBuilder();

		try {

			if (!blockChainHelper.getChainConfig().isNodeStart()) {
				oRet.clear();
				oRet.setRetCode(-1);
				oRet.setRetMsg("节点未启动完成");
				handler.onFinished(PacketHelper.toPBReturn(pack, oRet.build()));
				return;
			}

			BlockInfo bi = blockChainHelper.getLastConnectBlock();
			if (bi != null) {
				oRet.addBlock(bi);
				oRet.setRetCode(1);
			}else {
				oRet.setRetCode(-1).setRetMsg("block not found");
			}
		} catch (Exception e) {
			// log.error("", e);
			oRet.clear();
			oRet.setRetCode(-1);
			oRet.setRetMsg(e.getMessage() == null ? "区块不存在" : e.getMessage());
		}

		handler.onFinished(PacketHelper.toPBReturn(pack, oRet.build()));
	}
}
