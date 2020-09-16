package cn.aberic.simple.module.service.impl;

import cn.aberic.simple.module.manager.SimpleManager;
import cn.aberic.simple.module.service.SimpleService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Hex;
import org.hyperledger.fabric.sdk.aberic.FabricManager;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 描述：
 *
 * @author : Aberic 【2018/6/4 15:03】
 */
@Service("simpleService")
public class SimpleServiceImpl implements SimpleService {

    @Override
    public String chainCode(JSONObject json) {
        String fcn = json.getString("fcn");
        String type = json.getString("type");
        JSONArray arrayJson = json.getJSONArray("arg");
        Map<String, String> resultMap;
        int length = arrayJson.size();
        String[] argArray = new String[length];
        for (int i = 0; i < length; i++) {
            argArray[i] = arrayJson.getString(i);
        }
        try {
            FabricManager manager = SimpleManager.obtain().getFabricManager();
            switch (fcn) {
                case "install":
                    resultMap = manager.install();
                    break;
                case "instantiate":
                    resultMap = manager.instantiate(argArray);
                    break;
                case "upgrade":
                    resultMap = manager.upgrade(argArray);
                    break;
                case "invoke":
                    resultMap = manager.invoke(fcn, argArray);
                    break;
                case "query":
                    resultMap = manager.query(fcn, argArray);
                    break;
                default:
                    throw new RuntimeException(String.format("no type was found with name %s", type));
            }
            if (resultMap.get("code").equals("error")) {
                return responseFail(resultMap.get("data"));
            } else {
                return responseSuccess(resultMap.get("data"), resultMap.get("txid"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return responseFail(String.format("请求失败： %s", e.getMessage()));
        }
    }

    @Override
    public String trace(JSONObject json) {
        String fcn = json.getString("fcn");
        String traceId = json.getString("traceId");
        Map<String, String> resultMap;
        try {
            FabricManager manager = SimpleManager.obtain().getFabricManager();
            switch (fcn) {
                case "queryBlockByTransactionID":
                    resultMap = manager.queryBlockByTransactionID(traceId);
                    break;
                case "queryBlockByHash":
                    resultMap = manager.queryBlockByHash(Hex.decodeHex(traceId.toCharArray()));
                    break;
                case "queryBlockByNumber":
                    resultMap = manager.queryBlockByNumber(Long.valueOf(traceId));
                    break;
                case "queryBlockchainInfo":
                    resultMap = manager.getBlockchainInfo();
                    break;
                default:
                    return responseFail("No func found, please check and try again.");
            }
            return responseSuccess(JSONObject.parseObject(resultMap.get("data")));
        } catch (Exception e) {
            e.printStackTrace();
            return responseFail(String.format("请求失败： %s", e.getMessage()));
        }
    }
}
