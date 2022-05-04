package edu.lab.erc20.lib;

import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Response {
    private EthSendTransaction ethSendTransaction;
    private TransactionReceipt receipt ;

    public Response(EthSendTransaction ethSendTransaction, TransactionReceipt receipt){
        this.ethSendTransaction = ethSendTransaction;
        this.receipt = receipt;
    }
}
