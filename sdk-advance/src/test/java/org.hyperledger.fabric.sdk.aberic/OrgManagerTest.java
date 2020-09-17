package org.hyperledger.fabric.sdk.aberic;

import jdk.nashorn.internal.scripts.JO;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hyperledger.fabric.sdk.aberic.OrgManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author 胡柏吉
 * @version 1.0
 * @description TODO
 * @date 2020-09-16 上午10:21
 * @email drbjhu@163.com
 */
public class OrgManagerTest {

    protected static Logger logger = LogManager.getLogger(OrgManagerTest.class);

    @Test
    public void test_e2e_cli() throws Exception {
        JSONObject json = new JSONObject();
        json.put("fcn","query");
        JSONArray arg = new JSONArray();
        arg.put(0,"a");
//        arg.put(1,"b");
        json.put("Args",arg);
        logger.debug(json.toString());
        String fcn = json.getString("fcn");
        JSONArray arrayJson = json.getJSONArray("Args");
        Map<String, String> resultMap;
        int length = arrayJson.length();
        String[] argArray = new String[length];
        for (int i = 0; i < length; i++) {
            argArray[i] = arrayJson.getString(i);
            logger.debug(argArray[i]);
        }
        try {
            FabricManager fabricManager = obtainFabricManager();
            switch (fcn) {
                case "install":
                    resultMap = fabricManager.install();
                    break;
                case "instantiate":
                    resultMap = fabricManager.instantiate(argArray);
                    break;
                case "upgrade":
                    resultMap = fabricManager.upgrade(argArray);
                    break;
                case "query":
                    // peer chaincode query -C $CHANNEL_NAME -n mycc -c '{"Args":["query","a"]}'
                    resultMap = fabricManager.query(fcn, argArray);
                    break;
                case "invoke":
                    resultMap = fabricManager.invoke(fcn, argArray);
                    break;
                default:
                    throw new RuntimeException(String.format("no fcn was found with name %s", fcn));
            }
            if (resultMap.get("code").equals("error")) {
                logger.debug(responseFail(resultMap.get("data")));
            } else {
                logger.debug(responseSuccess(resultMap.get("data"), resultMap.get("txid")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.debug(responseFail(String.format("请求失败： %s", e.getMessage())));
        }
    }


    private FabricManager obtainFabricManager() throws Exception {
        OrgManager orgManager = new OrgManager();
        orgManager.init("Org1")
                .setUser("Admin",getCryptoConfigPath("aberic"),getChannleArtifactsPath("aberic"))
                .setCA("ca1","http://127.0.0.1:7054")
                .setPeers("org1MSP","org1.example.com")
                .addPeer("peer0.org1.example.com","peer0.org1.example.com","grpc://127.0.0.1:7051","grpc://127.0.0.1:7051",true)
                .setOrderers("example.com")
                .addOrderer("orderer.example.com","grpc://127.0.0.1:7050")
                .setChannel("mychannel")
                .setChainCode("mycc","opt/gopath/","github.com/hyperledger/fabric/examples/chaincode/go/example02/cmd","1.0",9000,120)
                .openTLS(true)
                .openCATLS(false)
                .setBlockListener(map -> {
                    logger.debug(map.get("code"));
                    logger.debug(map.get("data"));
                })
                .add();
        logger.debug("openCATLS = false");
        return orgManager.use("Org1");
    }
    /**
     * 获取channel-artifacts配置路径
     *
     * @return /WEB-INF/classes/fabric/channel-artifacts/
     */
    protected String getChannleArtifactsPath(String module) throws IOException {
        /*String directorys = OrgManager.class.getClassLoader().getResource("fabric").getFile();
        logger.debug("directorys = " + directorys);
        File directory = new File(directorys);
        logger.debug("directory = " + directory.getPath());

        return directory.getPath() + "/" + module + "/channel-artifacts/";*/
        return "/opt/gopath/src/github.com/hyperledger/fabric/examples/e2e_cli/channel-artifacts/";
    }

    /**
     * 获取crypto-config配置路径
     *
     * @return /WEB-INF/classes/fabric/crypto-config/
     */
    protected String getCryptoConfigPath(String module) {
        /*String directorys = OrgManager.class.getClassLoader().getResource("fabric").getFile();
        logger.debug("directorys = " + directorys);
        File directory = new File(directorys);
        logger.debug("directory = " + directory.getPath());

        return directory.getPath() + "/" + module + "/crypto-config/";*/
        return "/opt/gopath/src/github.com/hyperledger/fabric/examples/e2e_cli/crypto-config/";
    }

    int SUCCESS = 200;
    int FAIL = 40029;
    int UN_LOGIN = 8;

    protected String responseSuccess(String result) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", SUCCESS);
        jsonObject.put("result", result);
        return jsonObject.toString();
    }

    protected String responseSuccess(String result, String txid) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", SUCCESS);
        jsonObject.put("result", result);
        jsonObject.put("txid", txid);
        return jsonObject.toString();
    }

    protected String responseSuccess(JSONObject json) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", SUCCESS);
        jsonObject.put("data", json);
        return jsonObject.toString();
    }

    protected String responseFail(String result) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", FAIL);
        jsonObject.put("error", result);
        return jsonObject.toString();
    }

    protected String responseUnLogin() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", UN_LOGIN);
        return jsonObject.toString();
    }
}
