package server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;

public class Computation implements Runnable {


    private DataOutputStream dos;
    private String key;

    private PublicKey publickey;
    private KeyAgreement keyAgreement;
    private byte[] sharedsecret;
    private String ALGO = "AES";


    int id;

    private void makeKeyExchangeParams() {
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(128);
            KeyPair kp = kpg.generateKeyPair();
            publickey = kp.getPublic();
            keyAgreement = KeyAgreement.getInstance("ECDH");
            keyAgreement.init(kp.getPrivate());

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    private void setReceiverPublicKey(PublicKey publickey) {
        try {
            keyAgreement.doPhase(publickey, true);
            sharedsecret = keyAgreement.generateSecret();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    private String encrypt(String msg) {
        try {
            Base64.Encoder encoder = Base64.getEncoder();
            Key key = generateKey();
            Cipher c = Cipher.getInstance(ALGO);
            c.init(Cipher.ENCRYPT_MODE, key);
            byte[] encVal = c.doFinal(msg.getBytes());
            String encoded_String = encoder.encodeToString(encVal);
            return encoded_String;
        } catch (BadPaddingException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return msg;
    }

    private String decrypt(String encryptedData) {
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            Key key = generateKey();
            Cipher c = Cipher.getInstance(ALGO);
            c.init(Cipher.DECRYPT_MODE, key);
            byte[] decordedValue = decoder.decode(encryptedData);
            byte[] decValue = c.doFinal(decordedValue);
            return new String(decValue);
        } catch (BadPaddingException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return encryptedData;
    }

    private PublicKey getPublickey() {
        return publickey;
    }

    private Key generateKey() {
        return new SecretKeySpec(sharedsecret, ALGO);
    }

    Computation(DataOutputStream dos, String key, int id) {
        this.dos = dos;
        this.key = key;
        this.id = id;
    }

    public void run() {
        try {
            makeKeyExchangeParams();
            //System.out.println("Enter the string to encodes: ");
            //plain = s.nextLine();*/
            PublicKey pub_key = getPublickey();
            setReceiverPublicKey(pub_key);
            String encrypted = encrypt(key);
            System.out.println(encrypted);
            for (long i = 0; i < 500000; i++)
                for (long j = 0; j < 10000; j++);
            //System.out.println("Encrypted Text: " + encrypted);
            //String decrypted = decrypt(key);
            //System.out.println("Decrypted Text: " + decrypted);
            dos.writeUTF(encrypted);
            dos.writeUTF(Integer.toString(id));
            dos.flush();
        } catch (IOException e ) {
            System.out.println("Server Error - Broken Pipeline.");
        }
    }
}
