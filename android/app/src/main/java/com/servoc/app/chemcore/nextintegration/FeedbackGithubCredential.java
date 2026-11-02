package com.servoc.app.chemcore.nextintegration;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Optional per-device, user-entered GitHub Fine-grained PAT encrypted using
 * AndroidKeyStore AES/GCM. No token or hard-coded secret exists in code, APK,
 * GitHub commits or saved feedback JSON. No Android backup of token is assumed.
 */
public final class FeedbackGithubCredential {
  private FeedbackGithubCredential(){}
  private static final String ALIAS="HazRobTank_Feedback_GitHub_PAT_V8";
  private static final String PREFS="hazrob_feedback_auth";
  private static final String IV="iv_b64",DATA="encrypted_token_b64";
  private static final String TRANSFORM="AES/GCM/NoPadding";
  private static SecretKey key() throws Exception {
    KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
    if(store.containsAlias(ALIAS))return ((KeyStore.SecretKeyEntry)
      store.getEntry(ALIAS,null)).getSecretKey();
    KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
    gen.init(new KeyGenParameterSpec.Builder(ALIAS,
        KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(256).build());
    return gen.generateKey();
  }
  public static void store(Context context,String supplied) throws Exception {
    if(context==null||supplied==null)throw new IllegalArgumentException("credential required");
    String token=supplied.trim();
    // The user's arbitrary short string is NOT a GitHub write credential.
    if(!(token.startsWith("github_pat_")||token.startsWith("ghp_"))||
       token.length()<30||token.contains("\n")||token.contains(" "))
      throw new IllegalArgumentException("GitHub fine-grained/classic write token required");
    Cipher cipher=Cipher.getInstance(TRANSFORM);
    cipher.init(Cipher.ENCRYPT_MODE,key());
    byte[] encrypted=cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
    boolean ok=context.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE)
      .edit().putString(IV,Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP))
      .putString(DATA,Base64.encodeToString(encrypted,Base64.NO_WRAP)).commit();
    if(!ok)throw new IllegalStateException("Secure credential storage commit failed");
  }
  public static boolean isConfigured(Context context){
    SharedPreferences p=context.getApplicationContext()
      .getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    return p.contains(IV)&&p.contains(DATA);
  }
  static String forAuthenticatedRequest(Context context) throws Exception {
    SharedPreferences p=context.getApplicationContext()
      .getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    String iv=p.getString(IV,""),data=p.getString(DATA,"");
    if(iv.isEmpty()||data.isEmpty())return null;
    Cipher cipher=Cipher.getInstance(TRANSFORM);
    cipher.init(Cipher.DECRYPT_MODE,key(),
      new GCMParameterSpec(128,Base64.decode(iv,Base64.DEFAULT)));
    return new String(cipher.doFinal(Base64.decode(data,Base64.DEFAULT)),
      StandardCharsets.UTF_8);
  }
  public static void clear(Context context) {
    context.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE)
      .edit().remove(IV).remove(DATA).commit();
    // Do not delete keystore key: ciphertext is erased and old tokens cannot
    // be restored through the app.
  }
}
