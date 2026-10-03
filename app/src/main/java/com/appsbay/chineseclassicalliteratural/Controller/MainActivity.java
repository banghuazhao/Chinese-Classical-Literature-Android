package com.appsbay.chineseclassicalliteratural.Controller;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.appsbay.chineseclassicalliteratural.Controller.Menu.MenuFragment;
import com.appsbay.chineseclassicalliteratural.R;
import com.appsbay.chineseclassicalliteratural.Tools.BookMotion;
import com.appsbay.chineseclassicalliteratural.Tools.AgeGate;
import com.appsbay.chineseclassicalliteratural.Tools.MyColor;
import com.appsbay.chineseclassicalliteratural.Tools.PrivacyManager;
import com.appsbay.chineseclassicalliteratural.Tools.RateItDialogFragment;
import com.appsbay.chineseclassicalliteratural.Tools.ScreenChrome;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG_HOME = "1";
    private static final String TAG_LIBRARY = "2";
    private static final String TAG_MORE = "3";

    private BooksFragment fragment1;
    private LibraryFragment fragment2;
    private MenuFragment fragment3;
    private FragmentManager fm;
    private Fragment active;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_main);
        MyColor.applyBottomNavigation(this, bottomNav);
        View adNavigationSpacer = findViewById(R.id.ad_navigation_spacer);
        adNavigationSpacer.setBackgroundColor(MyColor.getBottomBarColor(this));
        ScreenChrome.setupHome(
                this,
                toolbar,
                findViewById(R.id.main_root),
                bottomNav,
                findViewById(R.id.toolbar_hairline)
        );
        fm = getSupportFragmentManager();
        restoreOrCreateFragments(savedInstanceState);

        int selectedItem = R.id.nav_home;
        int titleRes = R.string.app_name;
        if (active == fragment2) {
            selectedItem = R.id.nav_library;
            titleRes = R.string.My_Library;
        } else if (active == fragment3) {
            selectedItem = R.id.nav_more;
            titleRes = R.string.Menu;
        }
        bottomNav.getMenu().findItem(selectedItem).setChecked(true);
        setDestinationTitle(titleRes);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        if (AgeGate.getGroup(this) == AgeGate.UNKNOWN) {
            AgeGate.showChoice(this, false, group -> {
                if (AgeGate.saveGroup(this, group)) {
                    // Rebuild the hidden More tab with the chosen age group's controls.
                    recreate();
                }
            });
        } else if (AgeGate.isAdult(this)) {
            startAdultPrivacy(savedInstanceState == null);
        }

        bottomNav.setOnNavigationItemReselectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                fragment1.scrollToTop();
            } else if (item.getItemId() == R.id.nav_library) {
                fragment2.scrollToTop();
            } else if (item.getItemId() == R.id.nav_more) {
                fragment3.scrollToTop();
            }
        });

        if (savedInstanceState == null) {
            BookMotion.revealOnce(findViewById(R.id.fragment_container));
        }
    }

    private void startAdultPrivacy(boolean firstLaunch) {
        PrivacyManager.get(this).start(this, () -> {
            if (firstLaunch && !isFinishing() && !isDestroyed()) {
                RateItDialogFragment.show(this, getSupportFragmentManager());
            }
        });
    }

    private void restoreOrCreateFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            fragment1 = new BooksFragment();
            fragment2 = new LibraryFragment();
            fragment3 = new MenuFragment();
            fm.beginTransaction()
                    .add(R.id.fragment_container, fragment2, TAG_LIBRARY)
                    .hide(fragment2)
                    .add(R.id.fragment_container, fragment3, TAG_MORE)
                    .hide(fragment3)
                    .add(R.id.fragment_container, fragment1, TAG_HOME)
                    .commitNow();
            active = fragment1;
            return;
        }

        Fragment restoredHome = fm.findFragmentByTag(TAG_HOME);
        Fragment restoredLibrary = fm.findFragmentByTag(TAG_LIBRARY);
        Fragment restoredMore = fm.findFragmentByTag(TAG_MORE);
        fragment1 = restoredHome instanceof BooksFragment
                ? (BooksFragment) restoredHome : null;
        fragment2 = restoredLibrary instanceof LibraryFragment
                ? (LibraryFragment) restoredLibrary : null;
        fragment3 = restoredMore instanceof MenuFragment
                ? (MenuFragment) restoredMore : null;

        boolean libraryWasActive = fragment2 != null && !fragment2.isHidden();
        boolean moreWasActive = fragment3 != null && !fragment3.isHidden();
        FragmentTransaction transaction = fm.beginTransaction();
        boolean changed = false;
        if (fragment1 == null) {
            fragment1 = new BooksFragment();
            transaction.add(R.id.fragment_container, fragment1, TAG_HOME);
            if (libraryWasActive || moreWasActive) {
                transaction.hide(fragment1);
            }
            changed = true;
        }
        if (fragment2 == null) {
            fragment2 = new LibraryFragment();
            transaction.add(R.id.fragment_container, fragment2, TAG_LIBRARY);
            if (!libraryWasActive) {
                transaction.hide(fragment2);
            }
            changed = true;
        }
        if (fragment3 == null) {
            fragment3 = new MenuFragment();
            transaction.add(R.id.fragment_container, fragment3, TAG_MORE);
            if (!moreWasActive) {
                transaction.hide(fragment3);
            }
            changed = true;
        }
        if (changed) {
            transaction.commitNow();
        }
        if (moreWasActive) {
            active = fragment3;
        } else if (libraryWasActive) {
            active = fragment2;
        } else {
            active = fragment1;
        }
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener =
            item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    fm.beginTransaction()
                            .setCustomAnimations(R.anim.shelf_view_enter, R.anim.shelf_view_exit)
                            .hide(active).show(fragment1).commit();
                    active = fragment1;
                    setDestinationTitle(R.string.app_name);
                    return true;
                } else if (id == R.id.nav_library) {
                    fm.beginTransaction()
                            .setCustomAnimations(R.anim.shelf_view_enter, R.anim.shelf_view_exit)
                            .hide(active).show(fragment2).commit();
                    active = fragment2;
                    setDestinationTitle(R.string.My_Library);
                    return true;
                } else if (id == R.id.nav_more) {
                    fm.beginTransaction()
                            .setCustomAnimations(R.anim.shelf_view_enter, R.anim.shelf_view_exit)
                            .hide(active).show(fragment3).commit();
                    active = fragment3;
                    setDestinationTitle(R.string.Menu);
                    return true;
                }
                return false;
            };

    private void setDestinationTitle(int titleRes) {
        setTitle(titleRes);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(titleRes);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(titleRes);
        }
    }
}
