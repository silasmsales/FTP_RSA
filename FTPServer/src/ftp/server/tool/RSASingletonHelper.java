/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ftp.server.tool;

/**
 *
 * @author silas
 */
import java.math.BigInteger;
import java.security.SecureRandom;

public class RSASingletonHelper {
    private static RSASingletonHelper uniqueInstance;
    private BigInteger n, d, e;
    private int bitlen = 2048;
    private BigInteger p;
    private BigInteger q;

    private RSASingletonHelper() {
        initializeKeys();
    }

    public static RSASingletonHelper getInstance() {
        if (uniqueInstance == null) {
            synchronized (RSASingletonHelper.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new RSASingletonHelper();
                }
            }
        }
        return uniqueInstance;
    }

    private void initializeKeys() {
        SecureRandom r = new SecureRandom();
        p = new BigInteger(bitlen / 2, 100, r);
        q = new BigInteger(bitlen / 2, 100, r);

        n = p.multiply(q);

        BigInteger m = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));

        e = new BigInteger("3");
        while (m.gcd(e).intValue() > 1) {
            e = e.add(new BigInteger("2"));
        }

        d = e.modInverse(m);
    }

    public String getPublicKeys() {
        return e.toString() + ";" + n.toString();
    }

    public String getPrivateKeys() {
        return d.toString() + ";" + n.toString();
    }

    public String encrypt(String content, String publicKey) {
        String[] keys = publicKey.trim().split(";");
        BigInteger e = new BigInteger(keys[0]);
        BigInteger n = new BigInteger(keys[1]);

        return new BigInteger(content.getBytes()).modPow(e, n).toString();
    }

    public String decrypt(String content, String privateKey) {
        String[] keys = privateKey.trim().split(";");
        BigInteger d = new BigInteger(keys[0]);
        BigInteger n = new BigInteger(keys[1]);

        return new String(new BigInteger(content).modPow(d, n).toByteArray());
    }
}
