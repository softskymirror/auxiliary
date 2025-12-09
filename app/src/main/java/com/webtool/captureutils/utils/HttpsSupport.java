package com.captureutils.utils;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * https????????
 * @author puhaiyang
 * created on 2019/10/25 22:27
 */
public class HttpsSupport {
    /**
     * ???
     */
    private SslContext clientSslCtx;
    /**
     * ????????
     */
    private String issuer;
    /**
     * ???????
     */
    private Date caNotBefore;
    /**
     * ?????????
     */
    private Date caNotAfter;
    /**
     * ca??
     */
    private PrivateKey caPriKey;
    /**
     * ???????
     */
    private PrivateKey serverPriKey;
    /**
     * ???????
     */
    private PublicKey serverPubKey;

    /**
     * ???cahce
     */
    private Map<String, X509Certificate> certCache = new HashMap<>();
    /**
     *
     */
    private KeyFactory keyFactory = null;

    private HttpsSupport() {
        initHttpsConfig();
    }

    private static HttpsSupport httpsSupport;

    public static HttpsSupport getInstance() {
        if (httpsSupport == null) {
            httpsSupport = new HttpsSupport();
        }
        return httpsSupport;
    }

    private void initHttpsConfig() {
        try {
            keyFactory = KeyFactory.getInstance("RSA");

            setClientSslCtx(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build());
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            X509Certificate caCert = loadCert(classLoader.getResourceAsStream("ca.crt"));
            //???????????ca??
            PrivateKey caPriKey = loadPriKey(classLoader.getResourceAsStream("ca_private.der"));
            setCaPriKey(caPriKey);
            //??????????????????
            setIssuer(getSubjectByCert(caCert));
            //????ca?????????
            setCaNotBefore(caCert.getNotBefore());
            setCaNotAfter(caCert.getNotAfter());
            //?????????????????????SSL???L?????
            KeyPair keyPair = genKeyPair();
            //server????
            setServerPriKey(keyPair.getPrivate());
            //server????
            setServerPubKey(keyPair.getPublic());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ????RSA????????,?????2048
     */
    private KeyPair genKeyPair() throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        KeyPairGenerator caKeyPairGen = KeyPairGenerator.getInstance("RSA", "BC");
        caKeyPairGen.initialize(2048, new SecureRandom());
        return caKeyPairGen.genKeyPair();
    }

    /**
     * ??????????subject???
     */
    private String getSubjectByCert(X509Certificate certificate) {
        //??????????????????????
        List<String> tempList = Arrays.asList(certificate.getIssuerDN().toString().split(", "));
        return IntStream.rangeClosed(0, tempList.size() - 1)
                .mapToObj(i -> tempList.get(tempList.size() - i - 1)).collect(Collectors.joining(", "));
    }

    /**
     * ????ca????
     * @param inputStream ca???????
     */
    private PrivateKey loadPriKey(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] bts = new byte[1024];
        int len;
        while ((len = inputStream.read(bts)) != -1) {
            outputStream.write(bts, 0, len);
        }
        inputStream.close();
        outputStream.close();
        return loadPriKey(outputStream.toByteArray());
    }

    /**
     * ?????????RSA?? openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca.key -out
     * ca_private.der
     */
    private PrivateKey loadPriKey(byte[] bts)
            throws Exception {
        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(bts);
        return keyFactory.generatePrivate(privateKeySpec);
    }

    /**
     * ????ca?????
     *
     * @param inputStream ????????
     */
    private X509Certificate loadCert(InputStream inputStream) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(inputStream);
    }

    public SslContext getClientSslCtx() {
        return clientSslCtx;
    }

    public void setClientSslCtx(SslContext clientSslCtx) {
        this.clientSslCtx = clientSslCtx;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Date getCaNotBefore() {
        return caNotBefore;
    }

    public void setCaNotBefore(Date caNotBefore) {
        this.caNotBefore = caNotBefore;
    }

    public Date getCaNotAfter() {
        return caNotAfter;
    }

    public void setCaNotAfter(Date caNotAfter) {
        this.caNotAfter = caNotAfter;
    }

    public PrivateKey getCaPriKey() {
        return caPriKey;
    }

    public void setCaPriKey(PrivateKey caPriKey) {
        this.caPriKey = caPriKey;
    }

    public PrivateKey getServerPriKey() {
        return serverPriKey;
    }

    public void setServerPriKey(PrivateKey serverPriKey) {
        this.serverPriKey = serverPriKey;
    }

    public PublicKey getServerPubKey() {
        return serverPubKey;
    }

    public void setServerPubKey(PublicKey serverPubKey) {
        this.serverPubKey = serverPubKey;
    }


    /**
     * ??????
     *
     * @param host host
     * @return host????????
     */
    public X509Certificate getCert(String host) throws Exception {
        if (StringUtils.isBlank(host)) {
            return null;
        }
        X509Certificate cacheCert = certCache.get(host);
        if (cacheCert != null) {
            //?????????????
            return cacheCert;
        }
        //??????????????????????????
        host = host.trim().toLowerCase();
        String hostLowerCase = host.trim().toLowerCase();
        X509Certificate cert = genCert(getIssuer(), getCaPriKey(), getCaNotBefore(), getCaNotAfter(), getServerPubKey(), hostLowerCase);
        //????????
        certCache.put(host, cert);
        return certCache.get(host);
    }

    /**
     * ???????????????,??????CA???
     *
     * @param issuer ??????
     */
    /**
     * @param issuer        ??????
     * @param caPriKey      ca??
     * @param certStartTime ???????
     * @param certEndTime   ?????????
     * @param serverPubKey  server??????
     * @param hosts         host?????????????host
     * @return ???
     * @throws Exception Exception
     */
    public static X509Certificate genCert(String issuer, PrivateKey caPriKey, Date certStartTime,
                                          Date certEndTime, PublicKey serverPubKey,
                                          String... hosts) throws Exception {
        String subject = "C=CN, ST=SC, L=CD, O=hai, OU=study, CN=" + hosts[0];
        JcaX509v3CertificateBuilder jv3Builder = new JcaX509v3CertificateBuilder(new X500Name(issuer),
                 BigInteger.valueOf(System.currentTimeMillis() + (long) (Math.random() * 10000) + 1000),
                certStartTime,
                certEndTime,
                new X500Name(subject),
                serverPubKey);
        //SAN???????????????????????????????????
        GeneralName[] generalNames = new GeneralName[hosts.length];
        for (int i = 0; i < hosts.length; i++) {
            generalNames[i] = new GeneralName(GeneralName.dNSName, hosts[i]);
        }
        GeneralNames subjectAltName = new GeneralNames(generalNames);
        //???????????
        jv3Builder.addExtension(Extension.subjectAlternativeName, false, subjectAltName);
        //SHA256 ??SHA1????????????????????
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(caPriKey);
        return new JcaX509CertificateConverter().getCertificate(jv3Builder.build(signer));
    }
}
