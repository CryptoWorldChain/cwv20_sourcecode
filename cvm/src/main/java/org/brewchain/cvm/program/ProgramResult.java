package org.brewchain.cvm.program;

import static org.brewchain.cvm.utils.ByteUtil.EMPTY_BYTE_ARRAY;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.CallCreate;

/**
 * @author Roman Mandeleil
 * @since 07.06.2014
 */
public class ProgramResult {

    private long gasUsed;
    private byte[] hReturn = EMPTY_BYTE_ARRAY;
    private RuntimeException exception;
    private boolean revert;

    private Set<DataWord> deleteAccounts;
    private long futureRefund = 0;

    private String cmds = "";
    public void setCmds(String cmds) {
    		this.cmds = cmds;
    }
    public void addCmds(String cmd) {
		this.cmds += cmd;
	}
    public String getCmds() {
		return this.cmds;
	}
    
    /*
     * for testing runs ,
     * call/create is not executed
     * but dummy recorded
     */
    private List<CallCreate> callCreateList;

    public void spendGas(long gas) {
        gasUsed += gas;
    }

    public void setRevert() {
        this.revert = true;
    }

    public boolean isRevert() {
        return revert;
    }

    public void refundGas(long gas) {
        gasUsed -= gas;
    }

    public void setHReturn(byte[] hReturn) {
        this.hReturn = hReturn;

    }

    public byte[] getHReturn() {
        return hReturn;
    }

    public RuntimeException getException() {
        return exception;
    }

    public long getGasUsed() {
        return gasUsed;
    }

    public void setException(RuntimeException exception) {
        this.exception = exception;
    }

    public Set<DataWord> getDeleteAccounts() {
        if (deleteAccounts == null) {
            deleteAccounts = new HashSet<>();
        }
        return deleteAccounts;
    }

    public void addDeleteAccount(DataWord address) {
        getDeleteAccounts().add(address);
    }

    public void addDeleteAccounts(Set<DataWord> accounts) {
        if (!isEmpty(accounts)) {
            getDeleteAccounts().addAll(accounts);
        }
    }


    public List<CallCreate> getCallCreateList() {
        if (callCreateList == null) {
            callCreateList = new ArrayList<>();
        }
        return callCreateList;
    }

    public void addCallCreate(byte[] data, byte[] destination,  byte[] value) {
        getCallCreateList().add(new CallCreate(data, destination, value));
    }

    
    public static boolean isEmpty(final Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }
    public static int size(final Object object) {
        if (object == null) {
            return 0;
        }
        int total = 0;
        if (object instanceof Map<?,?>) {
            total = ((Map<?, ?>) object).size();
        } else if (object instanceof Collection<?>) {
            total = ((Collection<?>) object).size();
        } else if (object instanceof Object[]) {
            total = ((Object[]) object).length;
        } else if (object instanceof Iterator<?>) {
            final Iterator<?> it = (Iterator<?>) object;
            while (it.hasNext()) {
                total++;
                it.next();
            }
        } else if (object instanceof Enumeration<?>) {
            final Enumeration<?> it = (Enumeration<?>) object;
            while (it.hasMoreElements()) {
                total++;
                it.nextElement();
            }
        } else {
            try {
                total = Array.getLength(object);
            } catch (final IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unsupported object type: " + object.getClass().getName());
            }
        }
        return total;
    }

    public void addFutureRefund(long gasValue) {
        futureRefund += gasValue;
    }

    public long getFutureRefund() {
        return futureRefund;
    }

    public void resetFutureRefund() {
        futureRefund = 0;
    }
    
    public static ProgramResult createEmpty() {
        ProgramResult result = new ProgramResult();
        result.setHReturn(EMPTY_BYTE_ARRAY);
        return result;
    }
    
    
}
