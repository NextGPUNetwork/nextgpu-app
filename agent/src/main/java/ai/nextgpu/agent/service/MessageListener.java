package ai.nextgpu.agent.service;

import ai.nextgpu.common.dto.WebSocketMessageDto;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

/**
 * Callback contract for handling inbound WebSocket traffic and providing the session's
 * Kyber/AES key material needed to decrypt and encrypt messages exchanged over the connection.
 */
public interface MessageListener {

    /**
     * Called when a message is received from the server, for dispatch based on its
     * {@code messageCode} (see {@code WebSocketCodes}). If the payload is an encrypted envelope,
     * implementations are expected to decapsulate/decrypt it before handling.
     *
     * @param message the received WebSocket message
     */
    void onMessageReceived(WebSocketMessageDto message);


    /**
     * Returns the public key of this session's Kyber key pair, to be published to peers so
     * they can encapsulate a shared secret for this session.
     *
     * @return the session's public key
     * @throws NoSuchAlgorithmException if the key algorithm is not available
     * @throws InvalidKeySpecException if the stored key material is invalid
     * @throws NoSuchProviderException if the required security provider is not available
     */
    PublicKey getMyPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException;

    /**
     * Derives a 32-byte AES key via Kyber key encapsulation against the peer's public key.
     * The matching encapsulated key must be sent alongside the encrypted message so the peer
     * can decapsulate and derive the same secret; see {@link #getAesEncapsulatedKey()}.
     *
     * @return 32-byte AES key material
     * @throws NoSuchAlgorithmException if the key algorithm is not available
     * @throws InvalidKeySpecException if the peer's public key is invalid
     * @throws NoSuchProviderException if the required security provider is not available
     * @throws InvalidAlgorithmParameterException if the encapsulation parameters are invalid
     */
    byte[] get32BytesAesKey() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, InvalidAlgorithmParameterException;

    /**
     * Returns the encapsulated key bytes produced by the most recent Kyber encapsulation
     * (see {@link #get32BytesAesKey()}), to be sent to the peer so it can decapsulate the
     * shared AES secret.
     *
     * @return the encapsulated key bytes
     * @throws NoSuchAlgorithmException if the key algorithm is not available
     * @throws InvalidKeySpecException if the peer's public key is invalid
     * @throws NoSuchProviderException if the required security provider is not available
     * @throws InvalidAlgorithmParameterException if the encapsulation parameters are invalid
     */
    byte[] getAesEncapsulatedKey() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, InvalidAlgorithmParameterException;

    /**
     * Encrypts a plaintext payload with the given AES key before sending it over the WebSocket.
     *
     * @param plaintext the bytes to encrypt
     * @param aesKey the AES key to encrypt with (see {@link #get32BytesAesKey()})
     * @return the encrypted bytes
     * @throws GeneralSecurityException if encryption fails
     */
    byte[] encryptMessage(byte[] plaintext, byte[] aesKey) throws GeneralSecurityException;

    // TODO add methods such as setMyComputerID and getMyComputerID to identify the connected computer
    // TODO add methods such as getMyUsername to access the username of the connected user

    /**
     * Called when the server broadcasts an updated list of currently active/online users.
     *
     * @param users the linked entity IDs (wallet address or computer UUID) of active users
     */
    void onActiveUsersUpdated(ArrayList<String> users);
}
