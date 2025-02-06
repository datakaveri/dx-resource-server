package iudx.resource.server.encryptionnew.util;

import com.goterl.lazysodium.interfaces.MessageEncoder;
import java.util.Base64;

public class UrlBase64MessageEncoder implements MessageEncoder {
  @Override
  public String encode(byte[] bytes) {
    return Base64.getUrlEncoder().encodeToString(bytes);
  }

  @Override
  public byte[] decode(String s) {
    return Base64.getUrlDecoder().decode(s);
  }
}
