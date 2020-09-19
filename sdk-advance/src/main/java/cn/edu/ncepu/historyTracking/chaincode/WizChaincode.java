package cn.edu.ncepu.historyTracking.chaincode;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.hyperledger.fabric.shim.ledger.KeyModification;

import com.google.protobuf.ByteString;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.List;


public class WizChaincode extends ChaincodeBase {
  static private final Logger logger = LogManager.getLogger(WizChaincode.class);

  @Override
  public Response init(ChaincodeStub stub){
    System.out.println("--------init--------");
    stub.putStringState("a","100");
    stub.putStringState("b","200");
    return newSuccessResponse("init");
  }
  
  @Override
  public Response invoke(ChaincodeStub stub){
    System.out.println("--------invoke--------");
    String fcn = stub.getFunction();
    List<String> args = stub.getParameters();
    System.out.println("===>" + fcn);
    switch(fcn){
      case "createAsset": return createAsset(stub,args);
      case "transferAsset": return transferAsset(stub,args);
      case "getAsset": return getAsset(stub,args);
      case "getAssetHistory": return getAssetHistory(stub,args);
      case "inc": return inc(stub,args);
      case "reset": return reset(stub,args);
      case "value": return value(stub,args);
    }
    return newErrorResponse("unimplemented method");
  }

  // ---------------------------History tracking--------------------
  private Response createAsset(ChaincodeStub stub,List<String> args){
    String id = args.get(0);
    String owner = args.get(1);
    String desc = args.get(2);
    
    String assetStr = stub.getStringState(id);
    System.out.println("=> " + assetStr);
    if(assetStr != null && assetStr.length() > 0 )
      return newErrorResponse(String.format("asset %s already exists",id));
    JSONObject asset = new JSONObject();
    asset.put("id",id);
    asset.put("owner",owner);
    asset.put("desc",desc);
    stub.putStringState(id,asset.toString());
    return newSuccessResponse(String.format("asset %s created",id));
  }
  
  private Response transferAsset(ChaincodeStub stub,List<String> args){
    String id = args.get(0);
    String newOwner = args.get(1);
    
    String assetStr = stub.getStringState(id);
    if(assetStr == null || assetStr.length() == 0)
      return newErrorResponse(String.format("asset %s not found",id));
    JSONObject asset = new JSONObject(assetStr);
    asset.put("owner",newOwner);
    stub.putStringState(id,asset.toString());
    return newSuccessResponse(String.format("asset %s updated",id));
  }
  
  private Response getAsset(ChaincodeStub stub,List<String> args){
    String id = args.get(0);
    
    String assetStr = stub.getStringState(id);
    if(assetStr == null || assetStr.length() == 0) return newErrorResponse(String.format("asset %s not found",id));
    return newSuccessResponse(assetStr,ByteString.copyFromUtf8(assetStr).toByteArray());
  }
  
  private Response getAssetHistory(ChaincodeStub stub,List<String> args){
    String id = args.get(0);
    
    JSONArray ja = new JSONArray();
    QueryResultsIterator<KeyModification>  iterator = stub.getHistoryForKey(id);
    for(KeyModification m: iterator){
      logger.info(m.getTxId());
      JSONObject jo = new JSONObject();
      jo.put("txid",m.getTxId());
      jo.put("value",m.getStringValue());
      jo.put("timestamp",m.getTimestamp().toString());
      jo.put("deleted",Boolean.valueOf(m.isDeleted()));
      ja.put(jo);
    }
    String str = ja.toString();
    return newSuccessResponse(str,ByteString.copyFromUtf8(str).toByteArray());
  }

  // ---------------------------mple counter--------------------
  private Response inc(ChaincodeStub stub, List<String> args){
    int step = 1;
    step = Integer.parseInt(args.get(1));
    String valueStr = stub.getStringState(args.get(0));
    int value = Integer.parseInt(valueStr);
    value += step;
    stub.putStringState(args.get(0),Integer.toString(value));
    return newSuccessResponse(String.format("updated => %d",value));
  }

  private Response reset(ChaincodeStub stub, List<String> args){
    for (int i = 0; i<args.size();i++) {
      stub.putStringState(args.get(i),"0");
    }
    return  newSuccessResponse("reset to zero");
  }

  private Response value(ChaincodeStub stub, List<String> args) {
    String value = null;
    for (int i = 0; i<args.size();i++) {
      value = stub.getStringState(args.get(i));
    }
    return newSuccessResponse(value, ByteString.copyFromUtf8(value).toByteArray());
  }

  // -----------------------------------------------
  
  public static void main(String[] args){
    if (null != args && args.length > 0){
      for (int i = 0; i < args.length; i++) {
        System.out.println(args[i]);
      }
    }else {
      args = new String[4];
      args[0] = "--id";
      args[1] = "wizcc:0";
      args[2] = "--peer.address";
      args[3] = "127.0.0.1:7052";
    }
    logger.info("wizcc");
    new WizChaincode().start(args);
  }
}