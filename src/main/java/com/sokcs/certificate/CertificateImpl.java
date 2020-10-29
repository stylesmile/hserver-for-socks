package com.sokcs.certificate;

import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
public class CertificateImpl implements Certificate {
    private final Provider provider = new BouncyCastleProvider();

    // 用SHA1浏览器可能会提示证书不安全
    private final String SHA256WithRSAEncryption = "SHA256WithRSAEncryption";
    private final String RSA = "RSA";

    public CertificateImpl() {
        Security.addProvider(provider);
    }

    @Override
    public KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator caKeyPairGen = KeyPairGenerator.getInstance(RSA, provider.getName());
        caKeyPairGen.initialize(2048, new SecureRandom());

        return caKeyPairGen.genKeyPair();
    }

    @Override
    public X509Certificate generateRoot(String subject, Date notBefore, Date notAfter, KeyPair keyPair) throws GenerateCertificateException {
        try {
            JcaX509v3CertificateBuilder jcaX509v3CertificateBuilder = new JcaX509v3CertificateBuilder(new X500Name(subject),
                    BigInteger.valueOf(System.currentTimeMillis()),
                    notBefore,
                    notAfter,
                    new X500Name(subject),
                    keyPair.getPublic());

            jcaX509v3CertificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));

            ContentSigner contentSigner = new JcaContentSignerBuilder(SHA256WithRSAEncryption).build(keyPair.getPrivate());

            return new JcaX509CertificateConverter().getCertificate(jcaX509v3CertificateBuilder.build(contentSigner));
        } catch (CertIOException | OperatorCreationException | CertificateException e) {
            throw new GenerateCertificateException("生成根证书失败", e);
        }
    }

    @Override
    public X509Certificate generate(String issuer, PrivateKey rootPrivateKey, Date notBefore, Date notAfter, PublicKey publicKey, List<String> hosts) throws GenerateCertificateException {
        try {
            String subject = fromIssuer(issuer, hosts.get(0));

            JcaX509v3CertificateBuilder jcaX509v3CertificateBuilder = new JcaX509v3CertificateBuilder(new X500Name(issuer),
                    BigInteger.valueOf(System.currentTimeMillis()),
                    notBefore,
                    notAfter,
                    new X500Name(subject),
                    publicKey);

            List<GeneralName> generalNames = hosts.stream()
                    .map(host -> new GeneralName(GeneralName.dNSName, host))
                    .collect(Collectors.toList());

            GeneralName[] generalNameArray = new GeneralName[generalNames.size()];

            jcaX509v3CertificateBuilder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(generalNames.toArray(generalNameArray)));

            ContentSigner contentSigner = new JcaContentSignerBuilder(SHA256WithRSAEncryption).build(rootPrivateKey);

            return new JcaX509CertificateConverter().getCertificate(jcaX509v3CertificateBuilder.build(contentSigner));
        } catch (CertIOException | CertificateException | OperatorCreationException e) {
            throw new GenerateCertificateException("生成证书失败", e);
        }
    }

    private String fromIssuer(String issuer, String host) {
        String cn = "CN";

        return Stream.of(issuer.split(","))
                .map(String::trim)
                .map(s -> {
                    String[] strings = s.split("=");
                    if (cn.equals(strings[0])) {
                        return cn + "=" + host;
                    }
                    return s;
                }).collect(Collectors.joining(", "));
    }
}
