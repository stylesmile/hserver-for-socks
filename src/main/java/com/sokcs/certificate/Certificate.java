package com.sokcs.certificate;

import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

public interface Certificate {
    /**
     * 生成密钥
     *
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException;

    /**
     * 生成根证书
     *
     * @param subject
     * @param notBefore
     * @param notAfter
     * @param keyPair
     * @return
     * @throws GenerateCertificateException
     */
    X509Certificate generateRoot(String subject, Date notBefore, Date notAfter, KeyPair keyPair) throws GenerateCertificateException;

    /**
     * 根据根证书动态签发证书
     *
     * @param issuer
     * @param rootPrivateKey
     * @param notBefore
     * @param notAfter
     * @param publicKey
     * @param hosts
     * @return
     * @throws GenerateCertificateException
     */
    X509Certificate generate(String issuer, PrivateKey rootPrivateKey, Date notBefore, Date notAfter, PublicKey publicKey, List<String> hosts) throws GenerateCertificateException;
}
