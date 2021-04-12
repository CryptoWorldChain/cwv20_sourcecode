package org.brewchain.cvm.program;

import org.apache.commons.lang3.tuple.Pair;
import org.brewchain.cvm.exec.invoke.ProgramInvokerInfo;
import org.brewchain.mcore.bean.ApplyBlockContext;
import org.brewchain.mcore.bean.TransactionInfoWrapper;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.handler.MCoreServices;

public interface PrecompiledExecutor {
    public long getGasForData(byte[] data);

    public int getAccountType();

    public Pair<Boolean, byte[]> execute(TransactionInfoWrapper txw, ProgramInvokerInfo caller, AccountInfoWrapper contractAiw, byte[] data);
}
