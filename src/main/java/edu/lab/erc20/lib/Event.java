package edu.lab.erc20.lib;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.Utils;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
 
public class Event {

    public static final String EVENT_TRANSFER = "Transfer(address,address,uint256)";

    public static boolean getValueEvent(TransactionReceipt receipt, BigInteger amount, String nameEvent){
        String event = EventEncoder.buildEventSignature(nameEvent);

        boolean flag = false;

        outerloop:
        for (Log logTx : receipt.getLogs()) {
            
            for (String hash256Event : logTx.getTopics()) {

                if (event.equals(hash256Event)) {
                    
                    List<TypeReference<Type>> outputParameters = Utils.convert(Arrays.asList(new TypeReference<Uint256>() {}));
                    
                    BigInteger val =   Read.singleValueReturn(logTx.getData(), outputParameters, BigInteger.class );

                    flag = amount.equals(val);

                    break outerloop;
                }
            }
        }
        
        return flag;
    }
}
