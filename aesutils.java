package com.routdata.rd3f.core.api.common.utils;


import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import com.routdata.rd3f.core.api.common.exception.SystemException;

/**
 * 
 * 名称：AES加解密类 <br>
 * 功能：对字符串进行AES加解密 <br/>
 * <br/>
 * 加密范例：String encrypt = AesUtil.getInstance().encrypt(SALT, PLAIN_TEXT);
 * 解密范例：String decrypt = AesUtil.getInstance().decrypt(SALT, CIPHER_TEXT);
 * Implement 256-bit version like: http://securejava.wordpress.com/2012/10/25/aes-256/
 * @since JDK 1.7
 * @see 
 * @author zhoucl
 */

public class AesUtils {
    private final int    keySize;
    private final int    iterationCount;
    private final Cipher cipher;
    
    private static final String IV = "D27D5D9927726BDEFE7510B1BDD3D137";
    private static final String SALT = "3FF2EC019C627B945225DEBAD71A01B6985FE84C95A70EB132882F88C0A59A55";    
    /**
	 * 默认的AES加密串
	 */
	public static final String DEFAULT_AES_PDW = "D27D5D9927726BDEFE7510B1BDD3D137";
	
	public static final String AES_MODE = "AES/CBC/"+"PKCS5"+"Padding";
	
    
    private static class SingletonInstance {
        private static final AesUtils INSTANCE = new AesUtils();
    }

    /**
     * 
     * <b>名称</b>：获取AesUtil的实例<br/><br/>
     * <b>说明</b>：获取单例的AesUtil对象 <br/>
     * <br/>
     *
     * @return
     *
     * @see
     */
    public static AesUtils getInstance() {
        return SingletonInstance.INSTANCE;
    }
    
    /**
     * 默认128和1000次迭代
     * 构造方法： AesUtil.
     *
     */
    public AesUtils() {
        this(128, 1000);
    }
    
    /**
     * 
     * 构造方法： AesUtil.
     *
     * @param keySize
     * @param iterationCount
     */
    public AesUtils(int keySize, int iterationCount) {
        this.keySize = keySize;
        this.iterationCount = iterationCount;
        try {
            cipher = Cipher.getInstance(AES_MODE);
        }
        catch (
            NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw fail(e);
        }
    }
    
    /**
     * <b>名称</b>：AES加密方法<br/><br/>
     * <b>说明</b>：以salt作为key对字符串进行加密 <br/>
     * <br/>
     *
     * @param salt 加密盐
     * @param plaintext 待加密的文本
     * @return 以十六进制表达的字符串
     *
     * @see
     */
    public String encrypt(String passPhrase, String plaintext){
        return this.encrypt(SALT, IV, passPhrase, plaintext);
    }
    
    public String encrypt(String plaintext){
        return this.encrypt(DEFAULT_AES_PDW, plaintext);
    }
    
    
    /*public static void main(String[] args) {
    	String pass = AesUtils.getInstance().encrypt("zhoucl");
    	String pa = AesUtils.getInstance().decrypt("fd132c0c287d2cc10428393371d7ef8b");
    	System.out.println(pass+pa);
	}*/
    
    /**
     * 
     * <b>名称</b>：AES解密方法 <br/><br/>
     * <b>说明</b>：以salt作为key对字符串进行解密 <br/>
     * <br/>
     *
     * @param salt 解密字符串
     * @param ciphertext 待解密的16进制表达字符串
     * @return 解密后的文本
     *
     * @see
     */
    public String decrypt(String passPhrase, String ciphertext){
    	//屏蔽掉解密错误，如果解密出错则返回
		try {
			return this.decrypt(SALT, IV, passPhrase, ciphertext);
		} catch (IllegalStateException e) {
			return null;
		}catch(Exception e){
			return null;
		}
    }
    /**
     * 
     * <b>名称</b>：AES解密方法 <br/><br/>
     * <b>说明</b>：以salt作为key对字符串进行解密 <br/>
     * <br/>
     *
     * @param plaintext 解密字符串
     * @return 解密后的文本
     *
     * @see
     */    
    public String decrypt(String plaintext){
        return this.decrypt(DEFAULT_AES_PDW, plaintext);
    }
    
    public String encrypt(String salt, String iv, String passphrase, String plaintext) {
        try {
            SecretKey key = generateKey(salt, passphrase);
            byte[] encrypted = doFinal(Cipher.ENCRYPT_MODE, key, iv, plaintext.getBytes("UTF-8"));
            return hex(encrypted);
        }
        catch (UnsupportedEncodingException e) {
            throw fail(e);
        }
    }

    public String decrypt(String salt, String iv, String passphrase, String ciphertext) {
        try {
            SecretKey key = generateKey(salt, passphrase);
            byte[] decrypted = doFinal(Cipher.DECRYPT_MODE, key, iv, hex(ciphertext));
            return new String(decrypted, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw fail(e);
        }
    }

    private byte[] doFinal(int encryptMode, SecretKey key, String iv, byte[] bytes) {
        try {
            cipher.init(encryptMode, key, new IvParameterSpec(hex(iv)));
            return cipher.doFinal(bytes);
        }
        catch (
            InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException
            | BadPaddingException e) {
            throw fail(e);
        }
    }

    private SecretKey generateKey(String salt, String passphrase) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), hex(salt), iterationCount,
                keySize);
            SecretKey key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
            return key;
        }
        catch (
            NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw fail(e);
        }
    }

    public static String random(int length) {
        byte[] salt = new byte[length];
        new SecureRandom().nextBytes(salt);
        return hex(salt);
    }

    public static String base64(byte[] bytes) {
        return Base64.encodeBase64String(bytes);
    }

    public static byte[] base64(String str) {
        return Base64.decodeBase64(str);
    }

    public static String hex(byte[] bytes) {
        return Hex.encodeHexString(bytes);
    }

    public static byte[] hex(String str) {
        try {
            return Hex.decodeHex(str.toCharArray());
        }
        catch (DecoderException e) {
            throw new IllegalStateException(e);
        }
    }

    private IllegalStateException fail(Exception e) {
        return new IllegalStateException(e);
    }
    
	/**
	 * 加密微信密码串
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public static String weixinCrypt(String key, String value) {
		try {
			SecretKey secretKey = new SecretKeySpec(key.getBytes(), "AES");
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			// 先按utf-8解码字符数组，然后加密，最后base64编码
			byte[] bytes = cipher.doFinal(value.getBytes("UTF-8"));
			return Base64.encodeBase64String(bytes);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException | UnsupportedEncodingException e) {
			throw new SystemException("加密微信报文错误");
		}
	}
	
	
	   /**
     * AES解密
     * @param key
     * @param encrypted
     * @return
     */
	public static String weixinDeCrypt(String key, String encrypted) {
		try {
			SecretKey secretKey = new SecretKeySpec(key.getBytes(), "AES");
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			// 先按utf-8解码字符数组，然后加密，最后base64编码
			byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));
			return new String(original, "UTF-8");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException | UnsupportedEncodingException e) {
			throw new SystemException("解密微信报文错误");
		}
	}

}
