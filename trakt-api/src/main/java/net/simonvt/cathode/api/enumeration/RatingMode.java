package net.simonvt.cathode.api.enumeration;

import java.util.HashMap;
import java.util.Map;

public enum RatingMode {
  SIMPLE("simple"),
  ADVANCED("advanced");

  private String mode;

  RatingMode(String mode) {
    this.mode = mode;
  }

  private static final Map<String, RatingMode> STRING_MAPPING = new HashMap<String, RatingMode>();

  static {
    for (RatingMode via : RatingMode.values()) {
      STRING_MAPPING.put(via.toString().toUpperCase(), via);
    }
  }

  public static RatingMode fromValue(String value) {
    return STRING_MAPPING.get(value.toUpperCase());
  }

  @Override public String toString() {
    return mode;
  }
}
