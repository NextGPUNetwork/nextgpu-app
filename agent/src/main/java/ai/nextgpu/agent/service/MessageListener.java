package ai.nextgpu.agent.service;

import ai.nextgpu.common.dto.WebSocketMessageDto;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

public interface MessageListener {

    void onMessageReceived(WebSocketMessageDto message);


    PublicKey getMyPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException;

    byte[] get32BytesAesKey() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, InvalidAlgorithmParameterException;

    byte[] getAesEncapsulatedKey() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, InvalidAlgorithmParameterException;

    byte[] encryptMessage(byte[] plaintext, byte[] aesKey) throws GeneralSecurityException;

    // TODO add methods such as setMyComputerID and getMyComputerID to identify the connected computer
    // TODO add methods such as getMyUsername to access the username of the connected user

    void onActiveUsersUpdated(ArrayList<String> users);
}
