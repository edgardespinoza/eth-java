package edu.lab.erc20.service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;

import edu.lab.erc20.lib.ByteCode;
import edu.lab.erc20.lib.Event;
import edu.lab.erc20.lib.HttpService;
import edu.lab.erc20.lib.RawTransactionTx;
import edu.lab.erc20.lib.Read;
import edu.lab.erc20.lib.Response;
import okhttp3.OkHttpClient;
 
public class TransactionManagerTx  {

    private Web3j web3j;
    private String contractAddress;

    public TransactionManagerTx(String host, String contractAddress) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(60, TimeUnit.SECONDS) // connect timeout
                .writeTimeout(60, TimeUnit.SECONDS) // write timeout
                .readTimeout(60, TimeUnit.SECONDS); // read timeout

        HttpService httpService = new HttpService(host, builder.build());
        Map<String, String> header = new HashMap<>();
        header.put("Connection", "close");
        httpService.addHeaders(header);

        this.web3j = Web3j.build(httpService);
        this.contractAddress = contractAddress;
    }

    public BigInteger getNonce(String privateKey) throws IOException {
        EthGetTransactionCount ethGetTransactionCount = web3j
                .ethGetTransactionCount(Credentials.create(privateKey).getAddress(), DefaultBlockParameterName.LATEST)
                .send();

        return ethGetTransactionCount.getTransactionCount();
    }

    public BigInteger getGasPrice() throws IOException {

        return web3j.ethGasPrice().send().getGasPrice();
    }

    public TransactionReceipt getTx(String txHash) throws IOException, TransactionException {

        TransactionReceiptProcessor receiptProcessor = new PollingTransactionReceiptProcessor(web3j, 2000,
                TransactionManager.DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH);

        return receiptProcessor.waitForTransactionReceipt(txHash);

    }

   
    public String transfer(String privateKey, String recipient, BigInteger amount, double factor)
            throws IOException, TransactionException {

        String addressOwner = Credentials.create(privateKey).getAddress();

        if (amount.signum() != 1) {
            throw new Error("AMOUNT_EMPTY" + ":" + amount);
        }

        BigInteger balance = balanceOf(addressOwner, addressOwner);

        if (balance.compareTo(amount) < 0) {
            throw new Error("INSUFFICIENT_BALANCE" + ":" + balance + "<" + amount);
        }

        String encodedFunction = ByteCode.transfer(recipient, amount);

        Response response = process(web3j, contractAddress, privateKey, encodedFunction,
                2000, factor);

        boolean bool = Event.getValueEvent(response.getReceipt(), amount, Event.EVENT_TRANSFER);

        if (!bool) {
            throw new Error(
                    "RESULT_FALSE :" + response.getEthSendTransaction().getResult());
        }
        return response.getEthSendTransaction().getResult();
    }

    public BigInteger balanceOf(String addressQuery, String account) throws IOException {

        Function function = ByteCode.balanceOf(account);

        EthCall ethCall = web3j.ethCall(Transaction.createEthCallTransaction(addressQuery, this.contractAddress,
                FunctionEncoder.encode(function)), DefaultBlockParameterName.LATEST).send();

        return Read.singleValueReturn(ethCall.getValue(), function.getOutputParameters(), BigInteger.class);
    }

    public void shutdown() {
        if (web3j != null)
            web3j.shutdown();

    }

    public Response process(Web3j web3j, String contractAddress, String privateKey, String encodedFunction,
            long blockTime, double factor) throws IOException, TransactionException {

        BigInteger gasPrice = getGasPrice(web3j);
        BigInteger gasLimit = getEstimateGasLimit(Credentials.create(privateKey).getAddress(), contractAddress , encodedFunction);

        TransactionManager txManager = new RawTransactionTx(web3j, Credentials.create(privateKey));

        EthSendTransaction response = txManager.sendTransaction(gasPrice, gasLimit, contractAddress, encodedFunction,
                BigInteger.ZERO);

        TransactionReceiptProcessor receiptProcessor = new PollingTransactionReceiptProcessor(web3j, blockTime,
                TransactionManager.DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH);

        TransactionReceipt receipt = receiptProcessor.waitForTransactionReceipt(response.getResult());

        if (receipt == null) {
            throw new Error("RECEIPT_EMPTY" + response.getResult());
        }

        return new Response(response, receipt);
    }

    public BigInteger getGasPrice(Web3j web3j) throws IOException {
        return web3j.ethGasPrice().send().getGasPrice();

    }

    public BigInteger getEstimateGasLimit(String from, String to, String data) throws IOException {

        Transaction tx = Transaction.createEthCallTransaction(from, to, data);
        EthEstimateGas ethEstimateGas = web3j.ethEstimateGas(tx).send();
        return ethEstimateGas.getAmountUsed();
    }
}
