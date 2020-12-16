package org.brewchain.cvm.exec;

import org.brewchain.cvm.model.Cvm.CVMContract;
import org.brewchain.cvm.program.ProgramPrecompile;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.model.Account.AccountInfo;
import org.brewchain.mcore.model.Account.AccountInfo.AccountType;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class CVMAccountWrapper extends AccountInfoWrapper {

	CVMContract contractInfo;

	ProgramPrecompile compiledProgram;

	public CVMAccountWrapper(AccountInfo.Builder info) {
		super(info);
		try {
			if (!info.getExtData().isEmpty()) {
				contractInfo = CVMContract.parseFrom(info.getExtData());
				compiledProgram = ProgramPrecompile.compile(contractInfo.getDatas().toByteArray());
			} else {
				contractInfo = null;
			}
		} catch (Exception e) {
		}

	}

	@Override
	public synchronized AccountInfo build(long blocknumber) {
		return super.build(blocknumber);
	}

	public void setCVMContract(CVMContract contract) {
		contractInfo = contract;
		super.getInfo().setType(AccountType.CVM_CONTRACT);
		super.getInfo().setExtData(contract.toByteString());
		compiledProgram = ProgramPrecompile.compile(contractInfo.getDatas().toByteArray());
		super.setDirty(true);
	}
	
}
