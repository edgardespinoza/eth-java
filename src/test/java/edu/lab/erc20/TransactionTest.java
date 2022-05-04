package edu.lab.erc20;

import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Test;
import org.web3j.crypto.Credentials;

import edu.lab.erc20.lib.ByteCode;
import edu.lab.erc20.service.TransactionManagerTx;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransactionTest {
    
    @Test
    public void testTransfer() {

        String owner = "0xcd4d53fe14ef4943887b1f4be2bd175f9979ce3713bf96a9c32a9245fbdc9056";
   
        String addressErc20 =  "0xF89Ee199e4ed0500ce8cE245d59542d693A23AfC";

        TransactionManagerTx erc20 =new  TransactionManagerTx("https://data-seed-prebsc-1-s1.binance.org:8545/", addressErc20);

        try {
           
            String to = "0xB3cedeebaea9F8DeA537cc8e6136B6338104FE0f";

            BigInteger value = encode(BigDecimal.valueOf(1), BigInteger.valueOf(18));

            erc20.transfer(owner, to, value, 1.4);

        } catch (Exception e) {
            log.error("Error.testTransfer", e);
            fail();
        } finally {
            erc20.shutdown();
        }
    }

    public static BigInteger encode(BigDecimal amount, BigInteger decimal){
        BigInteger pow = BigInteger.TEN.pow(decimal.intValue());

        return amount.multiply(new BigDecimal(pow)).toBigInteger();
    }
    
    public static BigDecimal decode(BigInteger amount, BigInteger decimal){
 
        BigInteger pow = BigInteger.TEN.pow(decimal.intValue());

       return new BigDecimal( amount ).divide(new BigDecimal(pow));
    }


    @Test
    public void testGasLimit() {
        String owner = "0xcd4d53fe14ef4943887b1f4be2bd175f9979ce3713bf96a9c32a9245fbdc9056";
   
        String addressErc20 =  "0xF89Ee199e4ed0500ce8cE245d59542d693A23AfC";

        TransactionManagerTx erc20 =new  TransactionManagerTx("https://data-seed-prebsc-1-s1.binance.org:8545/", addressErc20);

        try {
           
            String to = "0xB3cedeebaea9F8DeA537cc8e6136B6338104FE0f";

            String value = ByteCode.transfer(to, BigInteger.valueOf(1));

            BigInteger estimate = erc20.getEstimateGasLimit(Credentials.create(owner).getAddress(), addressErc20, value);

            log.info("estimate:{} ",estimate);

        } catch (Exception e) {
            log.error("Error.testGasLimit", e);
        } finally {
            erc20.shutdown();
        }
    }

    
}
