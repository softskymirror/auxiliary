package com.qcloud.cos;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.AnonymousCOSCredentials;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.exception.MultiObjectDeleteException;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.DeleteObjectsRequest.KeyVersion;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.*;

/**
 * 腾讯云COS对象存储工具类
 * <p>封装了COS SDK的常用操作，包括存储桶管理、对象上传下载、复制删除、
 * 预签名URL生成、高级传输管理等功能</p>
 * <p>使用方式：通过构造方法传入secretId、secretKey和region初始化，
 * 然后调用各业务方法即可</p>
 *
 * @author auxiliary
 */
public class COSUtils {

    private final String secretId;
    private final String secretKey;
    private final String region;
    private COSClient cosClient;

    /**
     * 初始化COS工具类
     *
     * @param secretId  腾讯云SecretId
     * @param secretKey 腾讯云SecretKey
     * @param region    存储桶所在地域（如 ap-guangzhou、ap-beijing）
     */
    public COSUtils(String secretId, String secretKey, String region) {
        this.secretId = secretId;
        this.secretKey = secretKey;
        this.region = region;
    }

    /**
     * 创建并获取COS客户端实例
     *
     * @return COSClient 客户端实例
     */
    public COSClient createClient() {
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        cosClient = new COSClient(cred, clientConfig);
        return cosClient;
    }

    /**
     * 创建使用HTTPS协议的COS客户端实例
     *
     * @return COSClient 客户端实例
     */
    public COSClient createHttpsClient() {
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        clientConfig.setHttpProtocol(HttpProtocol.https);
        cosClient = new COSClient(cred, clientConfig);
        return cosClient;
    }

    /**
     * 创建带代理配置的COS客户端实例
     *
     * @param proxyIp   代理服务器IP
     * @param proxyPort 代理服务器端口
     * @param username  代理认证用户名（可为null）
     * @param password  代理认证密码（可为null）
     * @return COSClient 客户端实例
     */
    public COSClient createProxyClient(String proxyIp, int proxyPort, String username, String password) {
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        clientConfig.setHttpProxyIp(proxyIp);
        clientConfig.setHttpProxyPort(proxyPort);
        if (username != null && password != null) {
            clientConfig.setProxyUsername(username);
            clientConfig.setProxyPassword(password);
            clientConfig.setUseBasicAuth(true);
        }
        cosClient = new COSClient(cred, clientConfig);
        return cosClient;
    }

    /**
     * 关闭COS客户端，释放资源
     */
    public void shutdown() {
        if (cosClient != null) {
            cosClient.shutdown();
            cosClient = null;
        }
    }

    // ==================== 存储桶(Bucket)操作 ====================

    /**
     * 创建存储桶
     *
     * @param bucketName 存储桶名称（格式：BucketName-APPID）
     * @param acl        访问权限（如 CannedAccessControlList.PublicRead）
     * @return Bucket 创建成功的存储桶对象
     */
    public Bucket createBucket(String bucketName, CannedAccessControlList acl) {
        ensureClient();
        CreateBucketRequest request = new CreateBucketRequest(bucketName);
        if (acl != null) {
            request.setCannedAcl(acl);
        }
        return cosClient.createBucket(request);
    }

    /**
     * 删除存储桶（仅支持删除空桶）
     *
     * @param bucketName 存储桶名称
     */
    public void deleteBucket(String bucketName) {
        ensureClient();
        cosClient.deleteBucket(bucketName);
    }

    /**
     * 判断存储桶是否存在
     *
     * @param bucketName 存储桶名称
     * @return 存在返回true，否则返回false
     */
    public boolean doesBucketExist(String bucketName) {
        ensureClient();
        return cosClient.doesBucketExist(bucketName);
    }

    /**
     * 列出当前账号下所有存储桶
     *
     * @return 存储桶列表
     */
    public List<Bucket> listBuckets() {
        ensureClient();
        return cosClient.listBuckets();
    }

