package org.brewchain.mcore.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.brewchain.mcore.actuator.IActuator;
import org.brewchain.mcore.actuator.TransferActuator;
import org.brewchain.mcore.actuator.exception.TransactionVerifyException;
import org.brewchain.mcore.api.ITransactionExecListener;
import org.brewchain.mcore.bean.ApplyBlockContext;
import org.brewchain.mcore.bean.TransactionInfoWrapper;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.model.Transaction.TransactionBody;
import org.brewchain.mcore.model.Transaction.TransactionInfo;
import org.brewchain.mcore.tools.bytes.BytesComparisons;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;

@Slf4j
@Data
@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "bc_actuactor")
public class ActuactorHandler implements ActorService {

	@ActorRequire(name = "MCoreServices", scope = "global")
	MCoreServices mcore;

	ConcurrentHashMap<Integer, IActuator> actByType = new ConcurrentHashMap<>();
	
	List<ITransactionExecListener>  txListeners = new ArrayList<>();
	

	@Validate
	public void startup() {

		new Thread(new Runnable() {

			@Override
			public void run() {
				while (mcore == null) {
					try {
						Thread.sleep(1000);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				actByType.put(0, new TransferActuator(mcore));
				log.debug("create default transferacutator,mcore="+(mcore==null));
			}
		}).start();

	}
	
	public boolean registerTxListner(ITransactionExecListener newListener) {
		for(ITransactionExecListener txListener:txListeners) {
			if(txListener.getType() == newListener.getType()) {
				return false;
			}
		}
		txListeners.add(newListener);
		return true;
	}

	public ByteString onExecute(TransactionInfo transactionInfo) throws Exception {
		return ByteString.EMPTY;
	}

	public synchronized boolean registerActutor(IActuator act) {
		if (actByType.containsKey(act.getType())) {
			return false;
		}
		actByType.put(act.getType(), act);
		return true;
	}

	public IActuator getActuator(int type) {
		return actByType.get(type);
	}
	
	public void onPreExec(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext blockContext) {
		for(ITransactionExecListener txListener:txListeners) {
			txListener.onPreExec(sender, transactionInfo, blockContext);
		}
	}
	
	public void onPostExec(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext blockContext) {
		for(ITransactionExecListener txListener:txListeners) {
			txListener.onPostExec(sender, transactionInfo, blockContext);
		}
	}


	public void verifySignature(TransactionInfo transactionInfo) throws Exception {
		TransactionInfo.Builder signatureTx = transactionInfo.toBuilder();
		TransactionBody.Builder txBody = signatureTx.getBodyBuilder();
		byte[] oMultiTransactionEncode = txBody.build().toByteArray();
		byte[] hexPubKey = mcore.getCrypto().signatureToKey(oMultiTransactionEncode,
				transactionInfo.getSignature().toByteArray());

		byte[] address = mcore.getCrypto().signatureToAddress(oMultiTransactionEncode,
				transactionInfo.getSignature().toByteArray());

		try {
			if (!mcore.getCrypto().verify(hexPubKey, oMultiTransactionEncode,
					transactionInfo.getSignature().toByteArray())) {
				throw new TransactionVerifyException("signature verify fail with pubkey");
			} else if (!BytesComparisons.equal(address, txBody.getAddress().toByteArray())) {
				throw new TransactionVerifyException("invalid transaction sender");
			}
			for(ITransactionExecListener txListener:txListeners) {
				if(!txListener.verifyTx(transactionInfo)) {
					throw new TransactionVerifyException("transaction sender verify failed:"+txListener.getType());
				}
			}
		} catch (Exception e) {
			log.error("verify exception tx=" + mcore.getCrypto().bytesToHexStr(oMultiTransactionEncode) + " sign="
					+ mcore.getCrypto().bytesToHexStr(transactionInfo.getSignature().toByteArray()) + " pub="
					+ mcore.getCrypto().bytesToHexStr(hexPubKey));
			throw e;
		}
	}

}
