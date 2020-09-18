package cn.edu.ncepu.e2ecli.application;

import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.identity.X509Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoPrimitives;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Set;

/**
 * @author 胡柏吉
 * @version 1.0
 * @description 联盟用户对象
 * @date 2020-09-18 下午2:00
 * @email drbjhu@163.com
 */
public class FabricUser implements User {

    /**
     * 名称
     */
    private final String name;
    /**
     * 用户所属的角色
     */
    private Set<String> roles;
    /**
     * 账户
     */
    private String account;
    /**
     * 从属联盟
     */
    private String affiliation;
    /** 组织 */
//    private String organization;
    /**
     * 注册操作的密密钥
     */
    private String enrollmentSecret;
    /**
     * 会员id
     */
    private final String mspId;
    /**
     * 注册登记操作
     */
    private Enrollment enrollment = null;

    /**
     * 存储配置对象
     */
//    private transient FabricStore fabricStore;
    private String keyForFabricStoreName;

    public FabricUser(String name, String mspId, String keyFile, String certFile) throws ClassNotFoundException, InstantiationException, IllegalAccessException, CryptoException, IOException {
        this.name = name;
        this.mspId = mspId;
        this.enrollment = getEnrollment(keyFile, certFile);
    }

    private Enrollment getEnrollment(String keyFile, String certFile) throws IOException, IllegalAccessException, InstantiationException, ClassNotFoundException, CryptoException {
        byte[] keyPem = Files.readAllBytes(Paths.get(keyFile));
        byte[] certPem = Files.readAllBytes(Paths.get(certFile));
        CryptoPrimitives suite = new CryptoPrimitives();
        PrivateKey privateKey = suite.bytesToPrivateKey(keyPem);
        return new X509Enrollment(privateKey,new String(certPem));
    }

    /**
     * Get the name that identifies the user.
     *
     * @return the user name.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Get the roles to which the user belongs.
     *
     * @return role names.
     */
    @Override
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Get the user's account
     *
     * @return the account name
     */
    @Override
    public String getAccount() {
        return account;
    }

    /**
     * Get the user's affiliation.
     *
     * @return the affiliation.
     */
    @Override
    public String getAffiliation() {
        return affiliation;
    }

    /**
     * Get the user's enrollment certificate information.
     *
     * @return the enrollment information.
     */
    @Override
    public Enrollment getEnrollment() {
        return enrollment;
    }

    /**
     * Get the Membership Service Provider Identifier provided by the user's organization.
     *
     * @return MSP Id.
     */
    @Override
    public String getMspId() {
        return mspId;
    }
}
