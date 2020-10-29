package com.sokcs.utils;

import com.sokcs.certificate.CertificateName;
import com.sokcs.certificate.GenerateCertificateException;
import lombok.extern.slf4j.Slf4j;
import com.sokcs.certificate.Certificate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
public final class CertificateUtils {
    private static KeyFactory keyFactory;

    static {
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 生成默认的根证书
     *
     * @param certificate
     * @return
     * @throws GenerateCertificateException
     */
    public static byte[] generateDefaultRoot(Certificate certificate) throws GenerateCertificateException {
        try {
            KeyPair keyPair = certificate.generateKeyPair();

            // 20年
            X509Certificate x509Certificate = certificate.generateRoot("C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=Proxy",
                    new Date(),
                    new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365 * 20)),
                    keyPair);

            byte[] bytes = x509Certificate.getEncoded();

            writeRootCertificate(bytes, Paths.get(CertificateName.RootCertificateName));
            writePrivateKey(keyPair.getPrivate(), Paths.get(CertificateName.RootCertificatePrivateKeyName));

            return bytes;
        } catch (NoSuchAlgorithmException | NoSuchProviderException | CertificateEncodingException e) {
            throw new GenerateCertificateException("生成默认的根证书失败", e);
        }
    }

    /**
     * 获取证书中的subject
     *
     * @param certificate
     * @return
     */
    public static String subject(X509Certificate certificate) {
        String issuer = certificate.getIssuerDN().toString();

        List<String> issuerSplit = Stream.of(issuer.split(",")).map(String::trim).collect(Collectors.toList());
        // 反转
        Collections.reverse(issuerSplit);

        return String.join(", ", issuerSplit);
    }

    /**
     * 写入根证书
     *
     * @param certificate
     * @param path
     */
    private static void writeRootCertificate(X509Certificate certificate, Path path) {
        try {
            writeRootCertificate(certificate.getEncoded(), path);
        } catch (CertificateEncodingException e) {
            log.error("写入根证书失败", e);
        }
    }

    /**
     * 写入根证书
     *
     * @param bytes
     * @param path
     */
    private static void writeRootCertificate(byte[] bytes, Path path) {
        try {
            Files.deleteIfExists(path);
            Files.write(path, bytes);
        } catch (IOException e) {
            log.error("写入根证书失败", e);
        }
    }

    /**
     * 读取根证书
     *
     * @param path
     * @return
     */
    public static X509Certificate readRootCertificate(Path path) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            return (X509Certificate) cf.generateCertificate(Files.newInputStream(path));
        } catch (CertificateException | IOException e) {
            log.error("读取证书失败", e);
        }

        return null;
    }

    /**
     * 写入私钥
     *
     * @param privateKey
     * @param path
     */
    public static void writePrivateKey(PrivateKey privateKey, Path path) {
        try {
            Files.deleteIfExists(path);
            Files.write(path, new PKCS8EncodedKeySpec(privateKey.getEncoded()).getEncoded());
        } catch (IOException e) {
            log.error("写入密钥失败", e);
        }
    }

    /**
     * 读取私钥
     *
     * @param path
     * @return
     */
    public static PrivateKey readPrivateKey(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(bytes);

            return keyFactory.generatePrivate(privateKeySpec);
        } catch (IOException | InvalidKeySpecException e) {
            log.error("加载私钥失败", e);
        }

        return null;
    }
}
