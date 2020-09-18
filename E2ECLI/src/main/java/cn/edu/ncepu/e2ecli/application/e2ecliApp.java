package cn.edu.ncepu.e2ecli.application;


import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;

/**
 * @author 胡柏吉
 * @version 1.0
 * @description TODO
 * @date 2020-09-18 下午2:33
 * @email drbjhu@163.com
 */
public class e2ecliApp {
    private static final Logger logger = LogManager.getLogger(e2ecliApp.class);
    private User user;
    private HFClient client;
    private Channel channel;

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

    /**
     * @description: init Fabric user
     * @param: name
     * @param: mspId
     * @return:
     * @throw:
     */
    private void loadUser(String name, String mspId) throws Exception {
        String e2ecliPath = this.getClass().getClassLoader().getResource("e2e_cli").getPath();
        logger.debug(e2ecliPath);

        File keystore = Paths.get(e2ecliPath, "crypto-config/peerOrganizations/org1.example.com/users/", String.format("%s@%s/msp/keystore", "Admin", "org1.example.com")).toFile();
        File keyFile = findFileSk(keystore);
        if (null == keyFile) {
            throw new Exception("no secret key found");
        }
        String keyPathName = keyFile.getPath();

        File signCerts = Paths.get(e2ecliPath, "crypto-config/peerOrganizations/org1.example.com/users/", String.format("%s@%s/msp/signcerts", "Admin", "org1.example.com")).toFile();
        File certFile = findFileCert(signCerts);
        if (null == certFile) {
            throw new Exception("no signature cert found");
        }
        String certPathName = certFile.getPath();
        this.user = new FabricUser(name, mspId, keyPathName, certPathName);
    }

    private void initClient() throws Exception {
        if (null == this.user) {
            throw new Exception("user not loaded");
        }
        this.client = HFClient.createNewInstance();
        this.client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        this.client.setUserContext(this.user);
    }

    private void initChannel() throws InvalidArgumentException, TransactionException {
        this.channel = this.client.newChannel("mychannel");
        Peer peer = this.client.newPeer("peer0", "grpc://127.0.0.1:7051");
        this.channel.addPeer(peer);
        Orderer orderer = this.client.newOrderer("orderer", "grpc://127.0.0.1:7050");
        this.channel.addOrderer(orderer);
        this.channel.initialize();
    }

    private void query(String ccName, String fcn, String... args) throws ProposalException, InvalidArgumentException {
        logger.info(String.format("query %s %s", ccName,fcn));

        QueryByChaincodeRequest req = this.client.newQueryProposalRequest();
        ChaincodeID cid = ChaincodeID.newBuilder().setName(ccName).build();
        req.setChaincodeID(cid);
        req.setFcn(fcn);
        req.setArgs(args);

        Collection <ProposalResponse> rspc = this.channel.queryByChaincode(req);
        for (ProposalResponse rsp : rspc) {
            logger.info("status: "+ rsp.getStatus().getStatus());
            logger.info("message: " + rsp.getMessage());
            logger.info("payload: " + rsp.getProposalResponse().getResponse().getPayload().toStringUtf8());
        }
    }

    private void start() throws Exception {
        loadUser("admin", "Org1MSP");
        initClient();
        initChannel();
        query("mycc", "query", "query","a");
    }

    public static void main(String[] args) throws Exception {
        logger.info("e2ecliApp");
        new e2ecliApp().start();
    }

}
