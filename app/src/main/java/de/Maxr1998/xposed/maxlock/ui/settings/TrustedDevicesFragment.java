/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2015  Maxr1998
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

package de.Maxr1998.xposed.maxlock.ui.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.lib.FabView;

public class TrustedDevicesFragment extends Fragment implements AdapterView.OnItemLongClickListener {

    private static final int ADD = 1;
    private static final int REMOVE = 2;
    SharedPreferences pref;
    ViewGroup rootView;
    FabView mFab;
    private ListView list;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        // Preferences
        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_trusted_devices, container, false);
        list = (ListView) rootView.findViewById(R.id.trustedListView);
        list.setOnItemLongClickListener(this);
        mFab = (FabView) rootView.findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addTrustedWiFi();
            }
        });
        reloadList(trustedDevices(null, null));
        return rootView;
    }

    public void addTrustedWiFi() {
        WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        String SSID = wifiManager.getConnectionInfo().getSSID();
        if (SSID == null || SSID.equals("<unknown ssid>")) {
            Toast.makeText(getActivity(), R.string.no_network_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        reloadList(trustedDevices(ADD, SSID));
    }

    public void reloadList(Set<String> set) {
        List<String> devicesList = new LinkedList<>(set);
        ListAdapter listAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, devicesList);
        list.setAdapter(listAdapter);
    }

    @SuppressLint("NewApi")
    public Set<String> trustedDevices(Integer addOrRemove, String s) {
        Set<String> local = pref.getStringSet(Common.TRUSTED_DEVICES, ImmutableSet.of(getString(R.string.no_trusted_devices)));
        if (!(addOrRemove == null) && !(s == null)) {
            ImmutableSet.Builder<String> newTrustedDevices = new ImmutableSet.Builder<>();
            if (!local.contains(getString(R.string.no_trusted_devices))) {
                newTrustedDevices.addAll(local);
            }
            if (addOrRemove == ADD)
                newTrustedDevices.add(s);
            local = newTrustedDevices.build();
            if (addOrRemove == REMOVE) {
                local = Sets.difference(local, ImmutableSet.of(s)).immutableCopy();
                if (local.isEmpty()) {
                    local = ImmutableSet.of(getString(R.string.no_trusted_devices));
                }
            }
            pref.edit().putStringSet(Common.TRUSTED_DEVICES, local).apply();
        }
        return local;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (adapterView.getAdapter() == list.getAdapter()) {
            String name = adapterView.getItemAtPosition(i).toString();
            if (!name.equals(getString(R.string.no_trusted_devices)))
                reloadList(trustedDevices(REMOVE, name));
        }
        return false;
    }
}