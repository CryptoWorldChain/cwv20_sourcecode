package org.brewchain.cvm.exec;

import com.google.protobuf.ByteString;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.config.CVMConfig;
import org.brewchain.cvm.exec.invoke.BlockContextDWORD;
import org.brewchain.cvm.exec.invoke.ProgramInvokerInfo;
import org.brewchain.cvm.model.Cvm.CVMContract;
import org.brewchain.cvm.program.PrecompiledExecutor;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.ProgramResult;
import org.brewchain.mcore.actuator.IActuator;
import org.brewchain.mcore.actuator.exception.TransactionExecuteException;
import org.brewchain.mcore.actuator.exception.TransactionParameterInvalidException;
import org.brewchain.mcore.bean.ApplyBlockContext;
import org.brewchain.mcore.bean.TransactionInfoWrapper;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.handler.ActuactorHandler;
import org.brewchain.mcore.handler.MCoreServices;
import org.brewchain.mcore.model.Account.AccountInfo;
import org.brewchain.mcore.model.Account.AccountInfo.AccountType;
import org.brewchain.mcore.model.Transaction;
import org.brewchain.mcore.model.Transaction.TransactionBody;
import org.brewchain.mcore.model.Transaction.TransactionType;
import org.brewchain.mcore.tools.bytes.BytesHelper;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "cvm_actuator")
@Slf4j
@Data
public class CVMActuator implements IActuator, ActorService {

	@ActorRequire(name = "MCoreServices", scope = "global")
	MCoreServices mcore;

	@ActorRequire(name = "bc_actuactor", scope = "global")
	ActuactorHandler actuactorHandler;

    HashMap<Integer, PrecompiledExecutor> precompilers = new HashMap<>();

    public CVMActuator() {

    }


