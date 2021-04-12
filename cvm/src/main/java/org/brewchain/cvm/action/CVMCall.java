package org.brewchain.cvm.action;

import com.google.protobuf.ByteString;
import javafx.application.Application;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import org.apache.commons.lang3.StringUtils;
import org.brewchain.cvm.exec.CVMActuator;
import org.brewchain.cvm.model.Cvm;
import org.brewchain.mcore.actuator.exception.TransactionExecuteException;
import org.brewchain.mcore.api.ICryptoHandler;
import org.brewchain.mcore.bean.ApplyBlockContext;
import org.brewchain.mcore.bean.TransactionInfoWrapper;
import org.brewchain.mcore.bean.TransactionMessage;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.concurrent.AtomicBigInteger;
import org.brewchain.mcore.handler.ActuactorHandler;
import org.brewchain.mcore.handler.BlockHandler;
import org.brewchain.mcore.handler.ChainHandler;
import org.brewchain.mcore.handler.TransactionHandler;
import org.brewchain.mcore.model.Account;
import org.brewchain.mcore.model.Action.ActionCommand;
import org.brewchain.mcore.model.Action.ActionModule;
import org.brewchain.mcore.model.Action.RetSendTransactionMessage;
import org.brewchain.mcore.model.Action.TransactionInfoImpl;
import org.brewchain.mcore.model.Block;
import org.brewchain.mcore.model.Transaction;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

@NActorProvider
@Slf4j
@Data
public class CVMCall extends SessionModules<Cvm.CVMReqCall> {
	@ActorRequire(name = "bc_chain", scope = "global")
	ChainHandler blockChainHelper;
	@ActorRequire(name = "bc_transaction", scope = "global")
	TransactionHandler transactionHandler;
	@ActorRequire(name = "bc_crypto", scope = "global")
	ICryptoHandler crypto;

	@ActorRequire(name = "cvm_actuator", scope = "global")
	CVMActuator cvmActuator;

	@Override
	public String[] getCmds() {
        return new String[] { Cvm.PCommand.CAL.name() };
	}

	@Override
	public String getModule() {
		return Cvm.PModule.CVM.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final Cvm.CVMReqCall pb, final CompleteHandler handler) {
		Cvm.CVMRespCall.Builder oret = Cvm.CVMRespCall.newBuilder();
		try {
			oret.setRetCode(1);
			byte [] from;
			if(StringUtils.isNotBlank(pb.getFrom())) {
				from = Hex.decode(pb.getFrom().replaceFirst("0x",""));
			}else {
				from = Hex.decode("0000000000000000000000000000000000000000");
			}
			byte []to;
			if(StringUtils.isNotBlank(pb.getTo())) {
				to = Hex.decode(pb.getTo().replaceFirst("0x",""));
			}else {
				throw new TransactionExecuteException("contract address error");
			}

			AccountInfoWrapper sender =new AccountInfoWrapper( cvmActuator.getMcore().getAccountHandler().getAccountOrCreate(from));

			Transaction.TransactionInfo txinfo = Transaction.TransactionInfo.newBuilder().setBody(
					Transaction.TransactionBody.newBuilder()
							.setCodeData(pb.getData())
							.setAddress(ByteString.copyFrom(from))
							.addOutputs(
									Transaction.TransactionOutput.newBuilder().setAddress(ByteString.copyFrom(to)).build())
							.setInnerCodetype(4)
							.build()).build();


			TransactionInfoWrapper txw=new TransactionInfoWrapper(txinfo);
			txw.setMcore(cvmActuator.getMcore());

			CountDownLatch cdl = new CountDownLatch(1);
			byte[][] txkeys = new byte[1][];
			byte[][] txvalues = new byte[1][];
			byte results[][] = new byte[1][];
			AtomicBigInteger gasAccumulate = new AtomicBigInteger(BigInteger.ZERO);
			ConcurrentHashMap<ByteString, AccountInfoWrapper> accounts=new ConcurrentHashMap<ByteString, AccountInfoWrapper>();

			Block.BlockInfo currentBlock=cvmActuator.getMcore().getChainHandler().getLastConnectBlock();
			Block.BlockInfo blockInfo=currentBlock;
			ApplyBlockContext blockContext = new ApplyBlockContext(
					currentBlock.toBuilder(), blockInfo, accounts, results, cdl,
					txkeys, txvalues,gasAccumulate);

			txw.setBlockContext(blockContext);

			ByteString result = cvmActuator.execCVMFunc(sender,txw);
			oret.setResult(result);


		} catch (Exception e) {
			log.error("", e);
			oret.clear();
			oret.setRetCode(-1);
			oret.setRetMsg(e.getMessage());
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oret.build()));
	}
}
