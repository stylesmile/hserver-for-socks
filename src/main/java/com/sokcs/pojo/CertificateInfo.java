package com.sokcs.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

@Data
@AllArgsConstructor
public class CertificateInfo {
    private KeyPair keyPair;
    private X509Certificate x509Certificate;
}
