package edu.lab.erc20.lib;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;


public class ByteCode {

    public static String transfer(String recipient, BigInteger amount) {
        
        final Function function = new Function(
            "transfer", 
                Arrays.asList(new Address(160, recipient), 
                new Uint256(amount)), 
                Collections.emptyList());

        return FunctionEncoder.encode(function);
    }

    public static Function balanceOf(String account) {
        
        return new Function(
            "balanceOf", 
                Arrays.asList(new Address(160, account)), 
                Arrays.asList(new TypeReference<Uint256>() {}));

    }



}
