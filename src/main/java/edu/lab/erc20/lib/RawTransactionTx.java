package edu.lab.erc20.lib;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.transaction.type.TransactionType;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetCode;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpType;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.exceptions.ContractCallException;
import org.web3j.tx.exceptions.TxHashMismatchException;
import org.web3j.utils.Numeric;
import org.web3j.utils.TxHashVerifier;

public class RawTransactionTx extends TransactionManager {

	private final Web3j web3j;
	final Credentials credentials;

	protected TxHashVerifier txHashVerifier = new TxHashVerifier();

	public RawTransactionTx(Web3j web3j, Credentials credentials) {
		super(web3j, credentials.getAddress());

		this.web3j = web3j;
		this.credentials = credentials;
	}

	protected BigInteger getNonce() throws IOException {
		EthGetTransactionCount ethGetTransactionCount = web3j
				.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST).send();

		return ethGetTransactionCount.getTransactionCount();
	}


	@Override
	public EthSendTransaction sendTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data,
			BigInteger value, boolean constructor) throws IOException {

		BigInteger nonce = getNonce();

		RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, value,
				data);

		return signAndSend(rawTransaction);
	}


	@Override
	public String sendCall(String to, String data, DefaultBlockParameter defaultBlockParameter) throws IOException {
		EthCall ethCall = web3j
				.ethCall(Transaction.createEthCallTransaction(getFromAddress(), to, data), defaultBlockParameter)
				.send();

		assertCallNotReverted(ethCall);
		return ethCall.getValue();
	}

	static void assertCallNotReverted(EthCall ethCall) {
		if (ethCall.isReverted()) {
			throw new ContractCallException(String.format(REVERT_ERR_STR, ethCall.getRevertReason()));
		}
	}

	@Override
	public EthGetCode getCode(final String contractAddress, final DefaultBlockParameter defaultBlockParameter)
			throws IOException {
		return web3j.ethGetCode(contractAddress, defaultBlockParameter).send();
	}

	/*
	 * @param rawTransaction a RawTransaction istance to be signed
	 * 
	 * @return The transaction signed and encoded without ever broadcasting it
	 */
	public String sign(RawTransaction rawTransaction) {
	 
		byte[] signedMessage = signMessage(rawTransaction, credentials);

		return Numeric.toHexString(signedMessage);
	}

	public static SignatureData signMessage(byte[] message, ECKeyPair keyPair, boolean needToHash) {
        BigInteger publicKey = keyPair.getPublicKey();
        byte[] messageHash;
        if (needToHash) {
            messageHash = Hash.sha3(message);
        } else {
            messageHash = message;
        }

        ECDSASignature sig = keyPair.sign(messageHash);

        // Now we have to work backwards to figure out the recId needed to recover the signature.
        int recId = -1;
        for (int i = 0; i < 4; i++) {
            BigInteger k = Sign.recoverFromSignature(i, sig, messageHash);
            if (k != null && k.equals(publicKey)) {
                recId = i;
                break;
            }
        }
        if (recId == -1) {
            throw new RuntimeException(
                    "Could not construct a recoverable key. Are your credentials valid?");
        }

        int headerByte = recId + 27;

        // 1 header + 32 bytes for R + 32 bytes for S
        byte[] v = new byte[] {(byte) headerByte};
        byte[] r = Numeric.toBytesPadded(sig.r, 32);
        byte[] s = Numeric.toBytesPadded(sig.s, 32);
        return new SignatureData(v, r, s);
    }

	public static byte[] signMessage(RawTransaction rawTransaction, Credentials credentials) {
        byte[] encodedTransaction = TransactionEncoder.encode(rawTransaction);
		
        Sign.SignatureData signatureData =
                signMessage(encodedTransaction, credentials.getEcKeyPair(), true);

        return encode(rawTransaction, signatureData);
    }
	private static byte[] encode(RawTransaction rawTransaction, Sign.SignatureData signatureData) {
        List<RlpType> values = TransactionEncoder.asRlpValues(rawTransaction, signatureData);
        RlpList rlpList = new RlpList(values);
        byte[] encoded = RlpEncoder.encode(rlpList);
        if (!rawTransaction.getType().equals(TransactionType.LEGACY)) {
            return ByteBuffer.allocate(encoded.length + 1)
                    .put(rawTransaction.getType().getRlpType())
                    .put(encoded)
                    .array();
        }
        return encoded;
    }
	public EthSendTransaction signAndSend(RawTransaction rawTransaction) throws IOException {
		String hexValue = sign(rawTransaction);

		EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();

		if (ethSendTransaction != null && !ethSendTransaction.hasError()) {
			String txHashLocal = Hash.sha3(hexValue);
			String txHashRemote = ethSendTransaction.getTransactionHash();
			if (!txHashVerifier.verify(txHashLocal, txHashRemote)) {
				throw new TxHashMismatchException(txHashLocal, txHashRemote);
			}
		}

		return ethSendTransaction;
	}

	@Override
	public EthSendTransaction sendEIP1559Transaction(long chainId, BigInteger maxPriorityFeePerGas,
			BigInteger maxFeePerGas, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor)
			throws IOException {

		BigInteger nonce = getNonce();

		RawTransaction rawTransaction = RawTransaction.createTransaction(chainId, nonce, gasLimit, to, value, data,
				maxPriorityFeePerGas, maxFeePerGas);

		return signAndSend(rawTransaction);
	}

}
