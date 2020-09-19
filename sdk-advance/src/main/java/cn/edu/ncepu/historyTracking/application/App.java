package cn.edu.ncepu.historyTracking.application;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;

public class App{
  protected static Logger logger = LogManager.getLogger(App.class);

  private User user;
  private HFClient client;
  private Channel channel;
  
  public User loadUser(String name,String mspId) throws Exception{
    String networkPath = this.getClass().getClassLoader().getResource("historyTracking").getFile()+"/network"; // 定位resources文件夹下的文件路径
    logger.debug(networkPath);
    File keystore = Paths.get(networkPath, "crypto-config","/peerOrganizations/org1.example.com", String.format("/users/%s@%s/msp/keystore", "Admin", "org1.example.com")).toFile();
    File keyFile = findFileSk(keystore); // 查找keystore路径下的以_sk结尾的文件
    if(null == keyFile ){
      throw new Exception("no secret key found");
    }
    String keyFileName = keystore.getPath() + "/" + keyFile.getName();

    File signCerts = Paths.get(networkPath, "crypto-config","/peerOrganizations/org1.example.com", String.format("/users/%s@%s/msp/signcerts", "Admin", "org1.example.com")).toFile();
    logger.debug(signCerts.getPath());
    File certFiles = findFileCert(signCerts);
    if(null == certFiles)
      throw new Exception("no signature cert found");
    String certFileName = signCerts.getPath() + "/" + certFiles.getName();
    
    this.user = new LocalUser(name,mspId,keyFileName,certFileName);
    return this.user;
  }

  /**
   * 从指定路径中获取后缀为 _sk 的文件，且该路径下有且仅有该文件
   *
   * @param directory 指定路径
   * @return File
   */
  private File findFileSk(File directory) {
    File[] matches = directory.listFiles((dir, name) -> name.endsWith("_sk"));
    if (null == matches) {
      throw new RuntimeException(String.format("Matches returned null, does %s directory exist?", directory.getAbsoluteFile().getName()));
    }
    if (matches.length != 1) {
      throw new RuntimeException(String.format("Expected in %s only 1 sk file, but found %d", directory.getAbsoluteFile().getName(), matches.length));
    }
    return matches[0];
  }

  /**
   * 从指定路径中获取后缀为 .pem 的文件，且该路径下有且仅有该文件
   *
   * @param directory 指定路径
   * @return File
   */
  private File findFileCert(File directory) {
    File[] matches = directory.listFiles((dir, name) -> name.endsWith(".pem"));
    if (null == matches) {
      throw new RuntimeException(String.format("Matches returned null, does %s directory exist?", directory.getAbsoluteFile().getName()));
    }
    if (matches.length != 1) {
      throw new RuntimeException(String.format("Expected in %s only 1 pem file, but found %d", directory.getAbsoluteFile().getName(), matches.length));
    }
    return matches[0];
  }

  public void initChannel() throws Exception{
    if(this.user == null) throw new Exception("user not loaded");

    this.client = HFClient.createNewInstance();
    this.client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
    this.client.setUserContext(this.user);
    
    this.channel = this.client.newChannel("ch1");
    Peer peer = this.client.newPeer("peer0","grpc://127.0.0.1:7051");
    this.channel.addPeer(peer);
    Orderer orderer = this.client.newOrderer("orderer1","grpc://127.0.0.1:7050");
    this.channel.addOrderer(orderer);
    this.channel.initialize();
  }

  public void query(String ccname, String fcn,String...args) throws Exception{
    System.out.format("query %s %s...\n",ccname,fcn);
    
    QueryByChaincodeRequest req = this.client.newQueryProposalRequest();
    ChaincodeID cid = ChaincodeID.newBuilder().setName(ccname).build();
    req.setChaincodeID(cid);
    req.setFcn(fcn);
    req.setArgs(args);
    
    Collection<ProposalResponse> rspc = channel.queryByChaincode(req);
    
    for(ProposalResponse rsp: rspc){
      logger.info(String.format("status: %d \n",rsp.getStatus().getStatus()));
      logger.info(String.format("message: %s\n",rsp.getMessage()));
      logger.info(String.format("payload: %s\n",rsp.getProposalResponse().getResponse().getPayload().toStringUtf8()));
    }
  }
  
  public void invoke(String ccname,String fcn,String... args) throws Exception{
    logger.info(String.format("invoke %s %s...\n",ccname,fcn));
    
    TransactionProposalRequest req = this.client.newTransactionProposalRequest();
    ChaincodeID cid = ChaincodeID.newBuilder().setName(ccname).build();    
    req.setChaincodeID(cid);
    req.setFcn(fcn);
    req.setArgs(args);
    
    Collection<ProposalResponse> rspc = channel.sendTransactionProposal(req);
    TransactionEvent event = channel.sendTransaction(rspc).get();

    logger.info(String.format("txid: %s\n", event.getTransactionID()));
    logger.info(String.format("valid: %b\n", event.isValid()));
  }
  
  public void start() throws Exception{
    loadUser("admin","Org1MSP");
    initChannel();

    query("wizcc","value","a");
    invoke("wizcc","inc","b","10");
    query("wizcc","value","b");

    query("wizcc","getAssetHistory","b");

    /*String id = "12345";
    invoke("wizcc","createAsset",id,"Tommy","a necklace");
    query("wizcc","getAsset",id);
    invoke("wizcc","transferAsset",id,"Mary");
    query("wizcc","getAssetHistory",id);*/
  }
  
  public static void main(String[] args) throws Exception{
    logger.info(String.format("wiz dapp"));
    new App().start();         
  }
}