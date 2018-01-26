/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2016 Max Rumpf alias Maxr1998
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.Maxr1998.xposed.maxlock.hooks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.crossbowffs.remotepreferences.RemotePreferences;

import de.Maxr1998.xposed.maxlock.BuildConfig;
import de.Maxr1998.xposed.maxlock.Common;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.Maxr1998.xposed.maxlock.Common.PREFS_APPS;
import static de.Maxr1998.xposed.maxlock.Common.PREFS_HISTORY;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class Main implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    private static SharedPreferences getRemotePreferences(String name) {
        // #############################################################################
        // Thanks to XposedGELSettings for the following snippet (https://git.io/vP2Gw):
        Object activityThread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
        if (activityThread == null)
            return null;
        Context context = (Context) callMethod(activityThread, "getSystemContext");
        // #############################################################################
        return new RemotePreferences(context, Common.PREFERENCE_PROVIDER_AUTHORITY, name);
    }

    static void logD(String message) {
        if (BuildConfig.DEBUG)
            log("ML: " + message);
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        log("ML: Loaded Main class");
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lPParam) throws Throwable {
        switch (lPParam.packageName) {
            case SystemUI.PACKAGE_NAME:
                SharedPreferences prefs = getRemotePreferences(Common.MAXLOCK_PACKAGE_NAME.concat("_preferences"));
                SharedPreferences prefsApps = getRemotePreferences(PREFS_APPS);
                SystemUI.init(lPParam, prefs, prefsApps);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    SystemUI.initScreenOff(lPParam, prefsApps, getRemotePreferences(PREFS_HISTORY));
                }
                return;
            case SystemUI.PACKAGE_NAME_KEYGUARD:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    SystemUI.initScreenOff(lPParam, getRemotePreferences(PREFS_APPS), getRemotePreferences(PREFS_HISTORY));
                    return;
                }
                break;
            case MaxLock.PACKAGE_NAME:
                MaxLock.init(lPParam, getRemotePreferences(PREFS_HISTORY));
                break;
            case DeviceAdminProtection.PACKAGE_NAME:
                DeviceAdminProtection.init(lPParam);
                break;
        }
        SharedPreferences prefsApps = getRemotePreferences(PREFS_APPS);
        SharedPreferences prefsHistory = getRemotePreferences(PREFS_HISTORY);
        if (prefsApps == null || prefsHistory == null)
            return;
        Apps.init(lPParam, prefsApps, prefsHistory);
    }
}