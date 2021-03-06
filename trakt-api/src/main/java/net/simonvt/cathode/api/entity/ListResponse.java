/*
 * Copyright (C) 2014 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.simonvt.cathode.api.entity;

import net.simonvt.cathode.api.enumeration.ListPrivacy;

public class ListResponse extends Response {

  private String name;

  private String slug;

  private ListPrivacy privacy;

  private Boolean showNumbers;

  private Boolean allowShouts;

  public String getName() {
    return name;
  }

  public String getSlug() {
    return slug;
  }

  public ListPrivacy getPrivacy() {
    return privacy;
  }

  public Boolean getShowNumbers() {
    return showNumbers;
  }

  public Boolean getAllowShouts() {
    return allowShouts;
  }
}
