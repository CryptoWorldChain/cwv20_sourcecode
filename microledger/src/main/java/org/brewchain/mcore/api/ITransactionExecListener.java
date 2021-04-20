package org.brewchain.mcore.api;

import org.brewchain.mcore.bean.ApplyBlockContext;
import org.brewchain.mcore.bean.TransactionInfoWrapper;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.model.Transaction.TransactionInfo;

public interface ITransactionExecListener {

	public void onPreExec(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext blockContext);
	
	public void onPostExec(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext blockContext) ;
	
	public boolean verifyTx(TransactionInfo transactionInfo) throws Exception;
	
	public int getType() ;

}
