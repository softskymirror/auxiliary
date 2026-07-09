package com.system;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 配置加解密工具类，支持对配置文件中的敏感字段进行保护。
 * <p>
 * 支持两种安全模式：
 * <ul>
 *   <li><b>AES 加密</b>：配置值格式为 {@code ENC(Base64密文)}，运行时自动解密</li>
 *   <li><b>环境变量引用</b>：配置值格式为 {@code ${ENV:变量名}}，运行时从环境变量读取</li>
 * </ul>
 * 普通明文值会原样返回，保持向后兼容。
 * <p>
 * AES 密钥从环境变量 {@code APP_SECRET_KEY} 读取；未设置时使用内置默认密钥（仅限开发环境，生产环境务必设置）。
 *
 * @author softskymirror
 */
public class CryptoUtils {

    /** 加密值前缀，如 ENC(xxx) */
    private static final String ENC_PREFIX = "ENC(";
    private static final String ENC_SUFFIX = ")";

    /** 环境变量引用前缀，如 ${ENV:VAR_NAME} */
    private static final String ENV_PREFIX = "${ENV:";
    private static final String ENV_SUFFIX = "}";

    /** 密钥来源的环境变量名 */
    private static final String SECRET_KEY_ENV = "APP_SECRET_KEY";

    /** 开发环境默认密钥（生产环境必须通过环境变量覆盖） */
    private static final String DEFAULT_SECRET = "JavaTool-Auxiliary-DefaultKey-2024";

    // AES 加密参数
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String KEY_DERIVATION = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH = 256;       // AES-256
    private static final int ITERATION_COUNT = 65536;
    private static final int IV_LENGTH = 16;          // AES CBC IV 长度

    // 固定 salt（与密钥派生配合使用，保证同一密钥派生出同一结果）
    private static final byte[] SALT = "JavaToolSalt".getBytes(StandardCharsets.UTF_8);

    private CryptoUtils() {}

    // ========== 核心方法：解析配置值 ==========

    /**
     * 智能解析配置值，自动识别加密值和环境变量引用。
     * <p>
     * 处理优先级：
     * <ol>
     *   <li>若值为 {@code ENC(...)} 格式，进行 AES 解密后返回</li>
     *   <li>若值为 {@code ${ENV:VAR}} 格式，从环境变量读取后返回</li>
     *   <li>否则原样返回（明文兼容模式）</li>
     * </ol>
     *
     * @param rawValue 配置文件中的原始值
     * @return 解析后的真实值
     * @throws RuntimeException 解密失败或环境变量不存在时抛出
     */
    public static String resolve(String rawValue) {
        if (rawValue == null || rawValue.isEmpty()) {
            return rawValue;
        }
        String trimmed = rawValue.trim();
        if (trimmed.startsWith(ENC_PREFIX) && trimmed.endsWith(ENC_SUFFIX)) {
            String cipherBase64 = trimmed.substring(ENC_PREFIX.length(), trimmed.length() - ENC_SUFFIX.length());
            return decrypt(cipherBase64);
        }
        if (trimmed.startsWith(ENV_PREFIX) && trimmed.endsWith(ENV_SUFFIX)) {
            String varName = trimmed.substring(ENV_PREFIX.length(), trimmed.length() - ENV_SUFFIX.length());
            String envValue = System.getenv(varName);
            if (envValue == null) {
                throw new RuntimeException("环境变量不存在: " + varName);
            }
            return envValue;
        }
        // 明文，原样返回
        return rawValue;
    }

    // ========== AES 加密 / 解密 ==========

    /**
     * 使用 AES-256-CBC 加密明文，返回 Base64 编码的密文。
     *
     * @param plaintext 明文
     * @return Base64 编码的密文
     */
    public static String encrypt(String plaintext) {
        try {
            byte[] iv = generateIv();
            SecretKey key = deriveKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            // 将 IV（16字节）和密文拼接后 Base64 编码
            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("AES 加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用 AES-256-CBC 解密 Base64 编码的密文。
     *
     * @param cipherBase64 Base64 编码的密文（前16字节为 IV）
     * @return 解密后的明文
     */
    public static String decrypt(String cipherBase64) {
        try {
            byte[] combined = Base64.getDecoder().decode(cipherBase64);
            if (combined.length < IV_LENGTH) {
                throw new IllegalArgumentException("密文长度不足，可能已损坏");
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            SecretKey key = deriveKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES 解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 加密并生成可直接写入配置文件的格式：{@code ENC(Base64密文)}。
     *
     * @param plaintext 明文
     * @return 如 {@code ENC(aBcDeF==)} 格式的字符串
     */
    public static String encryptForConfig(String plaintext) {
        return ENC_PREFIX + encrypt(plaintext) + ENC_SUFFIX;
    }

    // ========== 内部方法 ==========

    /**
     * 从环境变量或默认值获取密钥，使用 PBKDF2 派生 AES-256 密钥。
     */
    private static SecretKey deriveKey() throws Exception {
        String secret = System.getenv(SECRET_KEY_ENV);
        if (secret == null || secret.isEmpty()) {
            secret = DEFAULT_SECRET;
            System.err.println("[安全警告] 未设置环境变量 " + SECRET_KEY_ENV + "，使用默认密钥（仅限开发环境）");
        }
        PBEKeySpec keySpec = new PBEKeySpec(secret.toCharArray(), SALT, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION);
        byte[] keyBytes = factory.generateSecret(keySpec).getEncoded();
        return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }

    /**
     * 生成随机 IV（初始化向量）。
     */
    private static byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    // ========== 命令行工具：用于加密密码 ==========

    /**
     * 命令行入口，用于加密明文密码。
     * <p>
     * 用法：{@code java -cp ... com.system.CryptoUtils encrypt "你的明文密码"}
     * <p>
     * 输出可直接写入配置文件的 {@code ENC(...)} 格式字符串。
     *
     * @param args 命令行参数，args[0] = "encrypt"，args[1] = 明文
     */
    public static void main(String[] args) {
        if (args.length < 2 || !"encrypt".equals(args[0])) {
            System.out.println("用法: java com.system.CryptoUtils encrypt <明文密码>");
            System.out.println("示例: java com.system.CryptoUtils encrypt 123456");
            return;
        }
        String plaintext = args[1];
        String encrypted = encryptForConfig(plaintext);
        System.out.println("加密结果（写入配置文件 password 字段）：");
        System.out.println(encrypted);
        System.out.println();
        System.out.println("验证解密...");
        String resolved = resolve(encrypted);
        System.out.println("解密结果: " + resolved);
        if (plaintext.equals(resolved)) {
            System.out.println("验证通过！");
        } else {
            System.err.println("验证失败！解密结果与原文不一致");
        }
    }
}