	@Validate
	public void startup() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					while (actuactorHandler == null || mcore == null) {
						log.debug("waiting actuator active...mcore=" + (mcore != null) + ",actuactorHandler="
								+ (actuactorHandler != null));
						Thread.sleep(1000);
					}
					actuactorHandler.registerActutor(CVMActuator.this);
					log.info("CVMActuator-Registered!");
				} catch (Exception e) {
					log.error("error in register CVMActuator", e);
				}
			}
		}).start();
	}

    public synchronized void bindPreExecutor(PrecompiledExecutor exec) {
        log.debug("bindPreExecutor:" + exec);
        precompilers.put(exec.getAccountType(), exec);
    }

    public synchronized void unbindPreExecutor(PrecompiledExecutor exec) {
        log.debug("unbindPreExecutor:" + exec);
        precompilers.remove(exec.getAccountType());
    }

    @Override
    public int getType() {
        return TransactionType.CVM_CONTRACT_VALUE;
    }

	@Override
	public boolean needSignature() {
		return CVMConfig.DEBUG_IGNORE_VERIFY_SIGN;// for test
	}

	@Override
	public void onVerifySignature(TransactionInfoWrapper transactionInfo) throws Exception {
		mcore.getActuactorHandler().verifySignature(transactionInfo.getTxinfo());
	}

	@Override
	public void prepareExecute(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo) throws Exception {

	}

	/**
	 * 加载关联的账户
	 */
	@Override
	public void preloadAccounts(TransactionInfoWrapper txw,ConcurrentHashMap<ByteString, AccountInfoWrapper> accounts) {
			if (isCreateContract(txw)) {
			// 构建函数
			ByteString newAddr = calcContractAddr(txw.getTxinfo().getBody());
			AccountInfo.Builder account = mcore.getAccountHandler().getAccount(newAddr);
			if (account != null) {
				txw.setValidTx(false);
				txw.setErrorMessage("contract already exists!");

			} else {
				txw.setParrallelExec(false);
			}
		} else if (txw.getTxinfo().getBody().getOutputsCount() == 1) {
			// 调用函数
			ByteString bsAddr = txw.getTxinfo().getBody().getOutputs(0).getAddress();
			AccountInfoWrapper aiw = accounts.get(bsAddr);
			if (aiw == null || !(aiw instanceof CVMAccountWrapper)) {
				if (aiw == null) {
					aiw = mcore.getAccountHandler().getAccountOrCreate(bsAddr);
				}
				if (aiw.getInfo().getType() == AccountType.CVM_CONTRACT) {
					CVMAccountWrapper contractAccount = new CVMAccountWrapper(aiw.getInfo());
					contractAccount.loadStorageTrie(mcore.getStateTrie());
					accounts.put(bsAddr, contractAccount);
					
					if (!contractAccount.getContractInfo().getParrallel()) {
						// 是否并行，默认是并行
						txw.setParrallelExec(false);
					} else {
						txw.setParrallelExec(true);
					}
				} else {
					txw.setValidTx(false);
					txw.setErrorMessage("not valid cvm contract");
				}
			}

		} else {
			txw.setValidTx(false);
			txw.setErrorMessage("output count large then 1");
		}

	}

	public ByteString execCVMFunc(AccountInfoWrapper sender,TransactionInfoWrapper txw) throws TransactionExecuteException {
		ByteString bsAddr = txw.getTxinfo().getBody().getOutputs(0).getAddress();
		// 调用函数
		AccountInfo.Builder account = mcore.getAccountHandler().getAccount(bsAddr);
		if(account==null){
			throw new TransactionExecuteException("contract address not found:"+Hex.toHexString(bsAddr.toByteArray()));
		}
		CVMAccountWrapper caw = new CVMAccountWrapper(account);

		caw.loadStorageTrie(mcore.getStateTrie());
		txw.getTouchAccounts().put(caw.getInfo().getAddress(), caw);

		BlockContextDWORD btx = new BlockContextDWORD(txw.getBlockContext());
		byte msgData[] = null;
		if (!txw.getTxinfo().getBody().getCodeData().isEmpty()) {
			msgData = txw.getTxinfo().getBody().getCodeData().toByteArray();
		}

		ProgramInvokerInfo invoker = new ProgramInvokerInfo(sender, txw, caw, msgData, btx, 0,DataWord.ZERO);
		Program program = new Program(caw, txw, invoker, new TxStorage(txw), precompilers);
		// try {

		if(!invoker.getCallValue().isZero())
		{
			BigInteger msgValue = BytesHelper.bytesToBigInteger(invoker.getCallValue().getData());
			if(sender.zeroSubCheckAndGet(msgValue).signum() < 0){
				throw new TransactionExecuteException("not enough balance");
			}
			caw.addAndGet(msgValue);
		}

		BrewVM.play(program);
		ProgramResult results = program.getResult();
		// reput it
		if (results.getException() != null || results.isRevert()) {
			if (results.getException() != null) {
				log.error("error on call conntract", results.getException());
				throw new TransactionExecuteException("error on call contract");
			} else {
				// 处理revert result
				byte[] funcBytes = new byte[4];
				// TODO cvm events
				// result.getLogInfoList()
				if (results.getHReturn().length > 0) {
					System.arraycopy(results.getHReturn(), 0, funcBytes, 0, 4);
					String func = mcore.getCrypto().bytesToHexStr(funcBytes);

					byte[] lenBytes = new byte[32];
					System.arraycopy(results.getHReturn(), 4 + 32, lenBytes, 0, 32);
					Integer len = Integer.parseInt(
							mcore.getCrypto().bytesToHexStr(BytesHelper.stripLeadingZeroes(lenBytes)), 16);

					byte[] resultBytes = new byte[len];
					System.arraycopy(results.getHReturn(), 4 + 32 + 32, resultBytes, 0, len);

					throw new TransactionExecuteException("REVERT opcode executed, func=" + func + ", reason="
							+ ByteString.copyFrom(resultBytes).toStringUtf8());
				} else {
					throw new TransactionExecuteException("REVERT opcode executed");
				}
			}
		} else {
			return ByteString.copyFrom(results.getHReturn());
			// return ByteString.copyFrom(result.getHReturn());
		}
	}

	public ByteString calcContractAddr(TransactionBody body) {
		byte[] nonce = ByteBuffer.allocate(8).putLong(body.getNonce()).array();
		byte[] timestamp = ByteBuffer.allocate(8).putLong(body.getTimestamp()).array();
		ByteString calculatedAddress = body.getAddress().concat(ByteString.copyFrom(nonce))
				.concat(ByteString.copyFrom(timestamp));

		return ByteString.copyFrom(mcore.getCrypto().sha3(calculatedAddress.toByteArray()), 0, 20);
	}

	static ByteString OneBS = ByteString.copyFrom(new byte[] { 0x01 });

    public boolean isCreateContract(TransactionInfoWrapper txw){
        return txw.getTxinfo().getBody().getOutputsCount() == 0
                || (txw.getTxinfo().getBody().getOutputsCount() == 1 && txw.getTxinfo().getBody().getOutputs(0).getAddress().isEmpty());
    }
    /**
     * 执行交易
     */
    @Override
    public ByteString execute(AccountInfoWrapper sender, TransactionInfoWrapper txw, ApplyBlockContext bcContext)
            throws Exception {
        DataWord callValue =DataWord.ZERO;
        if (txw.getTxinfo().getBody().getOutputsCount() == 1
                && !txw.getTxinfo().getBody().getOutputs(0).getAmount().isEmpty()) {
            callValue = new DataWord(txw.getTxinfo().getBody().getOutputs(0).getAmount().toByteArray());
        }
		log.info("contract address not found:"+Hex.toHexString(sender.getInfo().getAddress().toByteArray()));
		if (isCreateContract(txw)) {
            // 构建函数
            BlockContextDWORD btx = new BlockContextDWORD(bcContext);
            ByteString newAddr = calcContractAddr(txw.getTxinfo().getBody());
            AccountInfo.Builder account = mcore.getAccountHandler().getAccountOrCreate(newAddr.toByteArray());

			CVMAccountWrapper caw = new CVMAccountWrapper(account);
			
			CVMContract.Builder contract = CVMContract.newBuilder().setDatas(txw.getTxinfo().getBody().getCodeData());
			if (OneBS.equals(txw.getTxinfo().getBody().getExtData())) {
				contract.setParrallel(true);
			} else {
				contract.setParrallel(false);
			}
			caw.setCVMContract(contract.build());
			caw.loadStorageTrie(mcore.getStateTrie());
			txw.getTouchAccounts().put(caw.getInfo().getAddress(), caw);
			bcContext.getAccounts().put(newAddr, caw);
			ProgramInvokerInfo invoker = new ProgramInvokerInfo(sender, txw, caw, null, btx, 0,callValue);
			Program createProgram = new Program(caw, txw, invoker, new TxStorage(txw),precompilers);
            try {
                if(!invoker.getCallValue().isZero())
                {
                    BigInteger msgValue = BytesHelper.bytesToBigInteger(invoker.getCallValue().getData());
                    if(sender.zeroSubCheckAndGet(msgValue).signum() < 0){
                        throw new TransactionExecuteException("not enough balance");
                    }
                    caw.addAndGet(msgValue);
                }

                BrewVM.play(createProgram);
                BigInteger gas_cost = BigInteger.valueOf(Math.min(CVMConfig.GAS_CVM_MAX_COST, Math.max(CVMConfig.GAS_CVM_MIN_COST, createProgram.getGas_cost() / 10)));
                if (mcore.getChainConfig().getConfigAccount().getGasPrice().signum() > 0
                        && sender.zeroSubCheckAndGet(mcore.getChainConfig().getConfigAccount().getGasPrice().multiply(gas_cost)).signum() < 0) {
                    //
					if(!invoker.getCallValue().isZero())
					{
						BigInteger msgValue = BytesHelper.bytesToBigInteger(invoker.getCallValue().getData());
						sender.addAndGet(msgValue);
						caw.zeroSubCheckAndGet(msgValue);
					}
                    throw new TransactionParameterInvalidException(
                            "parameter invalid, balance of the sender is not enough for gas");
                }
				txw.getBlockContext().getAccounts().put(newAddr, caw);
                ProgramResult createResult = createProgram.getResult();
                byte[] creats = createResult.getHReturn();
                if (creats.length > 0) {
                    CVMContract.Builder newcontract = CVMContract.newBuilder().setDatas(ByteString.copyFrom(creats))
                            .setParrallel(contract.getParrallel());
                    caw.setCVMContract(newcontract.build());
					return newAddr;
				}else {
					if(!invoker.getCallValue().isZero())
					{
						BigInteger msgValue = BytesHelper.bytesToBigInteger(invoker.getCallValue().getData());
						sender.addAndGet(msgValue);
						caw.zeroSubCheckAndGet(msgValue);
					}
					throw new TransactionExecuteException("error in create contract");
				}
				// reput it
			} catch (Exception e) {
				log.error("error in exec createcontract", e);
				throw new TransactionExecuteException(e.getMessage());
			}
			
		} else if (txw.getTxinfo().getBody().getOutputsCount() == 1) {
			ByteString bsAddr = txw.getTxinfo().getBody().getOutputs(0).getAddress();
			// 调用函数
			CVMAccountWrapper caw = (CVMAccountWrapper) txw.getAccount(bsAddr);
			caw.loadStorageTrie(mcore.getStateTrie());
			txw.getTouchAccounts().put(caw.getInfo().getAddress(), caw);
			BlockContextDWORD btx = new BlockContextDWORD(bcContext);
			byte msgData[] = null;
			if (!txw.getTxinfo().getBody().getCodeData().isEmpty()) {
				msgData = txw.getTxinfo().getBody().getCodeData().toByteArray();
			}
			ProgramInvokerInfo invoker = new ProgramInvokerInfo(sender, txw, caw, msgData, btx, 0,callValue);
			Program program = new Program(caw, txw, invoker, new TxStorage(txw),precompilers);
			// try {
            if(!invoker.getCallValue().isZero())
            {
                BigInteger msgValue = BytesHelper.bytesToBigInteger(invoker.getCallValue().getData());
                if(sender.zeroSubCheckAndGet(msgValue).signum() < 0){
					log.info("contract address not found:"+Hex.toHexString(sender.getInfo().getAddress().toByteArray()));
					throw new TransactionExecuteException("not enough balance");
                }
                caw.addAndGet(msgValue);
            }
				BrewVM.play(program);
				ProgramResult results = program.getResult();
				BigInteger gas_cost = BigInteger.valueOf(Math.min(CVMConfig.GAS_CVM_MAX_COST, Math.max(CVMConfig.GAS_CVM_MIN_COST, program.getGas_cost()/10))); 
				if (mcore.getChainConfig().getConfigAccount().getGasPrice().signum() > 0
						&& sender.zeroSubCheckAndGet(mcore.getChainConfig().getConfigAccount().getGasPrice().multiply(gas_cost)).signum() < 0) {
					//
					if(!invoker.getCallValue().isZero())
					{
						BigInteger msgValue = BytesHelper.bytesToBigInteger(invoker.getCallValue().getData());
						sender.addAndGet(msgValue);
						caw.zeroSubCheckAndGet(msgValue);
					}
					throw new TransactionParameterInvalidException(
							"parameter invalid, balance of the sender is not enough for gas");
				}
				bcContext.getGasAccumulate().addAndGet(BigInteger.ONE);
				
				// reput it
				if (results.getException() != null || results.isRevert()) {	               
	                if(!invoker.getCallValue().isZero())
	                {
	                    BigInteger msgValue = BytesHelper.bytesToBigInteger(invoker.getCallValue().getData());
	                    sender.addAndGet(msgValue);
	                    caw.zeroSubCheckAndGet(msgValue);
	                }

					if (results.getException() != null) {
						log.error("error on call conntract", results.getException());
						throw new TransactionExecuteException("error on call contract");
					} else {
						// 处理revert result
						byte[] funcBytes = new byte[4];
						// TODO cvm events
						// result.getLogInfoList()
						if (results.getHReturn().length > 0) {
							System.arraycopy(results.getHReturn(), 0, funcBytes, 0, 4);
							String func = mcore.getCrypto().bytesToHexStr(funcBytes);

							byte[] lenBytes = new byte[32];
							System.arraycopy(results.getHReturn(), 4 + 32, lenBytes, 0, 32);
							Integer len = Integer.parseInt(
									mcore.getCrypto().bytesToHexStr(BytesHelper.stripLeadingZeroes(lenBytes)), 16);

							byte[] resultBytes = new byte[len];
							System.arraycopy(results.getHReturn(), 4 + 32 + 32, resultBytes, 0, len);

							throw new TransactionExecuteException("REVERT opcode executed, func=" + func + ", reason="
									+ ByteString.copyFrom(resultBytes).toStringUtf8());
						} else {
							throw new TransactionExecuteException("REVERT opcode executed");
						}
					}
				} else {
					caw.setDirty(true);
					txw.putTouchAccount();
					return ByteString.copyFrom(results.getHReturn());
					// return ByteString.copyFrom(result.getHReturn());
				}

			// } catch (Exception e) {
			// 	log.error("error in create Contract", e);

			// }

		}
		return ByteString.EMPTY;
	}

	public String toString() {
		return "CVMActuator";
	}

}
