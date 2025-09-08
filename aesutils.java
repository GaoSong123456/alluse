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
 * ���ƣ�AES�ӽ����� <br>
 * ���ܣ����ַ�������AES�ӽ��� <br/>
 * <br/>
 * ���ܷ�����String encrypt = AesUtil.getInstance().encrypt(SALT, PLAIN_TEXT);
 * ���ܷ�����String decrypt = AesUtil.getInstance().decrypt(SALT, CIPHER_TEXT);
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
	 * Ĭ�ϵ�AES���ܴ�
	 */
	public static final String DEFAULT_AES_PDW = "D27D5D9927726BDEFE7510B1BDD3D137";
	
	public static final String AES_MODE = "AES/CBC/"+"PKCS5"+"Padding";
	
    
    private static class SingletonInstance {
        private static final AesUtils INSTANCE = new AesUtils();
    }

    /**
     * 
     * <b>����</b>����ȡAesUtil��ʵ��<br/><br/>
     * <b>˵��</b>����ȡ������AesUtil���� <br/>
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
     * Ĭ��128��1000�ε���
     * ���췽���� AesUtil.
     *
     */
    public AesUtils() {
        this(128, 1000);
    }
    
    /**
     * 
     * ���췽���� AesUtil.
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
     * <b>����</b>��AES���ܷ���<br/><br/>
     * <b>˵��</b>����salt��Ϊkey���ַ������м��� <br/>
     * <br/>
     *
     * @param salt ������
     * @param plaintext �����ܵ��ı�
     * @return ��ʮ�����Ʊ����ַ���
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
     * <b>����</b>��AES���ܷ��� <br/><br/>
     * <b>˵��</b>����salt��Ϊkey���ַ������н��� <br/>
     * <br/>
     *
     * @param salt �����ַ���
     * @param ciphertext �����ܵ�16���Ʊ���ַ���
     * @return ���ܺ���ı�
     *
     * @see
     */
    public String decrypt(String passPhrase, String ciphertext){
    	//���ε����ܴ���������ܳ����򷵻�
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
     * <b>����</b>��AES���ܷ��� <br/><br/>
     * <b>˵��</b>����salt��Ϊkey���ַ������н��� <br/>
     * <br/>
     *
     * @param plaintext �����ַ���
     * @return ���ܺ���ı�
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
	 * ����΢�����봮
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
			// �Ȱ�utf-8�����ַ����飬Ȼ����ܣ����base64����
			byte[] bytes = cipher.doFinal(value.getBytes("UTF-8"));
			return Base64.encodeBase64String(bytes);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException | UnsupportedEncodingException e) {
			throw new SystemException("����΢�ű��Ĵ���");
		}
	}
	
	
	   /**
     * AES����
     * @param key
     * @param encrypted
     * @return
     */
	public static String weixinDeCrypt(String key, String encrypted) {
		try {
			SecretKey secretKey = new SecretKeySpec(key.getBytes(), "AES");
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			// �Ȱ�utf-8�����ַ����飬Ȼ����ܣ����base64����
			byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));
			return new String(original, "UTF-8");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
				| BadPaddingException | UnsupportedEncodingException e) {
			throw new SystemException("����΢�ű��Ĵ���");
		}
	}

}
