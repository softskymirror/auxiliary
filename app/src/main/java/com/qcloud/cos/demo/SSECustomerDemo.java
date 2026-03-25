package com.qcloud.cos.demo;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.model.GetObjectMetadataRequest;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.SSECustomerKey;
import com.qcloud.cos.region.Region;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class SSECustomerDemo {
    private static COSClient cosClient = createCOSClient();

    private static String bucketName = "examplebucket-1250000000";

    private static String key = "aaa/bbb.txt";

    private static String base64EncodedKey = "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=";

    public static void main(String[] args) {
        try {
            SSECustomerUpload();
            SSECustomerDownload();
            SSECustomerHead();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cosClient.shutdown();
        }
    }

    private static COSClient createCOSClient() {
        // 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("COS_SECRETID", "COS_SECRETKEY");
        // 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-guangzhou"));
        // 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);

        return cosclient;
    }

    private static void SSECustomerUpload() {
        File localFile = new File("test.txt");
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, localFile);
        SSECustomerKey sseCustomerKey = new SSECustomerKey(base64EncodedKey);
        putObjectRequest.setSSECustomerKey(sseCustomerKey);

        try {
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
            // putobjectResult会返回文件的etag, 该md5值根据s3语义不是对象的md5，只是唯一性标志
            String etag = putObjectResult.getETag();
            System.out.println("finish upload, etag:" + etag);
        } catch (CosServiceException e) {
            e.printStackTrace();
        } catch (CosClientException e) {
            e.printStackTrace();
        }
    }
    private static void SSECustomerDownload() throws IOException {
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
        SSECustomerKey sseCustomerKey = new SSECustomerKey(base64EncodedKey);
        getObjectRequest.setSSECustomerKey(sseCustomerKey);
        COSObject cosObject = null;
        try {
            cosObject = cosClient.getObject(getObjectRequest);
            COSObjectInputStream cosObjectInputStream = cosObject.getObjectContent();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(cosObjectInputStream));
            System.out.println(bufferedReader.readLine());
        } catch (CosServiceException e) {
            e.printStackTrace();
        } catch (CosClientException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (cosObject != null) {
                cosObject.close();
            }
        }
    }

    private static void SSECustomerHead() {
        try {
            GetObjectMetadataRequest getObjectMetadataRequest = new GetObjectMetadataRequest(bucketName, key);
            SSECustomerKey sseCustomerKey = new SSECustomerKey(base64EncodedKey);
            getObjectMetadataRequest.setSSECustomerKey(sseCustomerKey);
            ObjectMetadata objectMetadata = cosClient.getObjectMetadata(getObjectMetadataRequest);
            System.out.println(objectMetadata);
        } catch (CosServiceException e) {
            e.printStackTrace();
        } catch (CosClientException e) {
            e.printStackTrace();
        }
    }
}

