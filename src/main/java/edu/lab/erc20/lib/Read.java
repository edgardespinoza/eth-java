package edu.lab.erc20.lib;

import java.util.List;

import org.web3j.abi.DefaultFunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.tx.exceptions.ContractCallException;

public class Read {



  @SuppressWarnings("rawtypes")
  private static Type singleValueReturn(String value, List<TypeReference<Type>> types) {

      List<Type> values = new DefaultFunctionReturnDecoder().decodeFunctionResult(value, types);

      if (!values.isEmpty()) {
          return  values.get(0);
      } else {
          return null;
      }
  }

  @SuppressWarnings("all")
  public static <R> R singleValueReturn(String data, List<TypeReference<Type>> types, Class<R> returnType)  {
   
      Type result = singleValueReturn(data, types);
      
      if (result == null) {
          throw new ContractCallException("Empty value (0x) returned from contract");
      }

      Object value = result.getValue();
      if (returnType.isAssignableFrom(value.getClass())) {
          return (R) value;
      } else if (result.getClass().equals(Address.class) && returnType.equals(String.class)) {
          return (R) result.toString(); // cast isn't necessary
      } else if (returnType.equals(result.getClass())) {
          return (R) result; // cast isn't necessary
      } else {
          throw new ContractCallException(
                  "Unable to convert response: "
                          + value
                          + " to expected type: "
                          + returnType.getSimpleName());
                        } 
  }
}