    /**
     * 设置存储桶ACL
     *
     * @param bucketName 存储桶名称
     * @param acl        预定义ACL（如 PublicRead、Private 等）
     */
    public void setBucketAcl(String bucketName, CannedAccessControlList acl) {
        ensureClient();
        cosClient.setBucketAcl(bucketName, acl);
    }

    /**
     * 获取存储桶ACL
     *
     * @param bucketName 存储桶名称
     * @return 访问控制列表
     */
    public AccessControlList getBucketAcl(String bucketName) {
        ensureClient();
        return cosClient.getBucketAcl(bucketName);
    }

    /**
     * 开启存储桶多版本控制
     *
     * @param bucketName 存储桶名称
     */
    public void enableBucketVersioning(String bucketName) {
        ensureClient();
        BucketVersioningConfiguration config =
                new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED);
        SetBucketVersioningConfigurationRequest request =
                new SetBucketVersioningConfigurationRequest(bucketName, config);
        cosClient.setBucketVersioningConfiguration(request);
    }

    /**
     * 暂停存储桶多版本控制
     *
     * @param bucketName 存储桶名称
     */
    public void suspendBucketVersioning(String bucketName) {
        ensureClient();
        BucketVersioningConfiguration config =
                new BucketVersioningConfiguration(BucketVersioningConfiguration.SUSPENDED);
        SetBucketVersioningConfigurationRequest request =
                new SetBucketVersioningConfigurationRequest(bucketName, config);
        cosClient.setBucketVersioningConfiguration(request);
    }

    /**
     * 获取存储桶多版本配置状态
     *
     * @param bucketName 存储桶名称
     * @return 版本控制配置（Enabled/Suspended）
     */
    public String getBucketVersioningStatus(String bucketName) {
        ensureClient();
        BucketVersioningConfiguration config = cosClient.getBucketVersioningConfiguration(bucketName);
        return config.getStatus();
    }

    // ==================== 对象(Object)上传操作 ====================

    /**
     * 上传本地文件到COS
     *
     * @param bucketName 存储桶名称
     * @param key        对象在COS中的路径（如 folder/file.txt）
     * @param localFile  本地文件
     * @return PutObjectResult 上传结果
     */
    public PutObjectResult putObject(String bucketName, String key, File localFile) {
        ensureClient();
        PutObjectRequest request = new PutObjectRequest(bucketName, key, localFile);
        return cosClient.putObject(request);
    }

    /**
     * 上传本地文件到COS（指定存储类型）
     *
     * @param bucketName  存储桶名称
     * @param key         对象路径
     * @param localFile   本地文件
     * @param storageClass 存储类型（Standard/Standard_IA/Archive）
     * @return PutObjectResult 上传结果
     */
    public PutObjectResult putObject(String bucketName, String key, File localFile, StorageClass storageClass) {
        ensureClient();
        PutObjectRequest request = new PutObjectRequest(bucketName, key, localFile);
        request.setStorageClass(storageClass);
        return cosClient.putObject(request);
    }

    /**
     * 通过输入流上传数据到COS
     * <p>注意：必须提供准确的流长度(contentLength)，否则可能导致内存OOM</p>
     *
     * @param bucketName    存储桶名称
     * @param key           对象路径
     * @param inputStream   输入流
     * @param contentLength 流的数据长度（字节）
     * @param contentType   内容类型（如 image/jpeg、text/plain），可为null
     * @return PutObjectResult 上传结果
     */
    public PutObjectResult putObject(String bucketName, String key, InputStream inputStream,
                                     long contentLength, String contentType) {
        ensureClient();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        if (contentType != null) {
            metadata.setContentType(contentType);
        }
        PutObjectRequest request = new PutObjectRequest(bucketName, key, inputStream, metadata);
        return cosClient.putObject(request);
    }

    /**
     * 追加上传（在对象末尾追加数据）
     *
     * @param bucketName 存储桶名称
     * @param key        对象路径
     * @param localFile  本地文件
     * @param position   追加起始位置（首次为0，后续使用上一次返回的nextPosition）
     * @return 下一次追加的起始位置
     */
    public long appendObject(String bucketName, String key, File localFile, long position) {
        ensureClient();
        AppendObjectRequest request = new AppendObjectRequest(bucketName, key, localFile);
        request.setPosition(position);
        AppendObjectResult result = cosClient.appendObject(request);
        return result.getNextAppendPosition();
    }

    // ==================== 对象(Object)下载操作 ====================

    /**
     * 下载COS对象到本地文件
     *
     * @param bucketName 存储桶名称
     * @param key        对象路径
     * @param localFile  本地保存路径
     * @return ObjectMetadata 对象元数据
     */
    public ObjectMetadata getObject(String bucketName, String key, File localFile) {
        ensureClient();
        GetObjectRequest request = new GetObjectRequest(bucketName, key);
        return cosClient.getObject(request, localFile);
    }

    /**
     * 下载COS对象并获取输入流
     * <p>注意：调用方需负责关闭返回的输入流</p>
     *
     * @param bucketName 存储桶名称
     * @param key        对象路径
     * @return COSObject 包含输入流和元数据的COS对象
     */
    public COSObject getObjectAsStream(String bucketName, String key) {
        ensureClient();
        GetObjectRequest request = new GetObjectRequest(bucketName, key);
        return cosClient.getObject(request);
    }

    /**
     * 获取对象的元数据信息
     *
     * @param bucketName 存储桶名称
     * @param key        对象路径
     * @return ObjectMetadata 对象元数据
     */
    public ObjectMetadata getObjectMetadata(String bucketName, String key) {
        ensureClient();
        return cosClient.getObjectMetadata(bucketName, key);
    }

    /**
     * 判断对象是否存在
     *
     * @param bucketName 存储桶名称
     * @param key        对象路径
     * @return 存在返回true，否则返回false
     */
    public boolean doesObjectExist(String bucketName, String key) {
        ensureClient();
        return cosClient.doesObjectExist(bucketName, key);
    }

    /**
     * 获取对象的访问URL（无需身份验证）
     *
     * @param bucketName 存储桶名称
     * @param key        对象路径
     * @param useHttps   是否使用HTTPS协议
     * @return 对象访问URL
     */
    public URL getObjectUrl(String bucketName, String key, boolean useHttps) {
        COSCredentials cred = new AnonymousCOSCredentials();
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        clientConfig.setHttpProtocol(useHttps ? HttpProtocol.https : HttpProtocol.http);
        COSClient anonymousClient = new COSClient(cred, clientConfig);
        try {
            return anonymousClient.getObjectUrl(bucketName, key);
        } finally {
            anonymousClient.shutdown();
        }
    }

    // ==================== 对象(Object)删除操作 ====================

    /**
     * 删除单个对象
     *
     * @param bucketName 存储桶名称
     * @param key        对象路径
     */
    public void deleteObject(String bucketName, String key) {
        ensureClient();
        cosClient.deleteObject(bucketName, key);
    }

    /**
     * 批量删除对象（单次最多1000个）
     *
     * @param bucketName 存储桶名称
     * @param keys       要删除的对象路径列表
     * @return 成功删除的对象列表
     */
    public List<DeleteObjectsResult.DeletedObject> deleteObjects(String bucketName, List<String> keys) {
        ensureClient();
        DeleteObjectsRequest request = new DeleteObjectsRequest(bucketName);
        List<KeyVersion> keyVersions = new java.util.ArrayList<>();
        for (String key : keys) {
            keyVersions.add(new KeyVersion(key));
        }
        request.setKeys(keyVersions);
        DeleteObjectsResult result = cosClient.deleteObjects(request);
        return result.getDeletedObjects();
    }

    // ==================== 对象(Object)复制操作 ====================

    /**
     * 复制对象（支持跨园区，最大支持5GB）
     *
     * @param srcRegion      源存储桶所在区域
     * @param srcBucketName  源存储桶名称
     * @param srcKey         源对象路径
     * @param destBucketName 目标存储桶名称
     * @param destKey        目标对象路径
     * @return CopyObjectResult 复制结果
     */
    public CopyObjectResult copyObject(String srcRegion, String srcBucketName, String srcKey,
                                       String destBucketName, String destKey) {
        ensureClient();
        CopyObjectRequest request = new CopyObjectRequest(
                new Region(srcRegion), srcBucketName, srcKey, destBucketName, destKey);
        return cosClient.copyObject(request);
    }

    // ==================== 对象(Object)其他操作 ====================

    /**
     * 列出存储桶中的对象
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象路径前缀过滤条件（为空则列出所有）
     * @param maxKeys    最大返回数量（最大1000）
     * @return 对象列表
     */
    public List<COSObjectSummary> listObjects(String bucketName, String prefix, int maxKeys) {
        ensureClient();
        ListObjectsRequest request = new ListObjectsRequest();
        request.setBucketName(bucketName);
        request.setPrefix(prefix);
        request.setMaxKeys(maxKeys);
        ObjectListing listing = cosClient.listObjects(request);
        return listing.getObjectSummaries();
    }

    /**
     * 设置对象ACL
     *
     * @param bucketName 存储桶名称
     * @param key        对象路径
     * @param acl        预定义ACL
     */
    public void setObjectAcl(String bucketName, String key, CannedAccessControlList acl) {
        ensureClient();
        cosClient.setObjectAcl(bucketName, key, acl);
    }

    /**
     * 获取对象ACL
     *
     * @param bucketName 存储桶名称
     * @param key        对象路径
     * @return 访问控制列表
     */
    public AccessControlList getObjectAcl(String bucketName, String key) {
        ensureClient();
        return cosClient.getObjectAcl(bucketName, key);
    }

    /**
     * 创建软链接
     *
     * @param bucketName 存储桶名称
     * @param symlink    软链接路径
     * @param target     目标对象路径
     * @return PutSymlinkResult 创建结果
     */
    public PutSymlinkResult createSymlink(String bucketName, String symlink, String target) {
        ensureClient();
        PutSymlinkRequest request = new PutSymlinkRequest(bucketName, symlink, target);
        return cosClient.putSymlink(request);
    }

    /**
     * 获取软链接指向的目标对象路径
     *
     * @param bucketName 存储桶名称
     * @param symlink    软链接路径
     * @return 目标对象路径
     */
    public String getSymlinkTarget(String bucketName, String symlink) {
        ensureClient();
        GetSymlinkResult result = cosClient.getSymlink(new GetSymlinkRequest(bucketName, symlink, null));
        return result.getTarget();
    }

    /**
     * 恢复归档类型对象
     *
     * @param bucketName 存储桶名称
     * @param key        对象路径
     * @param expireDays 恢复后临时副本的过期天数
     * @param tier       恢复模式（Standard/Expedited/Bulk）
     */
    public void restoreObject(String bucketName, String key, int expireDays, Tier tier) {
        ensureClient();
        RestoreObjectRequest request = new RestoreObjectRequest(bucketName, key, expireDays);
        CASJobParameters casParams = new CASJobParameters();
        casParams.setTier(tier);
        request.setCASJobParameters(casParams);
        cosClient.restoreObject(request);
    }

    // ==================== 预签名URL ====================

    /**
     * 生成预签名的下载URL
     *
     * @param bucketName       存储桶名称
     * @param key              对象路径
     * @param expirationMillis URL有效期（毫秒）
     * @return 预签名下载URL
     */
    public URL generatePresignedDownloadUrl(String bucketName, String key, long expirationMillis) {
        ensureClient();
        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(bucketName, key, HttpMethodName.GET);
        request.setExpiration(new Date(System.currentTimeMillis() + expirationMillis));
        return cosClient.generatePresignedUrl(request);
    }

    /**
     * 生成预签名的上传URL
     *
     * @param bucketName       存储桶名称
     * @param key              对象路径
     * @param expirationMillis URL有效期（毫秒）
     * @return 预签名上传URL
     */
    public URL generatePresignedUploadUrl(String bucketName, String key, long expirationMillis) {
        ensureClient();
        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(bucketName, key, HttpMethodName.PUT);
        request.setExpiration(new Date(System.currentTimeMillis() + expirationMillis));
        return cosClient.generatePresignedUrl(request);
    }

    // ==================== 高级传输管理器(TransferManager) ====================

    /**
     * 创建传输管理器（高级异步传输接口）
     *
     * @param threadPoolSize 线程池大小
     * @return TransferManager 传输管理器实例
     */
    public TransferManager createTransferManager(int threadPoolSize) {
        ensureClient();
        ExecutorService threadPool = Executors.newFixedThreadPool(threadPoolSize);
        return new TransferManager(cosClient, threadPool);
    }

    /**
     * 高级上传（自动根据文件大小选择简单上传或分块上传）
     *
     * @param transferManager 传输管理器
     * @param bucketName      存储桶名称
     * @param key             对象路径
     * @param localFile       本地文件
     * @return Upload 异步上传任务（可调用waitForUploadResult等待完成）
     */
    public Upload asyncUpload(TransferManager transferManager, String bucketName,
                              String key, File localFile) {
        PutObjectRequest request = new PutObjectRequest(bucketName, key, localFile);
        return transferManager.upload(request);
    }

    /**
     * 高级下载（支持断点续传）
     *
     * @param transferManager 传输管理器
     * @param bucketName      存储桶名称
     * @param key             对象路径
     * @param localFile       本地保存文件
     * @param resumable       是否启用断点续传
     * @return Download 异步下载任务
     */
    public Download asyncDownload(TransferManager transferManager, String bucketName,
                                  String key, File localFile, boolean resumable) {
        GetObjectRequest request = new GetObjectRequest(bucketName, key);
        return transferManager.download(request, localFile, resumable);
    }

    /**
     * 高级复制（自动根据文件大小选择简单复制或分块复制）
     *
     * @param transferManager 传输管理器
     * @param srcRegion       源区域
     * @param srcBucketName   源存储桶
     * @param srcKey          源对象路径
     * @param destBucketName  目标存储桶
     * @param destKey         目标对象路径
     * @return Copy 异步复制任务
     */
    public Copy asyncCopy(TransferManager transferManager, String srcRegion,
                          String srcBucketName, String srcKey,
                          String destBucketName, String destKey) {
        CopyObjectRequest request = new CopyObjectRequest(
                new Region(srcRegion), srcBucketName, srcKey, destBucketName, destKey);
        return transferManager.copy(request);
    }

    /**
     * 批量上传整个目录
     *
     * @param transferManager 传输管理器
     * @param bucketName      存储桶名称
     * @param cosPathPrefix   COS上的目标路径前缀
     * @param localDir        本地目录
     * @param recursive       是否递归上传子目录
     * @return MultipleFileUpload 异步批量上传任务
     */
    public MultipleFileUpload asyncUploadDirectory(TransferManager transferManager,
                                                    String bucketName, String cosPathPrefix,
                                                    File localDir, boolean recursive) {
        return transferManager.uploadDirectory(bucketName, cosPathPrefix, localDir, recursive);
    }

    /**
     * 批量下载整个目录
     *
     * @param transferManager 传输管理器
     * @param bucketName      存储桶名称
     * @param cosPathPrefix   COS上的对象路径前缀
     * @param localDir        本地保存目录
     * @return MultipleFileDownload 异步批量下载任务
     */
    public MultipleFileDownload asyncDownloadDirectory(TransferManager transferManager,
                                                        String bucketName, String cosPathPrefix,
                                                        File localDir) {
        return transferManager.downloadDirectory(bucketName, cosPathPrefix, localDir);
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 确保COS客户端已初始化，若未初始化则自动创建
     */
    private void ensureClient() {
        if (cosClient == null) {
            createClient();
        }
    }
}
