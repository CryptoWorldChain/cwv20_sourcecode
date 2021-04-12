package org.brewchain.cvm.exec.code;

import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.OpCode;
import org.brewchain.cvm.program.Program;
import org.brewchain.cvm.program.Stack;
import org.spongycastle.util.encoders.Hex;

public class CR_SELFBALANCE extends AbstractCodeRunner {

    public CR_SELFBALANCE(OpCode op) {
        super(op);
        // TODO Auto-generated constructor stub
    }

    public int exec(Program program, Stack stack, StringBuffer hint) {
        DataWord balance = program.getBalance();

        if (hint != null) {
            hint.append("self balance: " + " balance: " + balance.toString());
        }

        program.stackPush(balance);
        program.step();
        return 0;
    }

}
