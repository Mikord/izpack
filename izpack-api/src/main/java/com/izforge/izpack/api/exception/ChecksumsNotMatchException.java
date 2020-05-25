package com.izforge.izpack.api.exception;

/**
 * An exception is thrown when the checksums do not match
 *
 * @author Fanzil Fattakhov
 */
public class ChecksumsNotMatchException extends ResourceException {
  public ChecksumsNotMatchException(String message) {
    super(message);
  }
}
