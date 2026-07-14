package com.system;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Memory;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.win32.W32APITypeMapper;

import java.nio.charset.StandardCharsets;

/**
 * Windows 凭据管理器工具类。
 * <p>
 * 封装对 Windows Credential Manager（凭据管理器）的读写操作，
 * 用于在本地安全存储数据库密码等敏感信息，避免明文写入配置文件。
 * <p>
 * 凭据以 Generic 类型存储，持久化为本地计算机，重启后仍然可用。
 *
 * @author softskymirror
 */
public class WindowsCredentialUtils {

    private WindowsCredentialUtils() {}

    /**
     * Windows 凭据类型：通用凭据
     */
    private static final int CRED_TYPE_GENERIC = 1;

    /**
     * 持久化方式：本地计算机，用户登录后可读取
     */
    private static final int CRED_PERSIST_LOCAL_MACHINE = 2;

    /**
     * JNA 接口映射：Advapi32.dll 中的凭据 API
     */
    public interface CredAdvapi32 extends StdCallLibrary {
        CredAdvapi32 INSTANCE = Native.load("Advapi32", CredAdvapi32.class, W32APIOptions.UNICODE_OPTIONS);

        boolean CredRead(String targetName, int type, int reservedFlag, PointerByReference credentialPtr);

        boolean CredWrite(CREDENTIAL credential, int flags);

        boolean CredDelete(String targetName, int type, int flags);

        void CredFree(Pointer credential);
    }

    /**
     * Windows FILETIME 结构体本地映射（按值嵌入 CREDENTIAL）。
     */
    @Structure.FieldOrder({"dwLowDateTime", "dwHighDateTime"})
    public static class FILETIME extends Structure {
        public int dwLowDateTime;
        public int dwHighDateTime;

        public FILETIME() {
            super(ALIGN_MSVC);
        }
    }

    /**
     * Windows CREDENTIAL 结构体本地映射。
     */
    @Structure.FieldOrder({
            "Flags", "Type", "TargetName", "Comment", "LastWritten",
            "CredentialBlobSize", "CredentialBlob", "Persist",
            "AttributeCount", "Attributes", "TargetAlias", "UserName"
    })
    public static class CREDENTIAL extends Structure {
        public int Flags;
        public int Type;
        public String TargetName;
        public String Comment;
        public FILETIME LastWritten;
        public int CredentialBlobSize;
        public Pointer CredentialBlob;
        public int Persist;
        public int AttributeCount;
        public Pointer Attributes;
        public String TargetAlias;
        public String UserName;

        public CREDENTIAL() {
            super(ALIGN_MSVC, W32APITypeMapper.UNICODE);
        }

        public CREDENTIAL(Pointer memory) {
            super(memory, ALIGN_MSVC, W32APITypeMapper.UNICODE);
            read();
        }
    }

    /**
     * 从 Windows 凭据管理器读取密码。
     *
     * @param targetName 凭据目标名称（如 AuxiliaryDBPassword）
     * @return 密码明文
     * @throws RuntimeException 凭据不存在或读取失败
     */
    public static String readPassword(String targetName) {
        if (!isWindows()) {
            throw new RuntimeException("Windows 凭据管理器仅在 Windows 系统可用");
        }
        PointerByReference pref = new PointerByReference();
        if (!CredAdvapi32.INSTANCE.CredRead(targetName, CRED_TYPE_GENERIC, 0, pref)) {
            int errorCode = Native.getLastError();
            throw new RuntimeException("读取 Windows 凭据失败: " + targetName + ", 错误码: " + errorCode);
        }
        try {
            CREDENTIAL cred = new CREDENTIAL(pref.getValue());
            byte[] blob = cred.CredentialBlob.getByteArray(0, cred.CredentialBlobSize);
            return new String(blob, StandardCharsets.UTF_16LE);
        } finally {
            CredAdvapi32.INSTANCE.CredFree(pref.getValue());
        }
    }

    /**
     * 将密码写入 Windows 凭据管理器。
     *
     * @param targetName 凭据目标名称
     * @param username   用户名
     * @param password   密码明文
     * @throws RuntimeException 写入失败
     */
    public static void writePassword(String targetName, String username, String password) {
        if (!isWindows()) {
            throw new RuntimeException("Windows 凭据管理器仅在 Windows 系统可用");
        }
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_16LE);
        Memory blob = new Memory(passwordBytes.length);
        blob.write(0, passwordBytes, 0, passwordBytes.length);

        CREDENTIAL cred = new CREDENTIAL();
        cred.Flags = 0;
        cred.Type = CRED_TYPE_GENERIC;
        cred.TargetName = targetName;
        cred.UserName = username;
        cred.CredentialBlobSize = passwordBytes.length;
        cred.CredentialBlob = blob;
        cred.Persist = CRED_PERSIST_LOCAL_MACHINE;
        cred.write();

        if (!CredAdvapi32.INSTANCE.CredWrite(cred, 0)) {
            int errorCode = Native.getLastError();
            throw new RuntimeException("写入 Windows 凭据失败: " + targetName + ", 错误码: " + errorCode);
        }
    }

    /**
     * 删除指定的 Windows 凭据。
     *
     * @param targetName 凭据目标名称
     * @throws RuntimeException 删除失败
     */
    public static void deletePassword(String targetName) {
        if (!isWindows()) {
            throw new RuntimeException("Windows 凭据管理器仅在 Windows 系统可用");
        }
        if (!CredAdvapi32.INSTANCE.CredDelete(targetName, CRED_TYPE_GENERIC, 0)) {
            int errorCode = Native.getLastError();
            throw new RuntimeException("删除 Windows 凭据失败: " + targetName + ", 错误码: " + errorCode);
        }
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("win");
    }

    /**
     * 命令行工具：写入凭据。
     * <p>
     * 用法：{@code java com.system.WindowsCredentialUtils write <targetName> <username> <password>}
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        if (args.length < 4 || !"write".equals(args[0])) {
            System.out.println("用法: java com.system.WindowsCredentialUtils write <targetName> <username> <password>");
            System.out.println("示例: java com.system.WindowsCredentialUtils write AuxiliaryDBPassword root 123456");
            return;
        }
        String targetName = args[1];
        String username = args[2];
        String password = args[3];
        writePassword(targetName, username, password);
        System.out.println("凭据已写入 Windows 凭据管理器: " + targetName);
    }
}
