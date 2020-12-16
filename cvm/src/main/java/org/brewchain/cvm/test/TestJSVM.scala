package org.brewchain.cvm.test

import javax.script.SimpleScriptContext
import javax.script.ScriptEngineManager

object TestJSVM {
  def main(args: Array[String]): Unit = {

    class storage  {
      
    }
    val fetchjsengine = new ScriptEngineManager().getEngineByName("JavaScript");
    val jscontext = new SimpleScriptContext();
     
    val contract = """
      var  name;
      var symbol;
      var decimals = 18;
      
        // internal variables
      var  _totalSupply;
      
      var _balances=new Array();
      
      function transfer(sender, to,  value)  {
          if(to == "0")
          {
          	return false;
          }
          if(value > _balances[sender])
          {
            return false;
          }
          _balances[sender] = _balances[sender] - value;
          _balances[to] = _balances[to]+value;
          return true;
      }
      
      """

    val obj = fetchjsengine.eval(contract);
    val runcount = 50000;
    val start = System.currentTimeMillis();
    for (i <- 1 to runcount) {
      fetchjsengine.eval("_balances[1]=100000000");
      val jsret = fetchjsengine.eval("""transfer(1,2,1)""");
    }
    println("cost=="+(System.currentTimeMillis() - start))
    
//    println(jsret)

  }
}