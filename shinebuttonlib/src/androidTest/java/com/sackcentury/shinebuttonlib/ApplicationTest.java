package com.sackcentury.shinebuttonlib;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ApplicationTest {
    @Test
    public void shineButtonUpdatesCheckedState() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ShineButton button = new ShineButton(context);

        button.setChecked(true, false);
        assertTrue(button.isChecked());

        button.setChecked(false, false);
        assertFalse(button.isChecked());
    }
}
