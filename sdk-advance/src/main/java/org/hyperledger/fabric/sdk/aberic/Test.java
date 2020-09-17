package org.hyperledger.fabric.sdk.aberic;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author 胡柏吉
 * @version 1.0
 * @description TODO
 * @date 2020-09-16 下午9:25
 * @email drbjhu@163.com
 */
public class Test {

    public static void main(String[] args) {

        JSONObject student1 = new JSONObject();
        student1.put("name", "zhangsan");
        student1.put("age", 15);

        JSONObject student2 = new JSONObject();
        student2.put("name", "lisi");
        student2.put("age", 16);

        JSONArray students = new JSONArray();
        students.put(student1);
        students.put(student2);

        JSONObject classRoom = new JSONObject();
        classRoom.put("students", students);

        System.out.println(classRoom);
    }
}
