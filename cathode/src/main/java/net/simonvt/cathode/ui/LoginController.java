/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.ViewGroup;
import net.simonvt.cathode.R;
import net.simonvt.cathode.ui.fragment.LoginFragment;

public class LoginController extends UiController {

  private LoginFragment loginFragment;

  public static LoginController newInstance(HomeActivity activity, ViewGroup parent) {
    return newInstance(activity, parent, null);
  }

  public static LoginController newInstance(HomeActivity activity, ViewGroup parent,
      Bundle inState) {
    return new LoginController(activity, parent, inState);
  }

  public LoginController(HomeActivity activity, ViewGroup parent, Bundle inState) {
    super(activity, inState);
    loginFragment =
        (LoginFragment) activity.getSupportFragmentManager().findFragmentByTag(FRAGMENT_LOGIN);

    if (loginFragment == null) {
      loginFragment = new LoginFragment();
    }

    activity.getActionBar().setTitle(R.string.app_name);
    activity.getActionBar().setDisplayHomeAsUpEnabled(false);
    activity.getActionBar().setHomeButtonEnabled(false);

    FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
    transaction.setCustomAnimations(R.anim.fade_in_front, R.anim.fade_out_back);

    if (loginFragment.isDetached()) {
      transaction.attach(loginFragment);
    } else if (!loginFragment.isAdded()) {
      transaction.add(parent.getId(), loginFragment, FRAGMENT_LOGIN);
    }

    transaction.commit();
  }

  @Override public void destroy(boolean completely) {
    if (completely) {
      FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
      transaction.setCustomAnimations(R.anim.fade_in_front, R.anim.fade_out_back);
      transaction.remove(loginFragment);
      transaction.commit();
    }
    super.destroy(completely);
  }
}
